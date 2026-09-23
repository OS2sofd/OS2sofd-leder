package dk.digitalidentity.config.settings;

import dk.digitalidentity.config.DbConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager.email")
public class EmailConfig {

	@DbConfiguration(group = "E-mail", label = "Send e-mail-kvitteringer ved ændringer")
	private boolean enabled = false;

	private String from;

	@DbConfiguration(group = "E-mail", label = "Afsendernavn på e-mails")
	private String fromName;

	private String username;
	private String password;
	private String host;
}
