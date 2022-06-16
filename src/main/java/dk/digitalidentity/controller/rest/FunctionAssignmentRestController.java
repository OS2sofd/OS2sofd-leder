package dk.digitalidentity.controller.rest;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.RequireSubstituteOrManagerOrAdminAccess;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.AddFunctionAssignmentDTO;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.AutoCompleteResult;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FunctionAssignmentEditDTO;

@RequireSubstituteOrManagerOrAdminAccess
@RestController
public class FunctionAssignmentRestController {
	
	@Autowired
	private SOFDFunctionAssignmentService sofdFunctionAssignmentService;

	@PostMapping(value = "/rest/functionassignment/delete/{id}")
	public HttpEntity<?> deleteSubstituteAssignemnt(@PathVariable("id") long id) {
		boolean success = sofdFunctionAssignmentService.delete(id);
		
		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@GetMapping(value = "/rest/functionassignment/search/person")
	public ResponseEntity<?> searchPerson(@RequestParam("query") String query, HttpServletRequest request) {
		String ousString = (String) request.getSession().getAttribute(SessionConstants.SESSION_CONTEXT_OUS);
		
		AutoCompleteResult result = sofdFunctionAssignmentService.getAutoCompleteResultFunctionAssignmentPersons(query, ousString);
		return new ResponseEntity<>(result.suggestions(), HttpStatus.OK);
	}
	
	@ResponseBody
	@PostMapping(value = "/rest/functionassignment/add")
	public HttpEntity<?> createSubstituteAssignment(@RequestBody AddFunctionAssignmentDTO dto) {
		boolean success = sofdFunctionAssignmentService.addFunctionAssignment(dto);

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@ResponseBody
	@PostMapping(value = "/rest/functionassignment/{id}/edit")
	public HttpEntity<?> createSubstituteAssignment(@PathVariable long id, @RequestBody FunctionAssignmentEditDTO dto) {
		boolean success = sofdFunctionAssignmentService.editFunctionAssignment(id, dto);

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
}
