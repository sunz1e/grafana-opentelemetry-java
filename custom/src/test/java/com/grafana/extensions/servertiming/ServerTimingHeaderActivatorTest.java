/*
 * Copyright Grafana Labs
 * SPDX-License-Identifier: Apache-2.0
 */

package com.grafana.extensions.servertiming;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerTimingHeaderActivatorTest {

  private static final String EMIT_RESPONSE_HEADERS =
      "grafana.otel.trace-response-header.enabled";
  private static final String CONFIG_FILE = "otel.config.file";
  private static final Map<String, String> NO_EXPORTERS =
      Map.of(
          "otel.traces.exporter", "none",
          "otel.metrics.exporter", "none",
          "otel.logs.exporter", "none");

  private final ServerTimingHeaderActivator activator = new ServerTimingHeaderActivator();
  private String originalConfigFile;

  @BeforeEach
  void setUp() {
    originalConfigFile = System.getProperty(CONFIG_FILE);
    System.clearProperty(CONFIG_FILE);
    ServerTimingHeaderCustomizer.enabled = false;
  }

  @AfterEach
  void tearDown() {
    if (originalConfigFile == null) {
      System.clearProperty(CONFIG_FILE);
    } else {
      System.setProperty(CONFIG_FILE, originalConfigFile);
    }
    ServerTimingHeaderCustomizer.enabled = false;
  }

  @Test
  void enabledByDefault() {
    activate(Map.of());

    assertThat(ServerTimingHeaderCustomizer.enabled).isTrue();
  }

  @Test
  void disabledByProperty() {
    activate(Map.of(EMIT_RESPONSE_HEADERS, "false"));

    assertThat(ServerTimingHeaderCustomizer.enabled).isFalse();
  }

  @Test
  void enabledWithDeclarativeConfig(@TempDir Path tempDir) throws Exception {
    Path configFile = tempDir.resolve("otel.yaml");
    Files.writeString(configFile, "file_format: \"1.0\"\n");
    System.setProperty(CONFIG_FILE, configFile.toString());

    activate(Map.of());

    assertThat(ServerTimingHeaderCustomizer.enabled).isTrue();
  }

  private void activate(Map<String, String> properties) {
    Map<String, String> configProperties = new HashMap<>(NO_EXPORTERS);
    configProperties.putAll(properties);
    AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk =
        AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier(() -> configProperties)
            .build();
    try {
      activator.afterAgent(autoConfiguredOpenTelemetrySdk);
    } finally {
      autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk().close();
    }
  }
}
