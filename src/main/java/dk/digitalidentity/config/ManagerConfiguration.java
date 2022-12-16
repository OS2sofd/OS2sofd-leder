package dk.digitalidentity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager")
public class ManagerConfiguration {
	private boolean schedulingEnabled = true;
	private Modules modules;
	private String sofdBaseUrl;
	private String sofdApiKey;
	private HTMLConfig html;
	private boolean substituteAutoCompleteLimited = true;
}