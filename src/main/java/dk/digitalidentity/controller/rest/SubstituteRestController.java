package dk.digitalidentity.controller.rest;

import java.util.List;

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

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.RequireManagerAccess;
import dk.digitalidentity.service.SOFDSubstituteService;
import dk.digitalidentity.service.SOFDSubstituteService.AutoCompleteResult;

@RequireManagerAccess
@RestController
public class SubstituteRestController {
	
	@Autowired
	private SOFDSubstituteService sofdSubstituteService;
	@Autowired
	private ManagerConfiguration config;

	@PostMapping(value = "/rest/substituteAssignment/delete/{id}")
	public HttpEntity<?> deleteSubstituteAssignemnt(@PathVariable("id") long id) {
		boolean success = sofdSubstituteService.delete(id);
		
		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@GetMapping(value = "/rest/substituteAssignment/search/person")
	public ResponseEntity<?> searchPerson(@RequestParam("query") String query, HttpServletRequest request) {
		String ousString = null;
		if (config.isSubstituteAutoCompleteLimited()) {
			ousString = (String) request.getSession().getAttribute(SessionConstants.SESSION_CONTEXT_OUS);
		}
		AutoCompleteResult result = sofdSubstituteService.getAutoCompleteResult(query, ousString);
		return new ResponseEntity<>(result.suggestions(), HttpStatus.OK);
	}
	
	record SubstituteAssignmentAddDTO(String substitute, long context, List<String> constraint) {}
	@ResponseBody
	@PostMapping(value = "/rest/substituteAssignment/add")
	public HttpEntity<?> createSubstituteAssignment(@RequestBody SubstituteAssignmentAddDTO dto) {
		
		boolean success = sofdSubstituteService.addSubstitute(dto.context(), dto.substitute(), dto.constraint());

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	record SubstituteAssignmentEditDTO(long id, List<String> constraint) {}
	@ResponseBody
	@PostMapping(value = "/rest/substituteAssignment/edit")
	public HttpEntity<?> editSubstituteAssignment(@RequestBody SubstituteAssignmentEditDTO dto) {
		
		boolean success = sofdSubstituteService.editSubstitute(dto.id(), dto.constraint());

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
