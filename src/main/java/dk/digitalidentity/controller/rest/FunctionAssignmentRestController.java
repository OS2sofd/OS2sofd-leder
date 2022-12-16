package dk.digitalidentity.controller.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.RequireSubstituteOrManagerOrAdminAccess;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.AutoCompleteResult;

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
		
		AutoCompleteResult result = sofdFunctionAssignmentService.getAllAutoCompleteResultFunctionAssignmentPersons(ousString);
		return new ResponseEntity<>(result.suggestions().stream().filter(s -> s.value().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList()), HttpStatus.OK);
	}

	private record AddFunctionAssignmentDTO(@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate stopDate, String affiliationUuid, long functionId, List<SOFDFunctionAssignmentService.FacetValueDTO> facetValues) {}
	@ResponseBody
	@PostMapping(value = "/rest/functionassignment/add")
	public HttpEntity<?> createSubstituteAssignment(@RequestBody AddFunctionAssignmentDTO dto) {
		SOFDFunctionAssignmentService.AddFunctionAssignmentRequestDTO body = new SOFDFunctionAssignmentService.AddFunctionAssignmentRequestDTO(dto.startDate(), dto.stopDate(), dto.affiliationUuid(), dto.functionId(), dto.facetValues());
		boolean success = sofdFunctionAssignmentService.addFunctionAssignment(body);

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	private record FunctionAssignmentEditDTO(@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") LocalDate stopDate, List<SOFDFunctionAssignmentService.FacetValueDTO> facetValues) {}
	@ResponseBody
	@PostMapping(value = "/rest/functionassignment/{id}/edit")
	public HttpEntity<?> editSubstituteAssignment(@PathVariable long id, @RequestBody FunctionAssignmentEditDTO dto) {
		SOFDFunctionAssignmentService.FunctionAssignmentEditRequestDTO body = new SOFDFunctionAssignmentService.FunctionAssignmentEditRequestDTO(dto.startDate(), dto.stopDate(), dto.facetValues());
		boolean success = sofdFunctionAssignmentService.editFunctionAssignment(id, body);

		if (!success) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
}
