package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager")
public class ManagerConfiguration {
    private boolean schedulingEnabled = true;
    private Modules modules;
    private String sofdBaseUrl;
    private String sofdApiKey;
    private HTMLConfig html = new HTMLConfig();
    private EmailConfig email = new EmailConfig();

    // used to point to mounted docker volume for custom resources (logo and css)
    // can be replace with e.g. "file:/custom/"
    private String customResourcePath = "classpath:/static/custom/";
    private boolean substituteAutoCompleteLimited = true;
}