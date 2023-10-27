package dk.digitalidentity.controller.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.config.RoleConstants;
import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FunctionDTO;
import dk.digitalidentity.service.SOFDManagerService;
import dk.digitalidentity.service.SOFDManagerService.ContextDTO;
import dk.digitalidentity.service.SOFDManagerService.ContextResultDTO;
import dk.digitalidentity.service.SOFDManagerService.ContextRole;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class DefaultRestController {
	
	@Autowired
	private SOFDManagerService sofdManagerService;
	
	@Autowired
	private SOFDFunctionAssignmentService sofdFunctionAssignmentService;

	record ContextTopBarDTO(String uuid, String name, String role, String ous, boolean selected) {}
	@GetMapping("/default/contexts")
	public HttpEntity<?> loginContexts(Model model, HttpServletRequest request) {
//		List<ContextDTO> contexts = (List<ContextDTO>) request.getSession().getAttribute(SessionConstants.SESSION_CONTEXT_LIST);
		ContextResultDTO contextResult = sofdManagerService.getLoginContexts();
		if (SecurityUtil.hasRole(RoleConstants.ADMIN.toString())) {
			try {
				contextResult.contexts().add(new ContextDTO("ADMIN" + SecurityUtil.getPersonUuid(), contextResult.name(), ContextRole.ADMIN, sofdFunctionAssignmentService.getParentOUUuid()));
			} catch (Exception e) {
				log.warn("Failed to get parent ou: " + e.getMessage());
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		if (SecurityUtil.hasRole(RoleConstants.READER.toString())) {
			try {
				contextResult.contexts().add(new ContextDTO("READER" + SecurityUtil.getPersonUuid(), contextResult.name(), ContextRole.READER, sofdFunctionAssignmentService.getParentOUUuid()));
			} catch (Exception e) {
				log.warn("Failed to get parent ou: " + e.getMessage());
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		String selectedUuid = (String) request.getSession().getAttribute(SessionConstants.SESSION_CONTEXT_UUID);
		List<ContextTopBarDTO> result = contextResult.contexts().stream().map(c -> new ContextTopBarDTO(c.uuid(), c.name(), c.role().toString(), c.ouUuids(), c.uuid().equals(selectedUuid))).collect(Collectors.toList());
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping("/default/functions")
	public HttpEntity<?> functions(Model model, HttpServletRequest request) {
		List<FunctionDTO> functions = sofdFunctionAssignmentService.getFunctionsAndFacets();
		return new ResponseEntity<>(functions, HttpStatus.OK);
	}
	
}
