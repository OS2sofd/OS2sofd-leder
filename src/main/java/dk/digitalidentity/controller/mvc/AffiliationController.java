package dk.digitalidentity.controller.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.config.settings.ManagerConfiguration;
import dk.digitalidentity.security.RequireSubstituteOrManagerAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RequireSubstituteOrManagerAccess
@Slf4j
@Controller
public class AffiliationController {

	@Autowired
	private ManagerConfiguration config;

	@Autowired
	private SOFDService sofdService;

	@GetMapping("/affiliations")
	public String listAffiliations(Model model, HttpServletRequest request) {
		if (!config.getModules().getAffiliationModule().isEnabled()) {
			return "error";
		}

		String uuid = SecurityUtil.getCurrentContext().uuid();
		model.addAttribute("affiliations", sofdService.getEditableAffilations(uuid));
		model.addAttribute("maxStopDateFutureDays", config.getModules().getAffiliationModule().getMaxStopDateFutureDays());

		return "affiliations/list";
	}

	@GetMapping("/affiliations/{affiliationUuid}")
	public String editAffiliation(Model model, @PathVariable("affiliationUuid") String affiliationUuid) {
		if (!config.getModules().getAffiliationModule().isEnabled()) {
			return "error";
		}

		String uuid = SecurityUtil.getCurrentContext().uuid();
		var affiliations = sofdService.getEditableAffilations(uuid);
		var affiliation = affiliations == null ? null
				: affiliations.stream().filter(a -> affiliationUuid.equalsIgnoreCase(a.affiliationUuid())).findFirst().orElse(null);

		if (affiliation == null) {
			model.addAttribute("error", "Du har ikke adgang til dette tilhørsforhold, eller tilhørsforholdet findes ikke.");
			return "error";
		}

		model.addAttribute("affiliation", affiliation);
		model.addAttribute("maxStopDateFutureDays", config.getModules().getAffiliationModule().getMaxStopDateFutureDays());
		return "affiliations/edit";
	}
}
