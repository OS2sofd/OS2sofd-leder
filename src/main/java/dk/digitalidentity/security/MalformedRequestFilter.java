package dk.digitalidentity.security;

import java.io.IOException;

import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

// Tomcat 11 throws InvalidParameterException from getParameter() when the request body cannot be parsed
// (typically a truncated multipart/form-data body from a vulnerability scanner). Nothing in the Spring Security
// filter chain catches it, so it would otherwise end as an ERROR log and a 500. We turn it into a WARN and a 400.
//
// Spring's FormContentFilter (PUT/PATCH/DELETE with application/x-www-form-urlencoded) parses the body itself and
// wraps URLDecoder failures (a bare '%' in the payload) in HttpMessageNotReadableException, so we catch that too.
//
// The 400 is written directly instead of via sendError(), because sendError() triggers an ERROR dispatch to /error,
// which runs the security filter chain again, calls getParameter() again, and Tomcat rethrows the same cached exception.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MalformedRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        }
        catch (InvalidParameterException | HttpMessageNotReadableException ex) {
            log.warn("Malformed request parameters for {} {} from IP {} (user_agent={}): {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    request.getHeader(HttpHeaders.USER_AGENT),
                    ex.getMessage());

            if (!response.isCommitted()) {
                response.resetBuffer();
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("text/plain");
                response.getWriter().write("Bad Request");
                response.flushBuffer();
            }
        }
    }

    // also wrap error dispatches, so a rethrow during error page rendering is swallowed too
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
