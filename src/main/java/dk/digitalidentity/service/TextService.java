package dk.digitalidentity.service;

import dk.digitalidentity.dao.TextDao;
import dk.digitalidentity.dao.model.Text;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class TextService {
	
    @Autowired
    private TextDao textDao;

    public record TextDTO(TextService.TextType textType, String value) { }

    public String getText(TextType textType) {
        Text text = textDao.findByKey(textType.name());
        if (text == null) {
            text = new Text();
            text.setKey(textType.name());
            text.setValue("");
            text = textDao.save(text);
        }
        return text.getValue();
    }

    public void setText(TextType textType, String value) {
        Text text = textDao.findByKey(textType.name());
        if (text.getValue() == null || !text.getValue().equals(value)) {
            text.setValue(value);
            textDao.save(text);
        }
    }

    // method used from thymeleaf to avoid too much boilerplate
    public String getText(String textType) {
        return getText(TextType.valueOf(textType));
    }

    public List<TextDTO> getAll() {
        var result = new ArrayList<TextDTO>();
        for( var textType : TextType.getValuesSorted() ) {
            result.add(new TextDTO(textType, getText(textType)));
        }
        return result;
    }

    @Getter
    public enum TextType {
        WELCOME_TEXT("Velkommen",1)
        ,ALL_FUNCTION_ASSIGNMENTS_HELPTEXT("Alle medarbejderfunktioner hjælpetekst",2)
        ,SUBSTITUTE_HELPTEXT("Stedfortrædere hjælpetekst",3)
        ,AFFILIATION_HELPTEXT("Tilhørsforhold hjælpetekst",4)
        ,LEAVE_HELPTEXT("Pausemarkering hjælpetekst",5)
        ,MANAGER_HELPTEXT("Lederopmærkning hjælpetekst",6);


        private final String displayValue;
        private final int order;

        private TextType(String displayValue, int order) {
            this.displayValue = displayValue;
            this.order = order;
        }

        public static List<TextType> getValuesSorted() {
            return Arrays.stream(TextType.values()).sorted(Comparator.comparing(t -> t.order)).toList();
        }
    }

}
