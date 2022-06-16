package dk.digitalidentity.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.dao.TextDao;
import dk.digitalidentity.dao.model.Text;

@Service
public class TextService {
	private static final String WELCOME_TEXT = "WELCOME_TEXT";

	@Autowired
	private TextDao textDao;
	
	public String getWelcomeText() {
		Text text = textDao.findByKey(WELCOME_TEXT);
		if (text == null) {
			text = new Text();
			text.setKey(WELCOME_TEXT);
			text.setValue("");
			text = textDao.save(text);
		}
		return text.getValue();
	}
	
	public void setWelcomeText(String value) {
		Text text = textDao.findByKey(WELCOME_TEXT);
		if (text.getValue() == null || (text.getValue() != null && !text.getValue().equals(value))) {
			text.setValue(value);
			textDao.save(text);
		}
	}
	
	
}
