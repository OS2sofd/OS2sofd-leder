package dk.digitalidentity.service;

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
public class SOFDManagerService {
    @Autowired
    private RestTemplate restTemplate;
	
	@Autowired
	private ManagerConfiguration config;
	
	public enum ContextRole { MANAGER, SUBSTITUTE, ADMIN;}
	public record ContextDTO(String uuid, String name, ContextRole role, String ouUuids) {}
	public record ContextResultDTO(String name, String username, String email, List<ContextDTO> contexts) {}
	public ContextResultDTO getLoginContexts() {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/manager/"+ SecurityUtil.getUserUuid() + "/logincontexts";
		ContextResultDTO result = null;

		ResponseEntity<ContextResultDTO> response = restTemplate.exchange(url + query, HttpMethod.GET, request, ContextResultDTO.class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of contexts. " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}
		
		if (response.getBody() != null) {
			result = response.getBody();
		} else {
			log.warn("Response body was null when fetching a list of contexts");
		}
			
		return result;
	}
	
	private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("apiKey", config.getSofdApiKey());
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        return headers;
    }
}
