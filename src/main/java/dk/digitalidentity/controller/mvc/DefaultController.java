package dk.digitalidentity.controller.mvc;

import dk.digitalidentity.config.RoleConstants;
import dk.digitalidentity.config.SessionConstants;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.SOFDFunctionAssignmentService;
import dk.digitalidentity.service.SOFDManagerService;
import dk.digitalidentity.service.SOFDManagerService.ContextDTO;
import dk.digitalidentity.service.SOFDManagerService.ContextResultDTO;
import dk.digitalidentity.service.SOFDManagerService.ContextRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public String defaultPage() {
        return "index";
    }

    @GetMapping("/logincontexts")
    public String loginContexts(Model model, HttpServletRequest request) {
        ContextResultDTO result = sofdManagerService.getLoginContexts();
        if (result == null) {
            return "error";
        }

        if (SecurityUtil.hasRole(RoleConstants.ADMIN.toString())) {
            try {
                result.contexts().add(new ContextDTO(SecurityUtil.getUserUuid(), result.name(), ContextRole.ADMIN, sofdFunctionAssignmentService.getParentOUUuid()));
            } catch (Exception e) {
                log.warn("Failed to get parent ou: " + e.getMessage());
                return "error";
            }
        }

        ContextDTO loginContext = null;

        if (result.contexts().size() == 1) {
            loginContext = result.contexts().get(0);
        } else if (result.contexts().size() > 1) {
            loginContext = result.contexts().stream().filter(c -> c.role().equals(ContextRole.SUBSTITUTE)).findFirst().orElse(null);
            ContextDTO manager = result.contexts().stream().filter(c -> c.role().equals(ContextRole.MANAGER)).findFirst().orElse(null);
            loginContext = manager != null ? manager : loginContext;
            ContextDTO admin = result.contexts().stream().filter(c -> c.role().equals(ContextRole.ADMIN)).findFirst().orElse(null);
            loginContext = admin != null ? admin : loginContext;
        }

        // tried to save List<ContextDTO> on session, but can't retrieve it in DefaultRestController
        //request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_LIST, contexts);

        sofdFunctionAssignmentService.cleanUpTask();
        if (loginContext != null) {
            sofdFunctionAssignmentService.getAllAutoCompleteResultFunctionAssignmentPersons(loginContext.ouUuids());
            chooseContext(request, loginContext);
            return "redirect:/";
        }

        log.warn("Person with uuid " + SecurityUtil.getUserUuid() + " logged in to the manager ui, but is not an admin, manager or substitute.");
        return "error";
    }

    public void chooseContext(HttpServletRequest request, ContextDTO context) {
        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_NAME, context.name());
        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_UUID, context.uuid());
        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_OUS, context.ouUuids());
        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_ROLE, context.role());

        if (context.role().equals(ContextRole.MANAGER)) {
            securityUtil.switchToManagerContext();
        } else if (context.role().equals(ContextRole.SUBSTITUTE)) {
            securityUtil.switchToSubstituteContext();
        } else if (context.role().equals(ContextRole.ADMIN)) {
            request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_UUID, "ADMIN"+context.uuid());
            securityUtil.switchToAdminContext();
        } else {
            log.error("Unknown ContextRole " + context.role().toString());
        }
    }

    @GetMapping("/choosecontext")
    public String chooseContext(Model model, HttpServletRequest request, @RequestParam String uuid, @RequestParam String name, @RequestParam String ous, @RequestParam ContextRole role) {
        sofdFunctionAssignmentService.cleanUpTask();

        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_NAME, name);
        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_UUID, uuid);
        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_OUS, ous);
        request.getSession().setAttribute(SessionConstants.SESSION_CONTEXT_ROLE, role);

        if (role.equals(ContextRole.MANAGER)) {
            securityUtil.switchToManagerContext();
        } else if (role.equals(ContextRole.SUBSTITUTE)) {
            securityUtil.switchToSubstituteContext();
        } else if (role.equals(ContextRole.ADMIN)) {
            if (SecurityUtil.getBooleanAttribute("admin")) {
                securityUtil.switchToAdminContext();
            } else {
                log.error("Logged in person with uuid " + SecurityUtil.getUserUuid() + " is not an admin, but tried to switch to admin context");
                return "error";
            }
        } else {
            log.error("Unknown ContextRole " + role.toString());
            return "error";
        }

        sofdFunctionAssignmentService.getAllAutoCompleteResultFunctionAssignmentPersons(ous);

        return "redirect:/";
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
