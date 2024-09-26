package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.RequireAdminAccess;
import dk.digitalidentity.security.RequireReadOrAdminAccess;
import dk.digitalidentity.service.TextService;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
public class AdminController {

    @Autowired
    private TextService textService;

    @RequireReadOrAdminAccess
    @GetMapping("/admin/helptext/edit")
    public String EditHelpText(Model model) {
        model.addAttribute("helpTexts", textService.getAll());
        return "admin/helptext_edit";
    }

    @RequireAdminAccess
    @PostMapping("/admin/helptext/edit")
    public String EditHelpTextPost(@RequestParam Map<String, String> parameters) {
        for( var textType : TextService.TextType.values() ) {
            if( parameters.containsKey(textType.name())) {
                textService.setText(textType, getSafeHtml(parameters.get(textType.name())));
            }
        }
        return "redirect:/admin/helptext/edit";
    }

    private String getSafeHtml(String html) {
        PolicyFactory policy = new HtmlPolicyBuilder()
                .allowCommonBlockElements()
                .allowCommonInlineFormattingElements()
                .allowElements("a")
                .allowUrlProtocols("https")
                .allowAttributes("href", "target").onElements("a")
                .allowAttributes("class", "style", "id", "name").globally()
                .toFactory();
        String safeHTML = policy.sanitize(html);
        return safeHTML;
    }
}
