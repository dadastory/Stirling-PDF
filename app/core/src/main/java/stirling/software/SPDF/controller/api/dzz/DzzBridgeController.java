package stirling.software.SPDF.controller.api.dzz;

import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dzz")
@RequiredArgsConstructor
public class DzzBridgeController {

    private final DzzBridgeClient dzzBridgeClient;

    @GetMapping(value = "/input", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> input(@RequestHeader("X-Dzz-Pdf-Job") String job) {
        DzzBridgeResponse response = dzzBridgeClient.input(job);
        if (response.status() != 200) {
            return ResponseEntity.status(response.status()).build();
        }
        String fileName = safePdfFilename(response.fileName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        "Content-Disposition",
                        ContentDisposition.inline().filename(fileName, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(response.body());
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> save(
            @RequestHeader("X-Dzz-Pdf-Job") String job,
            @RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        DzzBridgeResponse response =
                dzzBridgeClient.save(job, file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.status(response.status()).build();
    }

    private String safePdfFilename(String name) {
        if (name == null || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            return "document.pdf";
        }
        return name.replaceAll("[\\r\\n\\\"]", "_");
    }
}
