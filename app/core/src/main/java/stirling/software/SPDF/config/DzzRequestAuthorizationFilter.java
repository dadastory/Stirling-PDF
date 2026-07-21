package stirling.software.SPDF.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Requires the trusted task header that the Dzz gateway adds after its authorization subrequest.
 *
 * <p>The filter is inactive for a normal standalone Stirling deployment. In Dzz mode it also
 * prevents a non-2xx Dzz application response that has been rendered as an HTML error page from
 * accidentally exposing the embedded UI through Nginx {@code auth_request}.
 */
@Component
public class DzzRequestAuthorizationFilter extends OncePerRequestFilter {

    private static final String DZZ_JOB_HEADER = "X-Dzz-Pdf-Job";
    private static final String DZZ_GATEWAY_SECRET_HEADER = "X-Dzz-Stirling-Proxy";

    private final boolean dzzBridgeEnabled;
    private final String gatewaySecret;

    @Autowired
    public DzzRequestAuthorizationFilter(
            @Value("${dzz.stirling.bridge-url:}") String bridgeUrl,
            @Value("${dzz.stirling.gateway-secret:}") String gatewaySecret) {
        this.dzzBridgeEnabled = !bridgeUrl.isBlank();
        this.gatewaySecret = gatewaySecret == null ? "" : gatewaySecret;
    }

    DzzRequestAuthorizationFilter(String bridgeUrl) {
        this(bridgeUrl, "");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (dzzBridgeEnabled
                && (isBlank(request.getHeader(DZZ_JOB_HEADER))
                        || isBlank(gatewaySecret)
                        || !java.security.MessageDigest.isEqual(
                                gatewaySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                String.valueOf(request.getHeader(DZZ_GATEWAY_SECRET_HEADER))
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Dzz authorization is required");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
