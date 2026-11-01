package dev.pyrostudios.notsosimplevoicechat.audio;

import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

public final class DeepFilterModelStore {
    private static final String MODEL_FILE = "denoiser_model.onnx";
    private static final String MODEL_URL =
            "https://raw.githubusercontent.com/yuyun2000/SpeechDenoiser/main/48k/denoiser_model.onnx";
    private static final String EXPECTED_SHA256 =
            "fe5eb64fa2e4154c83f8e4935e82871c850c154387ee892e0ab65fe179e7d8c9";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private DeepFilterModelStore() {
    }

    public static Path modelPath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("notsosimplevoicechat")
                .resolve(MODEL_FILE);
    }

    public static Path ensureModel() throws IOException {
        Path path = modelPath();
        if (Files.isRegularFile(path) && verifySha256(path)) {
            return path;
        }

        Path devCopy = FabricLoader.getInstance().getGameDir().resolve("models").resolve(MODEL_FILE);
        if (Files.isRegularFile(devCopy) && verifySha256(devCopy)) {
            Files.createDirectories(path.getParent());
            Files.copy(devCopy, path, StandardCopyOption.REPLACE_EXISTING);
            return path;
        }

        NotSoSimpleVoiceChatClient.LOGGER.info(
                "Lade DeepFilterNet-ONNX-Modell (~16 MB) nach {} …", path);
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), "denoiser", ".part");

        HttpRequest request = HttpRequest.newBuilder(URI.create(MODEL_URL))
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new IOException("Modell-Download HTTP " + response.statusCode());
            }
            try (InputStream in = response.body()) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Modell-Download unterbrochen", e);
        }

        if (!verifySha256(temp)) {
            Files.deleteIfExists(temp);
            throw new IOException("DeepFilter-Modell: SHA-256-Prüfsumme ungültig");
        }

        Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        NotSoSimpleVoiceChatClient.LOGGER.info("DeepFilterNet-Modell bereit.");
        return path;
    }

    private static boolean verifySha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            String hash = HexFormat.of().formatHex(digest.digest());
            return EXPECTED_SHA256.equalsIgnoreCase(hash);
        } catch (Exception e) {
            return false;
        }
    }
}
