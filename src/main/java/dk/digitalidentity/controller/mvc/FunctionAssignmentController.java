package dk.digitalidentity.controller.mvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.RequireSubstituteOrManagerOrAdminAccess;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FacetDTO;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FacetValueDTO;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FunctionAssignmentDTO;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FunctionDTO;
import lombok.extern.slf4j.Slf4j;

@RequireSubstituteOrManagerOrAdminAccess
@Slf4j
@Controller
public class FunctionAssignmentController {
	
	@Autowired
	private SOFDFunctionAssignmentService sofdFunctionAssignmentService;
	
	@Autowired
	private ManagerConfiguration config;

	@GetMapping("/functionassignments")
	public String listFunctionAssignments(Model model, HttpServletRequest request, @RequestParam(name = "function") long functionId) {
		if (!config.getModules().getFunctionHierarchy().isEnabled()) {
			return "error";
		}
		
		List<FunctionDTO> functionsAndFacets = sofdFunctionAssignmentService.getFunctionsAndFacets();
		FunctionDTO function = functionsAndFacets.stream().filter(f -> f.id() == functionId).findFirst().orElse(null);
		if (function == null) {
			return "error";
		}
		
		Collections.sort(function.facets(), Comparator.comparingLong(FacetDTO :: id));
		
		String ousString = (String) request.getSession().getAttribute(SessionConstants.SESSION_CONTEXT_OUS);
		List<String> ouUuids = Arrays.asList(ousString.split(","));
		
		List<FunctionAssignmentDTO> functionAssignments = sofdFunctionAssignmentService.getFunctionAssignments(ousString).stream().filter(f -> f.function().id() == functionId).collect(Collectors.toList());
		for (FunctionAssignmentDTO dto : functionAssignments) {
			Collections.sort(dto.facetValues(), Comparator.comparingLong(FacetValueDTO :: facetId));
		}
		
		model.addAttribute("functionAssignments", functionAssignments);
		model.addAttribute("functions", functionsAndFacets);
		model.addAttribute("function", function);
		
		try {
			model.addAttribute("ous", sofdFunctionAssignmentService.getOus(ouUuids));
		} catch (Exception e) {
			log.warn("Failed to filter ous: " + e.getMessage());
			return "error";
		}
		
		return "functionAssignments/list";
	}
}
