package dk.digitalidentity.config.settings.modules;

import dk.digitalidentity.config.DbConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager.modules.function-hierarchy")
public class FunctionHierarchy {

	@DbConfiguration(group = "Medarbejderfunktioner", label = "Aktiver modulet")
	private boolean enabled;

	@DbConfiguration(group = "Medarbejderfunktioner", label = "Startdato er påkrævet")
	private boolean startDateMandatory = true;

	@DbConfiguration(group = "Medarbejderfunktioner", label = "Stopdato er påkrævet")
	private boolean stopDateMandatory = true;

	@DbConfiguration(group = "Medarbejderfunktioner", label = "Vis menupunktet \"Alle medarbejderfunktioner\"")
	private boolean showAllFunctionsEnabled = false;
}
