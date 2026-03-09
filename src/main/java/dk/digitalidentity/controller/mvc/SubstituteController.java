package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.security.RequireManagerAccess;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDSubstituteService;
import dk.digitalidentity.service.SOFDSubstituteService.ConstraintOrgUnit;
import dk.digitalidentity.service.SOFDSubstituteService.Substitute;
import dk.digitalidentity.service.SOFDSubstituteService.SubstituteContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequireManagerAccess
@Controller
public class SubstituteController {
	
	@Autowired
	private SOFDSubstituteService sofdSubstituteService;

	@Autowired
	private SOFDFunctionAssignmentService sofdFunctionAssignmentService;
	
	@Autowired
	private ManagerConfiguration config;

	public record SubstituteAssignmentDTO(long id, long substituteContextId, String substituteContextName, List<ConstraintOrgUnit> constraintOrgUnits, Substitute substitute, List<String> constraintOrgUnitsUuids, boolean isOrgUnitAssignment) {}

	@GetMapping("/substitutes")
	public String listSubstitutes(Model model) {
		if (!config.getModules().getSubstituteModule().isEnabled()) {
			return "error";
		}
		
		List<SubstituteContext> substituteContexts = sofdSubstituteService.getSubstituteContexts();
		List<ConstraintOrgUnit> managedOrgUnits = sofdSubstituteService.getManagedOrgUnits();
		List<String> allowedUuids = managedOrgUnits.stream().map(o -> o.uuid()).collect(Collectors.toList());
		model.addAttribute("allowedUuids", allowedUuids);

		// call method in sofdFunctionAssignmentService that allows us to know if a node should be opened or closed based on managed ous
		try {
			model.addAttribute("ous", sofdFunctionAssignmentService.getOus(allowedUuids));
		} catch (Exception e) {
			log.warn("Failed to filter ous: " + e.getMessage());
			return "error";
		}

		List<SubstituteAssignmentDTO> substituteAssignments = sofdSubstituteService.getSubstituteAssignments().stream().map(s -> new SubstituteAssignmentDTO(s.id(), s.substituteContextId(), s.substituteContextName(), s.constraintOrgUnits(), s.substitute(), s.constraintOrgUnits().stream().map(o -> o.uuid()).collect(Collectors.toList()), s.orgUnitAssignment())).collect(Collectors.toList());

		if (substituteAssignments == null || managedOrgUnits == null || substituteContexts == null) {
			return "error";
		}
		
		model.addAttribute("substituteAssignments", substituteAssignments);
		model.addAttribute("substituteContexts", substituteContexts);
		
		return "substitutes/list";
	}
}
