package dk.digitalidentity.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailConfig {
	private boolean enabled = false;
	private String from;
	private String fromName;
	private String username;
	private String password;
	private String host;
}
