package dk.digitalidentity.service;

import dk.digitalidentity.config.settings.ManagerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class SOFDManagerService {
	public enum ContextRole { MANAGER, SUBSTITUTE }

	@Autowired
    private RestTemplate restTemplate;
	
	@Autowired
	private ManagerConfiguration config;

	public record ContextDTO(String uuid, String name, ContextRole role, String ouUuids) {}
	public record ContextResultDTO(String name, String username, String email, List<ContextDTO> contexts) {}

	public ContextResultDTO getLoginContexts(String personUuid) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/manager/"+ personUuid + "/logincontexts";
		ContextResultDTO result = null;

		ResponseEntity<ContextResultDTO> response = restTemplate.exchange(url + query, HttpMethod.GET, request, ContextResultDTO.class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of contexts. " + response.getStatusCode().value() + ", response=" + response.getBody());
			return null;
		}
		
		if (response.getBody() != null) {
			result = response.getBody();
		} else {
			log.warn("Response body was null when fetching a list of contexts");
		}
			
		return result;
	}

	public record OrgUnitManagerDto(String orgUnitUuid, String parentOrgUnitUuid, String orgUnitName, boolean managerExists, String managerUuid, String managerName, String source, boolean inherited) {}

	public List<OrgUnitManagerDto> getOrgUnitManagers() {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();
		String query = "/api/manager/orgUnitManagers";

		var response = restTemplate.exchange(url + query, HttpMethod.GET, request,OrgUnitManagerDto[].class );
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to get OrgUnitManagers, response=" + response.getBody());
			return null;
		}
		var result = Arrays.asList(response.getBody());
		return result;
	}

	public record ManagerSuggestion(String uuid, String display) {}

	public List<ManagerSuggestion> searchManagers(String query) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		URI uri = UriComponentsBuilder.fromUriString(config.getSofdBaseUrl() + "/api/manager/searchManagers")
				.queryParam("query", query)
				.encode().build().toUri();
		var response = restTemplate.exchange(uri, HttpMethod.GET, request, ManagerSuggestion[].class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to search for managers, response=" + response.getBody());
			return null;
		}
		var result = Arrays.asList(response.getBody());
		return result;
	}


	private record SetOrgUnitManagerDto(String orgUnitUuid, String managerUuid) {};
	public void setOrgUnitManager(String orgUnitUuid, String managerUuid) {
		var request = new HttpEntity<>(new SetOrgUnitManagerDto(orgUnitUuid,managerUuid), getHeaders());
		String url = config.getSofdBaseUrl();
		String query = "/api/manager/orgUnitManagers";
		var response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to set OrgUnitManager on orgUnitUuid: " + orgUnitUuid + " to managerUuid: " + managerUuid + ", response=" + response.getBody());
		}
	}
	
	private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("apiKey", config.getSofdApiKey());
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        return headers;
    }
}
