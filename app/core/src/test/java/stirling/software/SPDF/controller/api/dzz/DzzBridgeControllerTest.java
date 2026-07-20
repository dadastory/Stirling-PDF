package stirling.software.SPDF.controller.api.dzz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DzzBridgeControllerTest {

    @Test
    void loadsTheSignedDzzInputFile() throws Exception {
        MockMvc mvc =
                MockMvcBuilders.standaloneSetup(
                                new DzzBridgeController(
                                        new StubBridgeClient("signed-job", "source.pdf")))
                        .build();

        mvc.perform(get("/api/v1/dzz/input").header("X-Dzz-Pdf-Job", "signed-job"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(
                        header()
                                .string(
                                        "Content-Disposition",
                                        "inline; filename=\"source.pdf\""));
    }

    @Test
    void savesOnlyThroughTheSignedDzzJob() throws Exception {
        MockMvc mvc =
                MockMvcBuilders.standaloneSetup(
                                new DzzBridgeController(
                                        new StubBridgeClient("signed-job", "source.pdf")))
                        .build();
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "edited.pdf",
                        "application/pdf",
                        "%PDF-1.7 edited".getBytes(StandardCharsets.US_ASCII));

        mvc.perform(
                        multipart("/api/v1/dzz/save")
                                .file(file)
                                .header("X-Dzz-Pdf-Job", "signed-job"))
                .andExpect(status().isCreated());
    }

    private static class StubBridgeClient implements DzzBridgeClient {
        private final String expectedJob;
        private final String fileName;

        StubBridgeClient(String expectedJob, String fileName) {
            this.expectedJob = expectedJob;
            this.fileName = fileName;
        }

        @Override
        public DzzBridgeResponse input(String job) {
            if (!expectedJob.equals(job)) {
                return new DzzBridgeResponse(401, new byte[0], null);
            }
            return new DzzBridgeResponse(200, "%PDF-1.7".getBytes(StandardCharsets.US_ASCII), fileName);
        }

        @Override
        public DzzBridgeResponse save(String job, String name, byte[] content) {
            if (!expectedJob.equals(job) || !name.endsWith(".pdf") || content.length == 0) {
                return new DzzBridgeResponse(422, new byte[0], null);
            }
            return new DzzBridgeResponse(201, new byte[0], null);
        }
    }
}
