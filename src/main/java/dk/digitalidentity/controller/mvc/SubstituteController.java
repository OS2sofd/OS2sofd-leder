package dk.digitalidentity.controller.mvc;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.security.RequireManagerAccess;
import dk.digitalidentity.service.SOFDSubstituteService;
import dk.digitalidentity.service.SOFDSubstituteService.ConstraintOrgUnit;
import dk.digitalidentity.service.SOFDSubstituteService.Substitute;
import dk.digitalidentity.service.SOFDSubstituteService.SubstituteContext;

@RequireManagerAccess
@Controller
public class SubstituteController {
	
	@Autowired
	private SOFDSubstituteService sofdSubstituteService;
	
	@Autowired
	private ManagerConfiguration config;

	public record SubstituteAssignmentDTO(long id, long substituteContextId, String substituteContextName, List<ConstraintOrgUnit> constraintOrgUnits, Substitute substitute, List<String> constraintOrgUnitsUuids) {}
	@GetMapping("/substitutes")
	public String listSubstitutes(Model model) {
		if (!config.getModules().getSubstituteModule().isEnabled()) {
			return "error";
		}
		
		List<SubstituteContext> substituteContexts = sofdSubstituteService.getSubstituteContexts();
		List<ConstraintOrgUnit> managedOrgUnits = sofdSubstituteService.getManagedOrgUnits();
		List<SubstituteAssignmentDTO> substituteAssignments = sofdSubstituteService.getSubstituteAssignments().stream().map(s -> new SubstituteAssignmentDTO(s.id(), s.substituteContextId(), s.substituteContextName(), s.constraintOrgUnits(), s.substitute(), s.constraintOrgUnits().stream().map(o -> o.uuid()).collect(Collectors.toList()))).collect(Collectors.toList());
		
		if (substituteAssignments == null || managedOrgUnits == null || substituteContexts == null) {
			return "error";
		}
		
		model.addAttribute("managedOrgUnits", managedOrgUnits);
		model.addAttribute("substituteAssignments", substituteAssignments);
		model.addAttribute("substituteContexts", substituteContexts);
		
		return "substitutes/list";
	}
}
