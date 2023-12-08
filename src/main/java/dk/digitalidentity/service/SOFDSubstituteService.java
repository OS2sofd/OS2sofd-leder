package dk.digitalidentity.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.security.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SOFDSubstituteService {
    @Autowired
    private RestTemplate restTemplate;
	
	@Autowired
	private ManagerConfiguration config;

	public record SubstituteContext(long id, String name, String identifier, boolean supportsConstraints, boolean canBeDeleted, boolean assignableToOrgunit, boolean inherit) {}
	public List<SubstituteContext> getSubstituteContexts() {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/substitutes/contexts";
		List<SubstituteContext> result = new ArrayList<>();

		ResponseEntity<SubstituteContext[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, SubstituteContext[].class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of substitute contexts. " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}
		
		if (response.getBody() != null) {
			result = new ArrayList<>(Arrays.asList(response.getBody()));
			// remove the global context
			result.removeIf(c -> c.identifier.equalsIgnoreCase("global"));
		} else {
			log.warn("Response body was null when fetching a list of substitute contexts");
		}
			
		return result;
	}
	
	public record Substitute(String uuid, String name) {}
	public record ConstraintOrgUnit(String uuid, String name, long assignmentId) {}
	public record SubstituteAssignment(long id, long substituteContextId, String substituteContextName, List<ConstraintOrgUnit> constraintOrgUnits, Substitute substitute, boolean orgUnitAssignment) {}
	public List<SubstituteAssignment> getSubstituteAssignments() {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String userUuid = SecurityUtil.getPersonUuid();
		String query = "/api/substitutes/assignments/" + userUuid;
		List<SubstituteAssignment> result = new ArrayList<>();

		ResponseEntity<SubstituteAssignment[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, SubstituteAssignment[].class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of substitute assignment for person with uuid " + userUuid + ". " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}
		
		if (response.getBody() != null) {
			result = new ArrayList<>(Arrays.asList(response.getBody()));

			// remove the global context assignments
			// todo: get the identifier out as well so we can compare on that instead of name. "Global" should be pretty static though.
			result.removeIf(a -> a.substituteContextName.equalsIgnoreCase("Global"));
		} else {
			log.warn("Response body was null when fetching a list of substitute assignmentfor person with uuid " + userUuid);
		}
			
		return result;
	}
	
	public List<ConstraintOrgUnit> getManagedOrgUnits() {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String userUuid = SecurityUtil.getPersonUuid();
		String query = "/api/substitutes/managedorgunits/" + userUuid;
		List<ConstraintOrgUnit> result = new ArrayList<>();

		ResponseEntity<ConstraintOrgUnit[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, ConstraintOrgUnit[].class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of managed orgUnits for person with uuid " + userUuid + ". " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}
		
		if (response.getBody() != null) {
			result = Arrays.asList(response.getBody());
		} else {
			log.warn("Response body was null when fetching a list of managed orgUnits for person with uuid " + userUuid);
		}
			
		return result;
	}

	public boolean delete(long id) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/substitutes/" + id + "/delete";

		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			return true;
		}
		
		log.warn("Failed to delete substitute assignment with id " + id);
		return false;
	}
	
	public record AutoCompleteResult(List<ValueData> suggestions) {}
	public record ValueData(String value, String data) {}
	public AutoCompleteResult getAutoCompleteResult(String term, String ousString) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String userUuid = SecurityUtil.getPersonUuid();
		String query = "/api/substitutes/search/person/" + userUuid + "?query=" + term + (ousString != null ? "&ous="+ousString : "");
		AutoCompleteResult result = null;

		ResponseEntity<AutoCompleteResult> response = restTemplate.exchange(url + query, HttpMethod.GET, request, AutoCompleteResult.class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch AutoCompleteResult for person with uuid " + userUuid + ". " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}
		
		if (response.getBody() != null) {
			result = response.getBody();
		} else {
			log.warn("Response body was null when fetching AutoCompleteResult for person with uuid " + userUuid);
		}
			
		return result;
	}
	
	record AddSubstituteDTO(long substituteContextId, String personUuid, String substitutePersonUuid, List<String> constraintOrgUnitUuids) {}
	public boolean addSubstitute(long substituteContextId, String substitutePersonUuid, List<String> constraintOrgUnitUuids) {
		String url = config.getSofdBaseUrl();
		String query = "/api/substitutes/assignments/create";

		String userUuid = SecurityUtil.getPersonUuid();
		AddSubstituteDTO dto = new AddSubstituteDTO(substituteContextId, userUuid, substitutePersonUuid, constraintOrgUnitUuids);
		HttpEntity<AddSubstituteDTO> request = new HttpEntity<>(dto, getHeaders());

		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			return true;
		}

		log.warn("Failed to create substitute assignment");
		return false;
	}


	record AddOrgUnitSubstituteDTO(long substituteContextId, String substitutePersonUuid, String orgUnitUuid) {}
	public boolean addOrgUnitSubstitute(long context, String substitute, String orgUnitUuid) {
		String url = config.getSofdBaseUrl();
		String query = "/api/substitutes/orgunit/assignments/create";

		AddOrgUnitSubstituteDTO dto = new AddOrgUnitSubstituteDTO(context, substitute, orgUnitUuid);
		HttpEntity<AddOrgUnitSubstituteDTO> request = new HttpEntity<>(dto, getHeaders());

		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			return true;
		}

		log.warn("Failed to create substitute orgunit assignment");
		return false;
	}

	public boolean deleteOrgUnitSubstitute(long id) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/substitutes/orgunit/" + id + "/delete";

		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			return true;
		}

		log.warn("Failed to delete substitute assignment with id " + id);
		return false;
	}
	
	public boolean editSubstitute(long assignmentId, List<String> constraintOus) {
		String url = config.getSofdBaseUrl();
		String query = "/api/substitutes/assignments/" + assignmentId + "/edit";
		
		HttpEntity<List<String>> request = new HttpEntity<>(constraintOus, getHeaders());
		
		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			return true;
		}
		
		log.warn("Failed to edit constraint ous for substitute assignment with id " + assignmentId);
		return false;
	}
	
	private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("apiKey", config.getSofdApiKey());
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        return headers;
    }
}
