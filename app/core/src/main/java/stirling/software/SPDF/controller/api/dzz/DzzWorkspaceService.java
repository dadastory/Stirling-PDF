package stirling.software.SPDF.controller.api.dzz;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** Short-lived, job-isolated workspace for Dzz editing sessions. */
@Service
public class DzzWorkspaceService {
    private static final Pattern RESULT_ID = Pattern.compile("^[A-Za-z0-9_-]{1,80}$");
    private static final String FILE_SEPARATOR = "--";

    private final Path root;
    private final long ttlSeconds;
    private final long maxBytes;
    private final int maxFiles;

    public DzzWorkspaceService(
            @Value("${dzz.stirling.workspace-root:/tmp/stirling-pdf/dzz-workspaces}") String root,
            @Value("${dzz.stirling.workspace-ttl-seconds:1800}") long ttlSeconds,
            @Value("${dzz.stirling.workspace-max-file-size-mb:100}") long maxFileSizeMb,
            @Value("${dzz.stirling.workspace-max-files:20}") int maxFiles) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.ttlSeconds = Math.max(ttlSeconds, 60);
        this.maxBytes = Math.max(maxFileSizeMb, 1) * 1024L * 1024L;
        this.maxFiles = Math.max(maxFiles, 1);
    }

    public WorkspaceEntry store(String job, String resultId, String name, byte[] content)
            throws IOException {
        validateResultId(resultId);
        validatePdf(content);
        String safeName = safeName(name);
        Path directory = workspace(job);
        Files.createDirectories(directory);

        Path existing = findFile(directory, resultId);
        if (existing == null && countFiles(directory) >= maxFiles) {
            throw new IllegalArgumentException("Dzz workspace result limit reached");
        }
        if (existing != null) Files.deleteIfExists(existing);

        Files.write(
                directory.resolve(resultId + FILE_SEPARATOR + safeName),
                content,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        return new WorkspaceEntry(resultId, safeName);
    }

    public byte[] read(String job, String resultId) throws IOException {
        Path file = fileFor(job, resultId);
        if (file == null) throw new java.nio.file.NoSuchFileException(resultId);
        return Files.readAllBytes(file);
    }

    public boolean exists(String job, String resultId) {
        try {
            return fileFor(job, resultId) != null;
        } catch (IllegalArgumentException | IOException exception) {
            return false;
        }
    }

    public List<WorkspaceEntry> list(String job) throws IOException {
        Path directory = workspace(job);
        if (!Files.isDirectory(directory)) return List.of();
        touch(directory);
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .map(this::entryFromFile)
                    .flatMap(java.util.Optional::stream)
                    .sorted(java.util.Comparator.comparing(WorkspaceEntry::name))
                    .toList();
        }
    }

    public void delete(String job) throws IOException {
        deleteTree(workspace(job));
    }

    @Scheduled(fixedDelayString = "${dzz.stirling.workspace-cleanup-interval-ms:600000}")
    public void cleanupExpired() throws IOException {
        if (!Files.isDirectory(root)) return;
        long cutoff = Instant.now().minusSeconds(ttlSeconds).toEpochMilli();
        try (var directories = Files.list(root)) {
            directories
                    .filter(Files::isDirectory)
                    .filter(
                            path -> {
                                try {
                                    return Files.getLastModifiedTime(path).toMillis() < cutoff;
                                } catch (IOException ignored) {
                                    return false;
                                }
                            })
                    .forEach(
                            path -> {
                                try {
                                    deleteTree(path);
                                } catch (IOException ignored) {
                                    // A later scheduled cleanup will retry files held by a running
                                    // request.
                                }
                            });
        }
    }

    public record WorkspaceEntry(String id, String name) {}

    private Path fileFor(String job, String resultId) throws IOException {
        validateResultId(resultId);
        Path directory = workspace(job);
        if (!Files.isDirectory(directory)) return null;
        touch(directory);
        return findFile(directory, resultId);
    }

    private static Path findFile(Path directory, String resultId) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(
                            path ->
                                    path.getFileName()
                                            .toString()
                                            .startsWith(resultId + FILE_SEPARATOR))
                    .findFirst()
                    .orElse(null);
        }
    }

    private int countFiles(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return (int) files.filter(Files::isRegularFile).count();
        }
    }

    private static void touch(Path directory) throws IOException {
        Files.setLastModifiedTime(directory, java.nio.file.attribute.FileTime.from(Instant.now()));
    }

    private java.util.Optional<WorkspaceEntry> entryFromFile(Path path) {
        String value = path.getFileName().toString();
        int separator = value.indexOf(FILE_SEPARATOR);
        if (separator <= 0 || separator == value.length() - FILE_SEPARATOR.length()) {
            return java.util.Optional.empty();
        }
        String id = value.substring(0, separator);
        String name = value.substring(separator + FILE_SEPARATOR.length());
        if (!RESULT_ID.matcher(id).matches() || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new WorkspaceEntry(id, name));
    }

    private Path workspace(String job) {
        if (job == null || job.isBlank()) throw new IllegalArgumentException("Dzz job is required");
        return root.resolve(sha256(job));
    }

    private void validatePdf(byte[] content) {
        if (content == null
                || content.length < 5
                || content.length > maxBytes
                || content[0] != '%'
                || content[1] != 'P'
                || content[2] != 'D'
                || content[3] != 'F'
                || content[4] != '-') {
            throw new IllegalArgumentException("A valid PDF result is required");
        }
    }

    private static void validateResultId(String resultId) {
        if (resultId == null || !RESULT_ID.matcher(resultId).matches()) {
            throw new IllegalArgumentException("Invalid Dzz workspace result id");
        }
    }

    private static String safeName(String name) {
        String value = name == null ? "result.pdf" : name.trim();
        if (value.length() > 128
                || value.indexOf('/') >= 0
                || value.indexOf('\\') >= 0
                || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid PDF result name");
        }
        if (!value.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF required");
        }
        return value;
    }

    private static String sha256(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var files = Files.walk(path)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(
                            file -> {
                                try {
                                    Files.deleteIfExists(file);
                                } catch (IOException exception) {
                                    throw new UncheckedIOException(exception);
                                }
                            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }
}
