package dk.digitalidentity.controller.mvc;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import dk.digitalidentity.security.RolePostProcesser.ManagerUIContext;
import dk.digitalidentity.security.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class DefaultController implements ErrorController {
    private ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @GetMapping("/")
    public String defaultPage(Model model) {
        return "index";
    }

    // used to force a login
    @GetMapping("/startLogin")
    public String defaultLoginPage(Model model) {
        return "redirect:/";
    }

    @GetMapping("/chooseContext")
    public String chooseContext(Model model, HttpServletRequest request, @RequestParam int id) {
        try {
            ManagerUIContext chosenContext = SecurityUtil.getLoggedInUser().contexts().stream().filter(c -> c.id() == id).findFirst().orElseThrow();
            SecurityUtil.setCurrentContext(chosenContext);
            return "redirect:/";
        }
        catch (Exception ex) {
        	log.error("Failed to set context", ex);

            model.addAttribute("error", "Ingen adgang");
            return "error";
        }
    }

    @RequestMapping(value = "/error", produces = "text/html")
    public String errorPage(Model model, HttpServletRequest request) {
        Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

        model.addAllAttributes(body);

        Object status = body.get("status");
        if (status != null && status instanceof Integer && (Integer) status == 403) {
            return "index";
        }

        return "error";
    }

    @RequestMapping(value = "/error", produces = "application/json")
    public ResponseEntity<Map<String, Object>> errorJSON(HttpServletRequest request) {
        Map<String, Object> body = getErrorAttributes(new ServletWebRequest(request));

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        try {
            status = HttpStatus.valueOf((int) body.get("status"));
        } catch (Exception ex) {
            ;
        }

        return new ResponseEntity<>(body, status);
    }

    private Map<String, Object> getErrorAttributes(WebRequest request) {
        return errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
    }
}
