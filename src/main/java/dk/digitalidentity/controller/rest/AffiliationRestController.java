package dk.digitalidentity.controller.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.RequireSubstituteOrManagerAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.EmailService;
import dk.digitalidentity.service.SOFDAffiliationService;
import dk.digitalidentity.service.SOFDManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequireSubstituteOrManagerAccess
@RestController
public class AffiliationRestController {

	@Autowired
	private SOFDAffiliationService sofdAffiliationService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private ManagerConfiguration configuration;

	@Autowired
	private SOFDManagerService sofdManagerService;

	private record EditAffiliationDTO(long id, String position, String positionDisplayName, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate stopDate, String internalReference, String reason) {}
	@ResponseBody
	@PostMapping(value = "/rest/affiliations/edit")
	public HttpEntity<?> editAffiliation(HttpServletRequest request, @RequestBody EditAffiliationDTO dto) {
		if (!configuration.getModules().getAffiliationModule().isEnabled()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		if (!StringUtils.hasLength(dto.position().trim()) || !StringUtils.hasLength(dto.reason().trim())) {
			return new ResponseEntity<>("Stillingsbetegnelse og begrundelse skal være udfyldt", HttpStatus.BAD_REQUEST);
		}

		if (dto.stopDate() != null && dto.stopDate().isBefore(dto.startDate())) {
			return new ResponseEntity<>("Stopdato skal være efter startdato", HttpStatus.BAD_REQUEST);
		}

		// find the affiliation from SOFD, we need that to compare for the email later
		String uuid = (String) request.getSession().getAttribute(SessionConstants.SESSION_CONTEXT_UUID);
		List<SOFDAffiliationService.SofdAffiliation> affiliations = sofdAffiliationService.getEditableAffilations(uuid);
		SOFDAffiliationService.SofdAffiliation match = affiliations.stream().filter(a -> a.id() == dto.id()).findAny().orElse(null);
		if (match == null) {
			return new ResponseEntity<>("Kunne ikke finde tilhørsforhold med id " + dto.id() + " i SOFD.", HttpStatus.BAD_REQUEST);
		}

		// save in SOFD
		boolean success = sofdAffiliationService.editAffiliation(uuid, dto.id(), dto.position(), dto.positionDisplayName(), dto.startDate(), dto.stopDate(), dto.internalReference());

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		// send email
		SOFDManagerService.ContextResultDTO loginContexts = sofdManagerService.getLoginContexts();

		StringBuilder builder = new StringBuilder();
		builder.append("<p>Et tilhørsforhold er opdateret af ").append(loginContexts.name()).append(" (uuid:").append(SecurityUtil.getUserUuid()).append(").</p>");
		builder.append("<p>Det drejer sig om tilhørsforholdet for ").append(match.personName()).append(" i ").append(match.orgUnitName()).append(" med stillingsbetegnelsen ").append(dto.position()).append("</p>");
		builder.append("<p>Det er opdateret med begrundelsen: </p>");
		builder.append("<p>").append(dto.reason()).append("</p>");
		builder.append("<br/>");
		builder.append("<p>Ændringer:<p/>");
		builder.append("<ul>");

		String matchPositionName = match.positionName() != null && match.positionName().trim().equals("") ? null : match.positionName();
		String dtoPositionName = dto.position() != null && dto.position().trim().equals("") ? null : dto.position();
		if (!Objects.equals(matchPositionName, dtoPositionName)) {
			builder.append("<li>Stillingsbetegnelse er ændret fra ").append(matchPositionName == null ? "ikke angivet" : matchPositionName).append(" til ").append(dtoPositionName == null ? "ikke angivet" : dtoPositionName).append("</li>");
		}

		String matchPositionDisplayName = match.positionDisplayName() != null && match.positionDisplayName().trim().equals("") ? null : match.positionDisplayName();
		String dtoPositionDisplayName = dto.positionDisplayName() != null && dto.positionDisplayName().trim().equals("") ? null : dto.positionDisplayName();
		if (!Objects.equals(matchPositionDisplayName, dtoPositionDisplayName)) {
			builder.append("<li>Alternativ stillingsbetegnelse er ændret fra ").append(matchPositionDisplayName == null ? "ikke angivet" : matchPositionDisplayName).append(" til ").append(dtoPositionDisplayName == null ? "ikke angivet" : dtoPositionDisplayName).append("</li>");
		}

		if (!Objects.equals(match.stopDate(), dto.stopDate())) {
			builder.append("<li>Stopdato er ændret fra ").append(match.stopDate() == null ? "ikke angivet" : match.stopDate()).append(" til ").append(dto.stopDate() == null ? "ikke angivet" : dto.stopDate()).append("</li>");
		}

		String matchInternalReference = match.internalReference() != null && match.internalReference().trim().equals("") ? null : match.internalReference();
		String dtoInternalReference = dto.internalReference() != null && dto.internalReference().trim().equals("") ? null : dto.internalReference();
		if (!Objects.equals(matchInternalReference, dtoInternalReference)) {
			builder.append("<li>Intern reference er ændret fra ").append(matchInternalReference == null ? "ikke angivet" : matchInternalReference).append(" til ").append(dtoInternalReference == null ? "ikke angivet" : dtoInternalReference).append("</li>");
		}

		builder.append("</ul>");
		builder.append("<br/>");
		builder.append("<p>Dette er dokumentation samt revisionsspor. Forlængelsen er gennemført i SOFD Core og sagen kan lukkes med det samme i It-service og support</p>");

		try {
			emailService.sendMessage(configuration.getModules().getAffiliationModule().getReceiptEmail(), "Opdatering af tilhørsforhold", builder.toString(), loginContexts.email());
		}
		catch (Exception ex) {
			log.error("Failed send mail after affiliation was updated. Affiliation id: " + dto.id(), ex);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
