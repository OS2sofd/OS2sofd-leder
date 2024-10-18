package dk.digitalidentity.service;

import dk.digitalidentity.config.ManagerConfiguration;
import dk.digitalidentity.controller.rest.LeaveRestController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    SOFDService self;

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
        EditAffiliationDTO dto = new EditAffiliationDTO(id, position, positionDisplayName, startDate == null ? null : Date.from(startDate.atStartOfDay(defaultZoneId).toInstant()), stopDate == null ? null : Date.from(stopDate.atStartOfDay(defaultZoneId).toInstant()), internalReference);

        HttpEntity<EditAffiliationDTO> request = new HttpEntity<>(dto, getHeaders());

        ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return true;
        }

        log.warn("Failed to edit affiliation with id " + id);
        return false;
    }

    private record User(String userId, String kombitUuid) { }
    private record Person(String uuid, List<User> users) { }

    @Cacheable(value = "personMap", unless = "#result == null")
    public Person getPersonByKombitUuid(String kombitUuid) {
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

        return response.getBody();
    }


    public String ConvertKombitUuidToPersonUuid(String kombitUuid) {
        var person = self.getPersonByKombitUuid(kombitUuid);
        return person == null ? null : person.uuid();
    }

    public String ConvertKombitUuidToUserId(String kombitUuid) {
        var person = self.getPersonByKombitUuid(kombitUuid);
        var user = person == null ? null : person.users().stream().filter(u -> kombitUuid.equalsIgnoreCase(u.kombitUuid())).findFirst().orElse(null);
        return user == null ? null : user.userId();
    }

    private record PausablePersonDTO(String uuid, String personName, LeaveDTO leaveDTO, List<String> positions) {}
    private record LeaveDTO(LocalDate startDate, LocalDate stopDate, String leaveReasonValue, String leaveReasonMessage, String reasonText, boolean disableAccountOrders, boolean expireAccounts) {}
    public List<PausablePersonDTO> getPausablePeople(String uuid) {
        HttpEntity<String> request = new HttpEntity<>(getHeaders());
        String url = config.getSofdBaseUrl();

        String query = "/api/manager/" + uuid + "/pausablepeople";
        List<PausablePersonDTO> result = new ArrayList<>();

        ResponseEntity<PausablePersonDTO[]> response = restTemplate.exchange(url + query, HttpMethod.GET, request, PausablePersonDTO[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.warn("Failed to fetch list of pausable people for manager with uuid " + uuid + ". " + response.getStatusCodeValue() + ", response=" + response.getBody());
            return null;
        }

        if (response.getBody() != null) {
            result = new ArrayList<>(Arrays.asList(response.getBody()));
        } else {
            log.warn("Response body was null when fetching a list of pausable people for manager with uuid " + uuid);
        }

        return result;
    }

    public boolean editLeave(String uuid, LeaveRestController.EditLeaveDTO dto) {
        String url = config.getSofdBaseUrl();
        String query = "/api/manager/" + uuid + "/leave/edit";

        HttpEntity<LeaveRestController.EditLeaveDTO> request = new HttpEntity<>(dto, getHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url + query, HttpMethod.POST, request, String.class);
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            return true;
        }

        log.warn("Person with uuid " + uuid + " failed to edit leave on person with uuid " + dto.uuid());
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
