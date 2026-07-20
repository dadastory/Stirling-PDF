package stirling.software.SPDF.controller.api.dzz;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HttpDzzBridgeClient implements DzzBridgeClient {

    private static final Pattern CONTENT_DISPOSITION_FILENAME =
            Pattern.compile("filename=\\\"?([^\\\";\\r\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(20);

    private final HttpClient httpClient;
    private final String bridgeUrl;

    @Autowired
    public HttpDzzBridgeClient(@Value("${dzz.stirling.bridge-url:}") String bridgeUrl) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), bridgeUrl);
    }

    HttpDzzBridgeClient(HttpClient httpClient, String bridgeUrl) {
        this.httpClient = httpClient;
        this.bridgeUrl = bridgeUrl == null ? "" : bridgeUrl.trim();
    }

    @Override
    public DzzBridgeResponse input(String job) {
        return send(job, "input", HttpRequest.BodyPublishers.noBody(), null);
    }

    @Override
    public DzzBridgeResponse save(String job, String name, byte[] content) {
        String boundary = "dzz-stirling-" + java.util.UUID.randomUUID();
        byte[] opening =
                ("--"
                                + boundary
                                + "\\r\\nContent-Disposition: form-data; name=\\\"file\\\"; filename=\\\"result.pdf\\\""
                                + "\\r\\nContent-Type: application/pdf\\r\\n\\r\\n")
                        .getBytes(StandardCharsets.US_ASCII);
        byte[] closing = ("\\r\\n--" + boundary + "--\\r\\n").getBytes(StandardCharsets.US_ASCII);
        return send(
                job,
                "save",
                HttpRequest.BodyPublishers.ofByteArrays(List.of(opening, content, closing)),
                "multipart/form-data; boundary=" + boundary);
    }

    private DzzBridgeResponse send(
            String job, String operation, HttpRequest.BodyPublisher body, String contentType) {
        if (bridgeUrl.isEmpty() || job == null || job.isBlank()) {
            return new DzzBridgeResponse(503, new byte[0], null);
        }
        try {
            String separator = bridgeUrl.contains("?") ? "&" : "?";
            HttpRequest.Builder request =
                    HttpRequest.newBuilder(URI.create(bridgeUrl + separator + "bridge=" + operation))
                            .timeout(REQUEST_TIMEOUT)
                            .header("X-Dzz-Pdf-Job", job)
                            .POST(body);
            if (contentType != null) {
                request.header("Content-Type", contentType);
            }
            HttpResponse<byte[]> response =
                    httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new DzzBridgeResponse(
                    response.statusCode(),
                    response.body(),
                    readFilename(response.headers().firstValue("Content-Disposition")));
        } catch (Exception exception) {
            log.warn("Dzz Stirling bridge {} request failed", operation, exception);
            return new DzzBridgeResponse(502, new byte[0], null);
        }
    }

    private String readFilename(Optional<String> contentDisposition) {
        if (contentDisposition.isEmpty()) {
            return null;
        }
        Matcher matcher = CONTENT_DISPOSITION_FILENAME.matcher(contentDisposition.get());
        return matcher.find() ? matcher.group(1) : null;
    }
}
