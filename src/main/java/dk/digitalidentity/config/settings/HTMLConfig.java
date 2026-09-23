package dk.digitalidentity.config.settings;

import dk.digitalidentity.config.DbConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager.html")
public class HTMLConfig {

	@DbConfiguration(group = "Visning", label = "Titel (vises i browser-faneblad og topbjælke)")
	private String title = "Lederportal";

	@DbConfiguration(group = "Visning", label = "Visningsnavn for SOFD-stedfortræderkontekst")
	private String sofdSubstituteContextName = "SOFD Core";
}
