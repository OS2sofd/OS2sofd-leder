package dk.digitalidentity.config.settings.modules;

import dk.digitalidentity.config.DbConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager.modules.substitute-module")
public class SubstituteModule {

	@DbConfiguration(group = "Stedfortrædere", label = "Aktiver modulet")
	private boolean enabled;

	@DbConfiguration(group = "Stedfortrædere", label = "Inkluder globale stedfortræderkontekster")
	private boolean includeGlobalContext;
}
