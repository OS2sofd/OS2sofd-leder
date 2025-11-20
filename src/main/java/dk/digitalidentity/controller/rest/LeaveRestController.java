package dk.digitalidentity.controller.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.security.RequireSubstituteOrManagerAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.EmailService;
import dk.digitalidentity.service.SOFDManagerService;
import dk.digitalidentity.service.SOFDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;

@Slf4j
@RequireSubstituteOrManagerAccess
@RestController
public class LeaveRestController {

	@Autowired
	private SOFDService sofdService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private ManagerConfiguration configuration;

	@Autowired
	private SOFDManagerService sofdManagerService;

	public record EditLeaveDTO(String uuid, boolean paused, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate stopDate, String reason, String reasonText, boolean disableAccountOrders, boolean expireAccounts) {}
	@ResponseBody
	@PostMapping(value = "/rest/leave/edit")
	public HttpEntity<?> editAffiliation(HttpServletRequest request, @RequestBody EditLeaveDTO dto) {
		if (!configuration.getModules().getPausePersonModule().isEnabled()) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		LocalDate startDate = dto.startDate();
		if (startDate == null) {
			startDate = LocalDate.now();
		}

		if (dto.stopDate() != null && dto.stopDate().isBefore(startDate)) {
			return new ResponseEntity<>("Stopdato skal være efter startdato", HttpStatus.BAD_REQUEST);
		}

		// save in SOFD
		String uuid = SecurityUtil.getCurrentContext().uuid();
		boolean success = sofdService.editLeave(uuid, dto);

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
