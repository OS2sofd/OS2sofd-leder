package dk.digitalidentity.controller.rest;

import java.util.List;

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

import dk.digitalidentity.config.settings.ManagerConfiguration;
import dk.digitalidentity.security.RequireManagerAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDSubstituteService;
import dk.digitalidentity.service.SOFDSubstituteService.AutoCompleteResult;
import jakarta.servlet.http.HttpServletRequest;

@RequireManagerAccess
@RestController
public class SubstituteRestController {
	
	@Autowired
	private SOFDSubstituteService sofdSubstituteService;
	
	@Autowired
	private ManagerConfiguration config;

	@PostMapping(value = "/rest/substituteAssignment/delete/{id}")
	public HttpEntity<?> deleteSubstituteAssignemnt(@PathVariable("id") long id) {
		if (!ownsAssignment(id, sofdSubstituteService.getSubstituteAssignments())) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

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
			ousString = String.join(",", SecurityUtil.getCurrentContext().constraintOUs());;
		}
		AutoCompleteResult result = sofdSubstituteService.getAutoCompleteResult(query, ousString);
		return new ResponseEntity<>(result.suggestions(), HttpStatus.OK);
	}
	
	record SubstituteAssignmentAddDTO(String substitute, long context, List<String> constraint) {}
	@ResponseBody
	@PostMapping(value = "/rest/substituteAssignment/add")
	public HttpEntity<?> createSubstituteAssignment(@RequestBody SubstituteAssignmentAddDTO dto) {
		SOFDSubstituteService.SubstituteContext substituteContext = sofdSubstituteService.getSubstituteContexts().stream().filter(c -> c.id() == dto.context()).findAny().orElse(null);
		if (substituteContext == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (substituteContext.assignableToOrgunit()) {
			if (dto.constraint() == null || dto.constraint().isEmpty()) {
				return new ResponseEntity<>("Der skal vælges minimum en enhed", HttpStatus.BAD_REQUEST);
			}

			boolean anyFailed = false;
			for (String constraintOUUuid : dto.constraint()) {
				boolean status = sofdSubstituteService.addOrgUnitSubstitute(dto.context(), dto.substitute(), constraintOUUuid);
				if (!status) {
					anyFailed = true;
				}
			}
			if (anyFailed) {
				return new ResponseEntity<>("En eller flere stedfortræderetildelinger kunne ikke gemmes", HttpStatus.BAD_REQUEST);
			}
		} else {
			boolean success = sofdSubstituteService.addSubstitute(dto.context(), dto.substitute(), dto.constraint());
			if (!success) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	record SubstituteAssignmentEditDTO(long id, boolean orgUnitAssignment, String substituteUuid, long contextId, boolean contextIsAssignableToOrgUnit, List<String> constraints, boolean deleteOrgUnitAssignment) {}
	@ResponseBody
	@PostMapping(value = "/rest/substituteAssignment/edit")
	public HttpEntity<?> editSubstituteAssignment(@RequestBody SubstituteAssignmentEditDTO dto) {
		List<SOFDSubstituteService.SubstituteAssignment> all = sofdSubstituteService.getSubstituteAssignments();
		if (!dto.orgUnitAssignment() && !ownsAssignment(dto.id(), all)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (dto.orgUnitAssignment()) {
			if ((dto.constraints() == null || dto.constraints().isEmpty()) && !dto.deleteOrgUnitAssignment()) {
				return new ResponseEntity<>("Der skal vælges minimum en enhed", HttpStatus.BAD_REQUEST);
			}

			SOFDSubstituteService.SubstituteAssignment match = all.stream().filter(a -> a.orgUnitAssignment() && !a.inherited() && a.substitute().uuid().equals(dto.substituteUuid()) && a.substituteContextId() == dto.contextId()).findAny().orElse(null);
			if (match == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			boolean anyFailed = false;
			List<String> matchOrgUnitUuids = match.constraintOrgUnits().stream().map(m -> m.uuid()).toList();

			// add missing ou assignments
			for (String uuid : dto.constraints()) {
				if (!matchOrgUnitUuids.contains(uuid)) {
					boolean status = sofdSubstituteService.addOrgUnitSubstitute(dto.contextId(), dto.substituteUuid(), uuid);
					if (!status) {
						anyFailed = true;
					}
				}
			}

			// delete ou assignments
			for (SOFDSubstituteService.ConstraintOrgUnit ou : match.constraintOrgUnits()) {
				if (!dto.constraints().contains(ou.uuid())) {
					boolean status = sofdSubstituteService.deleteOrgUnitSubstitute(ou.assignmentId());
					if (!status) {
						anyFailed = true;
					}
				}
			}

			if (anyFailed) {
				return new ResponseEntity<>("En eller flere stedfortræderetildelinger kunne ikke gemmes", HttpStatus.BAD_REQUEST);
			}

		} else {
			if (dto.contextIsAssignableToOrgUnit()) {

				// the current assignment is not an orgUnitAssignment, but it should be. Delete the old one and create new orgUnitAssignments
				if (dto.constraints() == null || dto.constraints().isEmpty()) {
					return new ResponseEntity<>("Der skal vælges minimum en enhed", HttpStatus.BAD_REQUEST);
				}

				boolean anyFailed = false;
				boolean deleteStatus = sofdSubstituteService.delete(dto.id());
				if (!deleteStatus) {
					anyFailed = true;
				}

				for (String uuid : dto.constraints()) {
					boolean status = sofdSubstituteService.addOrgUnitSubstitute(dto.contextId(), dto.substituteUuid(), uuid);
					if (!status) {
						anyFailed = true;
					}
				}

				if (anyFailed) {
					return new ResponseEntity<>("En eller flere stedfortræderetildelinger kunne ikke gemmes", HttpStatus.BAD_REQUEST);
				}
			} else {
				boolean success = sofdSubstituteService.editSubstitute(dto.id(), dto.constraints());

				if (!success) {
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
				}
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	// a person can only change their own assignments. Assignments inherited from an orgUnit further up
	// the hierarchy are shown on the page, but belong to another manager and are read-only here
	private boolean ownsAssignment(long assignmentId, List<SOFDSubstituteService.SubstituteAssignment> assignments) {
		if (assignments == null) {
			return false;
		}

		return assignments.stream().anyMatch(a -> !a.orgUnitAssignment() && !a.inherited() && a.id() == assignmentId);
	}
}
