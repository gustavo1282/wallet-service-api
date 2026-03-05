package com.guga.walletserviceapi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.guga.walletserviceapi.dto.webhook.AlertmanagerWebhook;

@Service
public class AlertProcessor {

  private static final Logger LOGGER = LogManager.getLogger(AlertProcessor.class);


  public void process(AlertmanagerWebhook payload) {
    if (payload.alerts() == null || payload.alerts().isEmpty()) {
        LOGGER.info("Alertmanager webhook recebido sem alertas. status={}", payload.status());
        return;
    }

    for (var a : payload.alerts()) {
        var labels = a.labels();
        var annotations = a.annotations();

        String alertname = labels != null ? labels.getOrDefault("alertname", "unknown") : "unknown";
        String severity = labels != null ? labels.getOrDefault("severity", "NA") : "NA";
        String service = firstNonNull(
            labels != null ? labels.get("service_name") : null,
            labels != null ? labels.get("service") : null,
            labels != null ? labels.get("application") : null
        );

        String summary = annotations != null ? annotations.getOrDefault("summary", "") : "";
        String desc = annotations != null ? annotations.getOrDefault("description", "") : "";

        LOGGER.warn("ALERT [{}] status={} severity={} service={} startsAt={} summary='{}' desc='{}' labels={}",
            alertname,
            a.status(),
            severity,
            service,
            a.startsAt(),
            summary,
            desc,
            labels
        );

        // Aqui é onde você conecta:
        // - salvar no banco
        // - publicar em RabbitMQ
        // - mandar Slack
        // - criar incidente, etc.
    }
  }

  private static String firstNonNull(String... values) {
    for (String v : values) if (v != null && !v.isBlank()) return v;
    return "unknown";
  }
}