package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@Controller
public class DefaultController implements ErrorController {

    @Autowired
    private SOFDManagerService sofdManagerService;

    @Autowired
    private SOFDFunctionAssignmentService sofdFunctionAssignmentService;

    @Autowired
    private SecurityUtil securityUtil;

    private ErrorAttributes errorAttributes = new DefaultErrorAttributes();

    @GetMapping("/")
    public String defaultPage(Model model) {
        return "index";
    }

    @GetMapping("/chooseContext")
    public String chooseContext(Model model, HttpServletRequest request, @RequestParam int id) {
        try {
            var chosenContext = SecurityUtil.getLoggedInUser().contexts().stream().filter(c -> c.id() == id).findFirst().orElseThrow();
            SecurityUtil.setCurrentContext(chosenContext);
            return "redirect:/";
        }
        catch(Exception e) {
            model.addAttribute("error","Ingen adgang");
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
