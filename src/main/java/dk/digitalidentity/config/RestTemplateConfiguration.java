package dk.digitalidentity.config;

import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.security.RolePostProcesser;
import dk.digitalidentity.security.SecurityUtil;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RestTemplateConfiguration {
    
	// TODO: Use RestClient
	@SuppressWarnings("deprecation")
	@Bean
	public RestTemplate restTemplate() throws Exception {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] _, String _) -> true;

		SSLContext sslContext = SSLContextBuilder.create()
            .loadTrustMaterial(acceptingTrustStrategy)
            .build();

		SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .build();
        
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
        
        // Build the HTTP client
        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

		BufferingClientHttpRequestFactory requestFactory = new BufferingClientHttpRequestFactory(new HttpComponentsClientHttpRequestFactory(client));

		RestTemplate restTemplate = new RestTemplate(requestFactory);

		// tell SOFD Core which person we are acting on behalf of, so audit log entries are attributed to the
		// logged in manager rather than to this application's API key. Null during login, where no user exists yet
		restTemplate.getInterceptors().add((request, body, execution) -> {
			RolePostProcesser.LoggedInUser loggedInUser = SecurityUtil.getLoggedInUser();
			if (loggedInUser != null && loggedInUser.userId() != null && !loggedInUser.userId().isBlank()) {
				request.getHeaders().set("OnBehalfOf", loggedInUser.userId());
			}

			return execution.execute(request, body);
		});

		restTemplate.setErrorHandler(new ResponseErrorHandler() {

			@Override
			public boolean hasError(ClientHttpResponse response) throws IOException {
				return false;
			}

			@Override
			public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
				;
			}
		});

		// Be lenient about missing / null JSON values for primitive Java fields - some SOFD Core
		// releases omit newer fields (e.g. SofdAffiliation.deleted) from the response. Without this,
		// Jackson 3 fails the entire response. Spring 7 doesn't expose a mapper setter on the
		// existing converter, so we replace the JSON converter with a fresh one carrying a
		// configured JsonMapper.
		JsonMapper lenientMapper = JsonMapper.builder()
				.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
				.build();
		JacksonJsonHttpMessageConverter lenientJson = new JacksonJsonHttpMessageConverter(lenientMapper);
		var converters = restTemplate.getMessageConverters();
		for (int i = 0; i < converters.size(); i++) {
			if (converters.get(i) instanceof JacksonJsonHttpMessageConverter) {
				converters.set(i, lenientJson);
			}
		}

		return restTemplate;
	}
}
