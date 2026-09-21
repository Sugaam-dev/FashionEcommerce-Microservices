package com.pmrgsolution.shared.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot {@link EnvironmentPostProcessor} that automatically finds and loads
 * the project's root {@code .env} file into the Spring {@link org.springframework.core.env.Environment}
 * with highest precedence before configuration files (like application.yaml) are resolved.
 * <p>
 * This allows all microservices to run seamlessly in any IDE, terminal, or container
 * without requiring hardcoded fallbacks in YAML files.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);
    private static final String PROPERTY_SOURCE_NAME = "dotenvProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File envFile = findDotenvFile();
        if (envFile == null || !envFile.exists() || !envFile.isFile()) {
            return;
        }

        Map<String, Object> envProperties = parseEnvFile(envFile);
        if (!envProperties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, envProperties));
            log.info("Successfully loaded {} environment variables from .env file at: {}", envProperties.size(), envFile.getAbsolutePath());
        }
    }

    private File findDotenvFile() {
        File currentDir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 5 && currentDir != null; i++) {
            File candidate = new File(currentDir, ".env");
            if (candidate.exists() && candidate.isFile()) {
                return candidate;
            }
            File candidateInParent = new File(currentDir, "fashion-parent/.env");
            if (candidateInParent.exists() && candidateInParent.isFile()) {
                return candidateInParent;
            }
            currentDir = currentDir.getParentFile();
        }
        return null;
    }

    private Map<String, Object> parseEnvFile(File file) {
        Map<String, Object> properties = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex > 0) {
                    String key = line.substring(0, separatorIndex).trim();
                    String value = line.substring(separatorIndex + 1).trim();

                    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                        if (value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                    } else {
                        // Strip unquoted inline comments (e.g. 900000 # 15 minutes)
                        int commentIndex = value.indexOf('#');
                        if (commentIndex >= 0) {
                            value = value.substring(0, commentIndex).trim();
                        }
                    }
                    properties.put(key, value);
                    // Also set in System properties so non-Spring components can read them
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read .env file from {}: {}", file.getAbsolutePath(), e.getMessage());
        }
        return properties;
    }
}
