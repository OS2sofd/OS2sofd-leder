package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.RequireSubstituteOrManagerAccess;
import dk.digitalidentity.service.SOFDAffiliationService;
import dk.digitalidentity.service.SOFDSubstituteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RequireSubstituteOrManagerAccess
@Slf4j
@Controller
public class AffiliationController {

	@Autowired
	private ManagerConfiguration config;

	@Autowired
	private SOFDAffiliationService sofdAffiliationService;

	@GetMapping("/affiliations")
	public String listSubstitutes(Model model, HttpServletRequest request) {
		if (!config.getModules().getAffiliationModule().isEnabled()) {
			return "error";
		}

		String uuid = (String) request.getSession().getAttribute(SessionConstants.SESSION_CONTEXT_UUID);
		model.addAttribute("affiliations", sofdAffiliationService.getEditableAffilations(uuid));

		return "affiliations/list";
	}
}
