package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.security.RequireManagerEditorOrAdminAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDManagerService;
import dk.digitalidentity.service.SOFDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@RequireManagerEditorOrAdminAccess
@Slf4j
@Controller
public class ManagerController {

	@Autowired
	private ManagerConfiguration config;

	@Autowired
	private SOFDService sofdService;

	@Autowired
	private SOFDManagerService sofdManagerService;

	private record OrgUnitManagerTreeDto(String id, String parent, String orgUnitName, boolean managerExists, String managerName, String source, boolean inherited, boolean access ) {}
	@GetMapping("/managers")
	public String managers(Model model, HttpServletRequest request) {
		if (!config.getModules().getManagerModule().isEnabled()) {
			return "error";
		}
		var orgUnitManagers = sofdManagerService.getOrgUnitManagers();
		var orgunitManagerTree = orgUnitManagers.stream().map(o -> new OrgUnitManagerTreeDto(
				o.orgUnitUuid(),
				o.parentOrgUnitUuid() != null ? o.parentOrgUnitUuid() : "#",
				o.orgUnitName(),
				o.managerExists(),
				o.managerName(),
				o.source(),
				o.inherited(),
				hasAccess(o.orgUnitUuid())
		)).toList();
		model.addAttribute("orgunitManagerTree", orgunitManagerTree);
		return "managers/tree";
	}

	@PostMapping("/managers/delete")
	public String deleteManager(@RequestParam String orgUnitUuid) {
		if( !hasAccess(orgUnitUuid ) ) {
			return "error";
		}
		sofdManagerService.setOrgUnitManager(orgUnitUuid, null);
		return "redirect:/managers";
	}

	@PostMapping("/managers/set")
	public String setManager(@RequestParam String orgUnitUuid, @RequestParam String managerUuid) {
		if( !hasAccess(orgUnitUuid ) ) {
			return "error";
		}

		sofdManagerService.setOrgUnitManager(orgUnitUuid, managerUuid);
		return "redirect:/managers";
	}

	private boolean hasAccess(String orgUnitUuid) {
		return SecurityUtil.isAdmin() || SecurityUtil.getCurrentContext().constraintOUs() == null || SecurityUtil.getCurrentContext().constraintOUs().contains(orgUnitUuid);
	}

}
