package dk.digitalidentity.controller.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.security.RequireSubstituteOrManagerAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RequireSubstituteOrManagerAccess
@Slf4j
@Controller
public class LeaveController {

	@Autowired
	private ManagerConfiguration config;

	@Autowired
	private SOFDService sofdService;

	@GetMapping("/leave")
	public String listPausablePeople(Model model, HttpServletRequest request) {
		if (!config.getModules().getPausePersonModule().isEnabled()) {
			return "error";
		}
		String uuid = SecurityUtil.getCurrentContext().uuid();
		model.addAttribute("pausablePeople", sofdService.getPausablePeople(uuid));

		return "leave/list";
	}
}
