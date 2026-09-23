package dk.digitalidentity.config.settings.modules;

import dk.digitalidentity.config.DbConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager.modules.affiliation-module")
public class AffiliationModule {

	@DbConfiguration(group = "Tilhørsforhold", label = "Aktiver modulet")
	private boolean enabled = false;

	@DbConfiguration(group = "Tilhørsforhold", label = "Kvitterings-emailadresse (efterlad tom for at deaktivere)")
	private String receiptEmail;

	@DbConfiguration(group = "Tilhørsforhold", label = "Maks. antal dage stopdato kan ligge frem i tiden (tom for ingen grænse)")
	private Integer maxStopDateFutureDays;
}
