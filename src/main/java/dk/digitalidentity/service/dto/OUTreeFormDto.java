package dk.digitalidentity.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OUTreeFormDto {
	private String id;
	private String parent;
	private String text;
	private boolean open = false;
}
