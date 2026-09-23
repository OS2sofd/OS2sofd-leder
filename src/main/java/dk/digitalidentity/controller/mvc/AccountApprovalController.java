package dk.digitalidentity.controller.mvc;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.config.settings.ManagerConfiguration;
import dk.digitalidentity.security.RequireSubstituteOrManagerAccess;
import dk.digitalidentity.security.SecurityUtil;
import dk.digitalidentity.service.AccountApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequireSubstituteOrManagerAccess
@Slf4j
public class AccountApprovalController {
    private final AccountApprovalService accountApprovalService;
    private final ManagerConfiguration config;

    @GetMapping("/pendingapprovals")
    public String listPendingApprovals(Model model, HttpServletRequest request) {
        if (!config.getModules().getAccountApprovalModule().isEnabled()) {
            return "error";
        }
        String uuid = SecurityUtil.getCurrentContext().uuid();
        model.addAttribute("orders", accountApprovalService.getApprovalPendingAccountOrders(uuid));

        return "accountApprovals/list";
    }

    @PostMapping("/pendingapprovals/{id}")
    public ResponseEntity<String> approvePendingApprovals(@PathVariable long id, HttpServletRequest request) {
        if (!config.getModules().getAccountApprovalModule().isEnabled()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if(accountApprovalService.approveApprovalPendingAccountOrder(id, SecurityUtil.getCurrentContext().uuid())) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

}
