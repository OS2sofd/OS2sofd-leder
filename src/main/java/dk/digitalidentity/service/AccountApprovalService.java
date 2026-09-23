package dk.digitalidentity.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.config.settings.ManagerConfiguration;
import dk.digitalidentity.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@EnableCaching
@RequiredArgsConstructor
@Service
@Slf4j
public class AccountApprovalService {
    private final ManagerConfiguration managerConfiguration;
    private final RestTemplate restTemplate;

    public record AccountOrderDTO (long id, @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date activationTimestamp, PersonDTO person, String userId, String userType) {}
    public record PersonDTO (String firstname, String surname) {}

    public List<AccountOrderDTO> getApprovalPendingAccountOrders(String managerUuid) {
        HttpEntity<String> request = new HttpEntity<>(getHeaders());
        String query = "/api/manager/" + managerUuid + "/accountorders";
        List<AccountOrderDTO> fetchedOrders = new ArrayList<>();
        ResponseEntity<AccountOrderDTO[]> response = restTemplate.exchange(managerConfiguration.getSofdBaseUrl() + query, HttpMethod.GET, request, AccountOrderDTO[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.warn("Failed to fetch list of account orders from sofd-core. " + response.getStatusCode().value() + ", response=" + Arrays.toString(response.getBody()));
            return null;
        }
        if (response.getBody() != null) {
            fetchedOrders = new ArrayList<>(Arrays.asList(response.getBody()));
        }
        else {
            log.warn("Response body was null when fetching a list of account orders from sofd");
        }
        return fetchedOrders;
    }

    public boolean approveApprovalPendingAccountOrder(long id, String managerUuid) {
        HttpEntity<String> request = new HttpEntity<>(getHeaders());

        //Checks if allowed to process them, but using the logic of getApprovalPendingAccountOrders
        //if it's not allowed to show it, it's not allowed to approve it
        if(getApprovalPendingAccountOrders(managerUuid).stream().noneMatch(approval -> approval.id == id)) {
            return false;
        }
        String query = "/api/manager/" + SecurityUtil.getLoggedInUser().personUuid() + "/accountorders/approve/" + id;
        ResponseEntity<AccountOrderDTO[]> response = restTemplate.exchange(managerConfiguration.getSofdBaseUrl() + query, HttpMethod.POST, request, AccountOrderDTO[].class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.warn("Failed to approve account order " + id + " from sofd-core. " + response.getStatusCode().value() + ", response=" + Arrays.toString(response.getBody()));
            return false;
        }
        return true;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("apiKey", managerConfiguration.getSofdApiKey());
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        return headers;
    }
}
