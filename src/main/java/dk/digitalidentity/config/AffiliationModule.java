package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AffiliationModule {
	private boolean enabled = false;
	private String receiptEmail;
}
