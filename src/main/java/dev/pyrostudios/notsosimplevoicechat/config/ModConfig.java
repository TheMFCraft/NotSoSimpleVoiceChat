package dev.pyrostudios.notsosimplevoicechat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.pyrostudios.notsosimplevoicechat.NotSoSimpleVoiceChatClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE = new ModConfig();

    public boolean enabled = true;
    public String krispApiKey = "";
    public String krispSessionToken = "";
    public int sessionTokenTtlMinutes = 180;
    public String model = "VIVA-tel";
    public double suppressionLevel = 100.0;

    public static ModConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path path = configPath();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                }
            } catch (IOException e) {
                NotSoSimpleVoiceChatClient.LOGGER.error("Konfiguration konnte nicht gelesen werden", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
        applyEnvironmentOverrides();
    }

    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            NotSoSimpleVoiceChatClient.LOGGER.error("Konfiguration konnte nicht gespeichert werden", e);
        }
    }

    public static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("notsosimplevoicechat.json");
    }

    public String resolveApiKey() {
        String env = System.getenv("KRISP_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return krispApiKey == null ? "" : krispApiKey.trim();
    }

    public String resolveSessionToken() {
        return krispSessionToken == null ? "" : krispSessionToken.trim();
    }

    private static void applyEnvironmentOverrides() {
        String envToken = System.getenv("KRISP_SESSION_TOKEN");
        if (envToken != null && !envToken.isBlank()) {
            INSTANCE.krispSessionToken = envToken.trim();
        }
    }
}
