package dev.pyrostudios.notsosimplevoicechat.krisp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class KrispSessionTokenService {
    private static final URI TOKEN_URI = URI.create("https://api.developers.krisp.ai/v2/sdk/tokens/session");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private KrispSessionTokenService() {
    }

    public static String createVoiceIsolationToken(String apiKey, int ttlMinutes) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Kein Krisp API-Key konfiguriert");
        }

        int ttl = Math.clamp(ttlMinutes, 5, 300);
        String body = """
                {"usage_type":"viva","expiration_ttl":%d}
                """.formatted(ttl);

        HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "api-key " + apiKey.trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Token-Anfrage unterbrochen", e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new IOException("Krisp Token HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (root.has("data") && root.getAsJsonObject("data").has("session_key")) {
            return root.getAsJsonObject("data").get("session_key").getAsString();
        }

        NotSoSimpleVoiceChatClient.LOGGER.error("Unerwartete Krisp-Antwort: {}", response.body());
        throw new IOException("Krisp session_key fehlt in der Antwort");
    }
}
