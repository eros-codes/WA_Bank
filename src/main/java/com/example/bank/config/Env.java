package com.example.bank.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Settings that must not live in the code: read from the environment first,
 * then from a {@code .env} file. The file is looked for at the path in
 * {@code BANK_ENV_FILE}, else in the working directory.
 *
 * <p>Format: one {@code KEY=value} per line; blank lines and lines starting
 * with {@code #} are ignored; a value may be wrapped in single or double quotes.
 */
public final class Env {

    private final Map<String, String> fileValues;

    private Env(Map<String, String> fileValues) {
        this.fileValues = fileValues;
    }

    /** Loads the .env file if there is one; a missing file is not an error. */
    public static Env load() {
        String custom = System.getenv("BANK_ENV_FILE");
        Path path = Path.of(custom != null && !custom.isBlank() ? custom : ".env");
        return new Env(Files.isRegularFile(path) ? parse(path) : Map.of());
    }

    static Map<String, String> parse(Path path) {
        Map<String, String> values = new HashMap<>();
        try {
            for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path.toAbsolutePath(), e);
        }
        return values;
    }

    /** The value from the environment, else from the file. */
    public Optional<String> get(String key) {
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) return Optional.of(fromEnv);
        return Optional.ofNullable(fileValues.get(key)).filter(v -> !v.isBlank());
    }

    /** A value the app cannot run without; the message says where to set it. */
    public String require(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException(
                key + " is not set. Copy .env.example to .env and fill it in, or set " + key + " in the environment."));
    }
}
