package dk.digitalidentity.controller.rest;

import dk.digitalidentity.security.RequireAdminAccess;
import dk.digitalidentity.security.RequireManagerEditorOrAdminAccess;
import dk.digitalidentity.service.SOFDManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RequireManagerEditorOrAdminAccess
@RestController
public class ManagerRestController {

	@Autowired
	SOFDManagerService sofdManagerService;

	@GetMapping(value = "/rest/manager/search/person")
	public ResponseEntity<?> searchPerson(@RequestParam("query") String query) {
		var result = sofdManagerService.searchManagers(query);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
