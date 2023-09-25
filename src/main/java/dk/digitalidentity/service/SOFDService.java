package dk.digitalidentity.service;

import dk.digitalidentity.config.ManagerConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class SOFDService {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ManagerConfiguration config;


    public record SofdAffiliation(long id, String personName, String positionName, String positionDisplayName,
                                  String orgUnitName, boolean active, LocalDate startDate, LocalDate stopDate,
                                  String internalReference, String adUserInfo) {
    }

    public List<SofdAffiliation> getEditableAffilations(String uuid) {
        HttpEntity<String> request = new HttpEntity<>(getHeaders());
        String url = config.getSofdBaseUrl();

        String query = "/api/manager/" + uuid + "/affiliations";
        List<SofdAffiliation> result = new ArrayList<>();

        ResponseEntity<SofdAffiliation[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, SofdAffiliation[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.warn("Failed to fetch list of editable affiliations for manager with uuid " + uuid + ". " + response.getStatusCodeValue() + ", response=" + response.getBody());
            return null;
        }

        if (response.getBody() != null) {
            result = new ArrayList<>(Arrays.asList(response.getBody()));
        } else {
            log.warn("Response body was null when fetching a list of editable affiliations for manager with uuid " + uuid);
        }

        return result;
    }

    private record EditAffiliationDTO(long id, String position, String positionDisplayName, Date startDate,
                                      Date stopDate, String internalReference) {
    }

    public boolean editAffiliation(String uuid, long id, String position, String positionDisplayName, LocalDate startDate, LocalDate stopDate, String internalReference) {
        String url = config.getSofdBaseUrl();
        String query = "/api/manager/" + uuid + "/affiliations/edit";
        ZoneId defaultZoneId = ZoneId.systemDefault();
        EditAffiliationDTO dto = new EditAffiliationDTO(id, position, positionDisplayName, Date.from(startDate.atStartOfDay(defaultZoneId).toInstant()), stopDate == null ? null : Date.from(stopDate.atStartOfDay(defaultZoneId).toInstant()), internalReference);

        HttpEntity<EditAffiliationDTO> request = new HttpEntity<>(dto, getHeaders());

        ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return true;
        }

        log.warn("Failed to edit affiliation with id " + id);
        return false;
    }

    private record Person(String uuid) {
    }
    @Cacheable(value = "personuuids", unless = "#result == null")
    public String ConvertKombitUuidToPersonUuid(String kombitUuid) {
        HttpEntity<String> request = new HttpEntity<>(getHeaders());
        String url = config.getSofdBaseUrl();
        String query = "/api/v2/persons/byKombitUuid/" + kombitUuid;

        ResponseEntity<Person> response = restTemplate.exchange(url + query, HttpMethod.GET, request, Person.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.warn("Failed to get person with kombituuid " + kombitUuid + ". " + response.getStatusCodeValue() + ", response=" + response.getBody());
            return null;
        }

        if (response.getBody() == null) {
            log.warn("Response body was null when fetching person with kombituuid " + kombitUuid);
            return null;
        }

        return response.getBody().uuid;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("apiKey", config.getSofdApiKey());
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        return headers;
    }
}
