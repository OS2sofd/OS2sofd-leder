package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.config.RoleConstants;
import dk.digitalidentity.security.RequireSubstituteOrManagerOrReaderOrAdminOrManagerEditorAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FacetDTO;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FacetValueDTO;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FunctionAssignmentDTO;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FunctionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequireSubstituteOrManagerOrReaderOrAdminOrManagerEditorAccess
@Slf4j
@Controller
public class FunctionAssignmentController {
	
	@Autowired
	private SOFDFunctionAssignmentService sofdFunctionAssignmentService;
	
	@Autowired
	private ManagerConfiguration config;

	@GetMapping({"/functionassignments/{functionId}", "/functionassignments/{functionId}/{mode}"})
	public String listFunctionAssignments(Model model, HttpServletRequest request, @PathVariable(name = "functionId") long functionId, @PathVariable(required = false) String mode) throws Exception {
		if (!config.getModules().getFunctionHierarchy().isEnabled()) {
			return "error";
		}
		var isFullScreen = "fullscreen".equals(mode);
		if (isFullScreen) {
            SecurityUtil.getLoggedInUser().contexts().stream().filter(c -> c.role().equals(RoleConstants.READER)).findFirst().ifPresent(SecurityUtil::setCurrentContext);
        }
		
		List<FunctionDTO> functionsAndFacets = sofdFunctionAssignmentService.getFunctionsAndFacets();
		FunctionDTO function = functionsAndFacets.stream().filter(f -> f.id() == functionId).findFirst().orElse(null);
		if (function == null) {
			return "error";
		}

		String ousString = String.join(",", SecurityUtil.getCurrentContext().constraintOUs());
		if (ousString == null) {
			return "error";
		}

		// if any of the facet has sortKeys (!= Long.MAX_VALUE) - sort by sortKey else sort by name
		if (function.facets().stream().anyMatch(f -> f.sortKey() != Long.MAX_VALUE)) {
			Collections.sort(function.facets(), Comparator.comparingLong(FacetDTO :: sortKey));
		} else {
			Collections.sort(function.facets(), Comparator.comparing(FacetDTO :: name));
		}

		List<String> ouUuids = Arrays.asList(ousString.split(","));

		List<FunctionAssignmentDTO> functionAssignments = new ArrayList<>();
		if (ousString.equals(sofdFunctionAssignmentService.getRootOUUuid())) {
			functionAssignments = sofdFunctionAssignmentService.getFunctionAssignments(null).stream().filter(f -> f.function().id() == functionId).collect(Collectors.toList());
		} else {
			functionAssignments = sofdFunctionAssignmentService.getFunctionAssignments(ousString).stream().filter(f -> f.function().id() == functionId).collect(Collectors.toList());
		}

		for (FunctionAssignmentDTO dto : functionAssignments) {
			// if any of the facet has sortKeys (!= Long.MAX_VALUE) - sort by sortKey else sort by name
			if (dto.facetValues().stream().anyMatch(f -> f.sortKey() != Long.MAX_VALUE)) {
				Collections.sort(dto.facetValues(), Comparator.comparingLong(FacetValueDTO :: sortKey));
			} else {
				Collections.sort(dto.facetValues(), Comparator.comparing(FacetValueDTO :: facetName));
			}
		}

		for (FunctionDTO dto : functionsAndFacets) {
			// if any of the facet has sortKeys (!= Long.MAX_VALUE) - sort by sortKey else sort by name
			if (dto.facets().stream().anyMatch(f -> f.sortKey() != Long.MAX_VALUE)) {
				Collections.sort(dto.facets(), Comparator.comparingLong(FacetDTO :: sortKey));
			} else {
				Collections.sort(dto.facets(), Comparator.comparing(FacetDTO :: name));
			}
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

		model.addAttribute("fullscreen", isFullScreen);
		
		return "functionAssignments/list";
	}

	@GetMapping("/allFunctionassignments")
	public String listAllFunctionAssignments(Model model, HttpServletRequest request) throws Exception {
		if (!config.getModules().getFunctionHierarchy().isEnabled()) {
			return "error";
		}

		List<FunctionDTO> functionsAndFacets = sofdFunctionAssignmentService.getFunctionsAndFacets();

		if (SecurityUtil.getCurrentContext() == null || SecurityUtil.getCurrentContext().constraintOUs() == null) {
			return "error";
		}
		String ousString = String.join(",", SecurityUtil.getCurrentContext().constraintOUs());
		if (ousString == null) {
			return "error";
		}
		List<String> ouUuids = Arrays.asList(ousString.split(","));
		List<FunctionAssignmentDTO> functionAssignments = new ArrayList<>();
		if (ousString.equals(sofdFunctionAssignmentService.getRootOUUuid())) {
			functionAssignments = sofdFunctionAssignmentService.getFunctionAssignments(null);
		} else {
			functionAssignments = sofdFunctionAssignmentService.getFunctionAssignments(ousString);
		}

		for (FunctionAssignmentDTO dto : functionAssignments) {
			Collections.sort(dto.facetValues(), Comparator.comparing(FacetValueDTO :: facetName));
		}

		for (FunctionDTO dto : functionsAndFacets) {
			Collections.sort(dto.facets(), Comparator.comparing(FacetDTO :: name));
		}

		model.addAttribute("functionAssignments", functionAssignments);
		model.addAttribute("functions", functionsAndFacets);

		try {
			model.addAttribute("ous", sofdFunctionAssignmentService.getOus(ouUuids));
		} catch (Exception e) {
			log.warn("Failed to filter ous: " + e.getMessage());
			return "error";
		}

		return "functionAssignments/listAll";
	}

}
