package dk.digitalidentity.config.settings.modules;

import dk.digitalidentity.config.DbConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager.modules.manager-module")
public class ManagerModule {

    @DbConfiguration(group = "Lederopmærkning", label = "Aktiver modulet")
    private boolean enabled = false;
}
