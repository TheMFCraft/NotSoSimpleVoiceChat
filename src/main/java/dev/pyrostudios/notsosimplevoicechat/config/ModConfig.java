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

    /** Addon-Rauschunterdrückung aktiv. */
    public boolean enabled = true;
    /** Simple-Voice-Chat-RNNoise deaktivieren, wenn das Addon aktiv ist (empfohlen). */
    public boolean replaceSvcDenoiser = true;
    /** Anteil des gefilterten Signals in Prozent (0 = trocken, 100 = nur gefiltert). */
    public double strengthPercent = 100.0;
    /** Lokale Engine: DEEPFILTER oder RNNOISE. */
    public String engine = DenoiseEngine.DEEPFILTER.name();

    /** Legacy (Krisp / alte Config) – wird beim Laden in {@link #strengthPercent} übernommen. */
    public double suppressionLevel = 100.0;
    public String krispApiKey = "";
    public String krispSessionToken = "";
    public int sessionTokenTtlMinutes = 180;
    public String model = "VIVA-tel";

    public static ModConfig get() {
        return INSTANCE;
    }

    public DenoiseEngine engineEnum() {
        try {
            return DenoiseEngine.valueOf(engine == null ? "" : engine.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DenoiseEngine.DEEPFILTER;
        }
    }

    public static void load() {
        Path path = configPath();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                INSTANCE = loaded == null ? new ModConfig() : migrateLegacy(loaded);
            } catch (IOException e) {
                NotSoSimpleVoiceChatClient.LOGGER.error("Konfiguration konnte nicht gelesen werden", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    private static ModConfig migrateLegacy(ModConfig loaded) {
        if (loaded.strengthPercent <= 0.0 && loaded.suppressionLevel > 0.0) {
            loaded.strengthPercent = loaded.suppressionLevel;
        }
        if (loaded.strengthPercent <= 0.0) {
            loaded.strengthPercent = 100.0;
        }
        return loaded;
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

    public static void applyFromDraft(ModConfig draft) {
        INSTANCE.enabled = draft.enabled;
        INSTANCE.replaceSvcDenoiser = draft.replaceSvcDenoiser;
        INSTANCE.strengthPercent = draft.strengthPercent;
        INSTANCE.engine = draft.engine == null ? DenoiseEngine.DEEPFILTER.name() : draft.engine;
    }
}
