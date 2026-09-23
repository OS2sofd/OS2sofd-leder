package dk.digitalidentity.config.settings;

import dk.digitalidentity.config.settings.modules.Modules;
import dk.digitalidentity.config.DbConfiguration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "manager")
public class ManagerConfiguration {
    private boolean schedulingEnabled = true;
    private String sofdBaseUrl;
    private String sofdApiKey;

    // used to point to mounted docker volume for custom resources (logo and css)
    // can be replace with e.g. "file:/custom/"
    private String customResourcePath = "classpath:/static/custom/";

    @DbConfiguration(group = "Generelle indstillinger", label = "Begræns stedfortræder-søgning til lederens egne afdelinger")
    private boolean substituteAutoCompleteLimited = true;

    // when true, the automatic leder/stedfortræder contexts fetched from SOFD Core
    // (/api/manager/{uuid}/logincontexts) are ignored. Only the SAML role claims
    // ROLE_leder / ROLE_stedfortraeder / ROLE_administrator / etc. grant access.
    @DbConfiguration(group = "Generelle indstillinger", label = "Ignorer automatisk leder-/stedfortræder-tildeling fra OS2sofd (kræver SAML-roller for adgang)")
    private boolean disableAutoSofdContexts = false;

    // nested config beans (each has its own @ConfigurationProperties prefix). Exposing them via
    // getters keeps the existing callsite shape: config.getHtml().getTitle(), config.getModules()...
    @Autowired private Modules modules;
    @Autowired private HTMLConfig html;
    @Autowired private EmailConfig email;
}
