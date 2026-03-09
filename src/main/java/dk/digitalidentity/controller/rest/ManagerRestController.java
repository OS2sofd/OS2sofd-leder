package dk.digitalidentity.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.security.RequireManagerEditorOrAdminAccess;
import dk.digitalidentity.service.SOFDManagerService;

@RequireManagerEditorOrAdminAccess
@RestController
public class ManagerRestController {

	@Autowired
	private SOFDManagerService sofdManagerService;

	@GetMapping(value = "/rest/manager/search/person")
	public ResponseEntity<?> searchPerson(@RequestParam("query") String query) {
		var result = sofdManagerService.searchManagers(query);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
