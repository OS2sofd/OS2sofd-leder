package dk.digitalidentity.service;

import dk.digitalidentity.dao.TextDao;
import dk.digitalidentity.dao.model.Text;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TextService {
    public static final String WELCOME_TEXT = "WELCOME_TEXT";
    public static final String SUBSTITUTE_HELPTEXT = "SUBSTITUTE_HELPTEXT";

    @Autowired
    private TextDao textDao;

    private String getText(String key) {
        Text text = textDao.findByKey(key);
        if (text == null) {
            text = new Text();
            text.setKey(key);
            text.setValue("");
            text = textDao.save(text);
        }
        return text.getValue();
    }

    private void setText(String key, String value) {
        Text text = textDao.findByKey(key);
        if (text.getValue() == null || (text.getValue() != null && !text.getValue().equals(value))) {
            text.setValue(value);
            textDao.save(text);
        }
    }

    public String getWelcomeText() {
        return this.getText(WELCOME_TEXT);
    }

    public void setWelcomeText(String value) {
        this.setText(WELCOME_TEXT, value);
    }

    public String getSubstituteHelptext() {
        return this.getText(SUBSTITUTE_HELPTEXT);
    }

    public void setSubstituteHelptext(String value) {
        this.setText(SUBSTITUTE_HELPTEXT, value);
    }


}
