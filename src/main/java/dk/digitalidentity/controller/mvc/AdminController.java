package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.RequireAdminAccess;
import dk.digitalidentity.service.TextService;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequireAdminAccess
public class AdminController {

    @Autowired
    private TextService textService;

    public record TextDTO(String value) {
    }

    @GetMapping("/admin/welcome/edit")
    public String EditWelcomeText(Model model) {
        model.addAttribute("TextDTO", new TextDTO(textService.getWelcomeText()));
        return "admin/welcome_edit";
    }

    @PostMapping("/admin/welcome/edit")
    public String EditWelcomeTextPost(Model model, TextDTO textDTO) {
        if (textDTO.value().length() > 65536) {
            TextDTO dto = new TextDTO(textDTO.value());
            model.addAttribute("TextDTO", dto);
            model.addAttribute("showError", true);

            return "admin/welcome_edit";
        }

        textService.setWelcomeText(getSafeHtml(textDTO.value()));
        return "redirect:/";
    }

    @GetMapping("/admin/helptext/edit")
    public String EditHelpText(Model model) {
        model.addAttribute("helpText", new TextDTO(textService.getSubstituteHelptext()));
        return "admin/helptext_edit";
    }

    @PostMapping("/admin/helptext/edit")
    public String EditHelpTextPost(Model model, TextDTO helpTextDTO) {
        if (helpTextDTO.value().length() > 65536) {
            TextDTO dto = new TextDTO(helpTextDTO.value());
            model.addAttribute("helpText", dto);
            model.addAttribute("showError", true);

            return "admin/helptext_edit";
        }
        textService.setSubstituteHelptext(getSafeHtml(helpTextDTO.value()));
        return "redirect:/";
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
