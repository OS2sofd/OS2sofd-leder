package dk.digitalidentity.service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OUTreeFormWithOpenDto {
	private String id;
	private String parent;
	private String text;
	private boolean open = false;

	public OUTreeFormWithOpenDto(OUTreeFormDto dto) {
		this.id = dto.getId();
		this.parent = dto.getParent();
		this.text = dto.getText();
	}
}