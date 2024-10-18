package dk.digitalidentity.controller.rest;

import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDFunctionAssignmentService.FunctionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class DefaultRestController {

	@Autowired
	private SOFDFunctionAssignmentService sofdFunctionAssignmentService;

	@GetMapping("/default/functions")
	public HttpEntity<?> functions(Model model, HttpServletRequest request) {
		List<FunctionDTO> functions = sofdFunctionAssignmentService.getFunctionsAndFacets();
		return new ResponseEntity<>(functions, HttpStatus.OK);
	}
	
}