package dk.digitalidentity.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.dto.OUTreeFormDto;
import dk.digitalidentity.service.dto.OUTreeFormWithOpenDto;
import lombok.extern.slf4j.Slf4j;

@EnableCaching
@EnableScheduling
@Service
@Slf4j
public class SOFDFunctionAssignmentService {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ManagerConfiguration config;

	@Autowired
	private SOFDFunctionAssignmentService self;

	public record FunctionDTO(long id, String name, String category, String description, List<FacetDTO> facets) {}
	public record FacetDTO(long id, String name, String description, String type, String pattern, Long sortKey, List<String> listItems) {}
	@Cacheable("functions")
	public List<FunctionDTO> getFunctionsAndFacets() {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/functionhierarchy/functions";
		List<FunctionDTO> result = new ArrayList<>();

		ResponseEntity<FunctionDTO[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, FunctionDTO[].class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of functions and facets. " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}

		if (response.getBody() != null) {
			result = Arrays.asList(response.getBody());
		}
		else {
			log.warn("Response body was null when fetching a list of functions and facets");
		}

		return result;
	}

	public record FunctionAssignmentDTO(long id, LocalDate startDate, LocalDate stopDate, String affiliationUuid, String affiliationPersonName, String affiliationPersonUserId, FunctionDTO function, List<FacetValueDTO> facetValues, String otherAssignments) {}
	public record FacetValueDTO(long facetId, String facetName, String text, String facetListItem, List<String> facetValueOrgunitUuids, String facetValueOrgunitNames, String facetValueAffiliationUuid, String facetValueAffiliationPersonName, LocalDate date) {}
	@Cacheable("functionAssignments")
	public List<FunctionAssignmentDTO> getFunctionAssignments(String ouUuids) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/functionhierarchy/functionassignments";
		if (ouUuids != null) {
			query += "?ous=" + ouUuids;
		}

		List<FunctionAssignmentDTO> result = new ArrayList<>();

		ResponseEntity<FunctionAssignmentDTO[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, FunctionAssignmentDTO[].class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of functionAssignments. " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}

		if (response.getBody() != null) {
			result = Arrays.asList(response.getBody());
		}
		else {
			log.warn("Response body was null when fetching a list of functionAssignments");
		}

		return result;
	}

	public boolean delete(long id) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/functionhierarchy/functionassignments/" + id + "/delete";

		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			self.cleanUpFunctionAssignments();
			return true;
		}

		log.warn("Failed to delete function assignment with id " + id);
		return false;
	}

	public record AddFunctionAssignmentRequestDTO(LocalDate startDate, LocalDate stopDate, String affiliationUuid, long functionId, List<FacetValueDTO> facetValues) {}
	public boolean addFunctionAssignment(AddFunctionAssignmentRequestDTO dto) {
		String url = config.getSofdBaseUrl();
		String query = "/api/functionhierarchy/functionassignments/create";

		HttpEntity<AddFunctionAssignmentRequestDTO> request = new HttpEntity<>(dto, getHeaders());

		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			self.cleanUpFunctionAssignments();
			return true;
		}

		log.warn("Failed to create function assignment: StatusCode: " + response.getStatusCodeValue() + " Body: " + response.getBody() );
		return false;
	}


	public record FunctionAssignmentEditRequestDTO(LocalDate startDate, LocalDate stopDate, List<FacetValueDTO> facetValues) {}
	public boolean editFunctionAssignment(long id, FunctionAssignmentEditRequestDTO dto) {
		String url = config.getSofdBaseUrl();
		String query = "/api/functionhierarchy/functionassignments/" + id + "/edit";

		HttpEntity<FunctionAssignmentEditRequestDTO> request = new HttpEntity<>(dto, getHeaders());

		ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
		if (response.getStatusCode().equals(HttpStatus.OK)) {
			self.cleanUpFunctionAssignments();
			return true;
		}

		log.warn("Failed to edit function assignment with id " + id);
		return false;
	}

	public record AutoCompleteResult(List<ValueData> suggestions) {}
	public record ValueData(String value, String data) {}
	@Cacheable(value = "autocompleteAll")
	public AutoCompleteResult getAllAutoCompleteResultFunctionAssignmentPersons(String ous) {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String userUuid = SecurityUtil.getPersonUuid();
		String query = "/api/functionhierarchy/search/all/?ous=" + ous;
		AutoCompleteResult result = null;

		ResponseEntity<AutoCompleteResult> response = restTemplate.exchange(url + query, HttpMethod.GET, request, AutoCompleteResult.class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch all AutoCompleteResultFunctionAssignmentPersons for person with uuid " + userUuid + ". " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}

		if (response.getBody() != null) {
			result = response.getBody();
		}
		else {
			log.warn("Response body was null when fetching all AutoCompleteResultFunctionAssignmentPersons for person with uuid " + userUuid);
		}

		return result;
	}

	@Cacheable("ous")
	public List<OUTreeFormDto> getAllOus() {
		HttpEntity<String> request = new HttpEntity<>(getHeaders());
		String url = config.getSofdBaseUrl();

		String query = "/api/functionhierarchy/ous";
		List<OUTreeFormDto> result = new ArrayList<>();

		ResponseEntity<OUTreeFormDto[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, OUTreeFormDto[].class);
		if (!response.getStatusCode().equals(HttpStatus.OK)) {
			log.warn("Failed to fetch list of ous. " + response.getStatusCodeValue() + ", response=" + response.getBody());
			return null;
		}

		if (response.getBody() != null) {
			result = Arrays.asList(response.getBody());
		}
		else {
			log.warn("Response body was null when fetching a list of ous");
		}

		return result;
	}

	public List<OUTreeFormWithOpenDto> getOus(List<String> ouUuids) throws Exception {
		var allOus = self.getAllOus().stream().map(o -> new OUTreeFormWithOpenDto(o)).toList();
		var topNode = allOus.stream().filter(o -> o.getParent().equals("#")).findFirst();
		if (!topNode.isPresent()) {
			throw new Exception("No top OU node was found");
		}
		var filteredOus = new ArrayList<OUTreeFormWithOpenDto>();
		filterOusRecursive(filteredOus, allOus, ouUuids, topNode.get());
		return filteredOus;
	}

	private boolean filterOusRecursive(List<OUTreeFormWithOpenDto> filteredOus, List<OUTreeFormWithOpenDto> allOus, List<String> allowedOus, OUTreeFormWithOpenDto node) {
		node.setOpen(node.isOpen() || allowedOus.contains(node.getId()));
		var open = node.isOpen();
		var children = allOus.stream().filter(o -> node.getId().equalsIgnoreCase(o.getParent())).collect(Collectors.toList());
		for (var child : children) {
			child.setOpen(node.isOpen());
			open |= filterOusRecursive(filteredOus, allOus, allowedOus, child);
		}

		filteredOus.add(node);

		if (open) {
			node.setOpen(true);
			return true;
		}
		return false;
	}

	public String getParentOUUuid() throws Exception {
		var allOus = self.getAllOus();
		var topNode = allOus.stream().filter(o -> o.getParent().equals("#")).findFirst();
		if (!topNode.isPresent()) {
			throw new Exception("No top OU node was found");
		}
		return topNode.get().getId();
	}
	
	@Cacheable("categories")
	public Map<String, List<FunctionDTO>> getAllCategories() {
		Map<String, List<FunctionDTO>> result = new TreeMap<>();
		List<FunctionDTO> functions = self.getFunctionsAndFacets();
		
		for (FunctionDTO function : functions) {
			if (!result.containsKey(StringUtils.capitalize(function.category()))) {
				result.put(StringUtils.capitalize(function.category()), new ArrayList<>());
			}
			
			result.get(StringUtils.capitalize(function.category())).add(function);
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

	@CacheEvict(value = "ous", allEntries = true)
	public void cleanUpOus() {
	}

	@CacheEvict(value = "functions", allEntries = true)
	public void cleanUpFunctions() {
	}

	@CacheEvict(value = "functionAssignments", allEntries = true)
	public void cleanUpFunctionAssignments() {
	}

	@CacheEvict(value = "autocompleteAll", allEntries = true)
	public void cleanUpAllAutocomplete() {
	}
	
	@CacheEvict(value = "categories", allEntries = true)
	public void cleanUpAllCategories() {
	}

	@Scheduled(fixedRate = 10 * 60 * 1000)
	public void cleanUpTask() {
		self.cleanUpOus();
		self.cleanUpFunctions();
		self.cleanUpFunctionAssignments();
		self.cleanUpAllAutocomplete();
		self.cleanUpAllCategories();
	}
}
