package stirling.software.SPDF.controller.api.dzz;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final DzzWorkspaceService workspaceService;

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
                        ContentDisposition.inline()
                                .filename(fileName, java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(response.body());
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> save(
            @RequestHeader("X-Dzz-Pdf-Job") String job, @RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        DzzBridgeResponse response =
                dzzBridgeClient.save(job, file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.status(response.status()).build();
    }

    @PostMapping(value = "/workspace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DzzWorkspaceService.WorkspaceEntry> storeWorkspaceResult(
            @RequestHeader("X-Dzz-Pdf-Job") String job,
            @RequestParam("id") String resultId,
            @RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        workspaceService.store(
                                job, resultId, file.getOriginalFilename(), file.getBytes()));
    }

    @GetMapping(value = "/workspace/{resultId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> readWorkspaceResult(
            @RequestHeader("X-Dzz-Pdf-Job") String job, @PathVariable String resultId)
            throws java.io.IOException {
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(workspaceService.read(job, resultId));
        } catch (java.nio.file.NoSuchFileException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/workspace/results")
    public java.util.List<DzzWorkspaceService.WorkspaceEntry> listWorkspaceResults(
            @RequestHeader("X-Dzz-Pdf-Job") String job) throws java.io.IOException {
        return workspaceService.list(job);
    }

    @DeleteMapping("/workspace")
    public ResponseEntity<Void> deleteWorkspace(@RequestHeader("X-Dzz-Pdf-Job") String job)
            throws java.io.IOException {
        workspaceService.delete(job);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> invalidWorkspaceRequest(
            IllegalArgumentException exception) {
        return ResponseEntity.unprocessableEntity()
                .body(java.util.Map.of("error", exception.getMessage()));
    }

    private String safePdfFilename(String name) {
        if (name == null || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            return "document.pdf";
        }
        return name.replaceAll("[\\r\\n\\\"]", "_");
    }
}
