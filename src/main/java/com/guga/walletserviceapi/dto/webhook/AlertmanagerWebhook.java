package com.guga.walletserviceapi.dto.webhook;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertmanagerWebhook(
    String status,
    List<Alert> alerts,
    Map<String, String> groupLabels,
    Map<String, String> commonLabels,
    Map<String, String> commonAnnotations,
    String externalURL,
    String version
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Alert(
      String status,
      Map<String, String> labels,
      Map<String, String> annotations,
      OffsetDateTime startsAt,
      OffsetDateTime endsAt,
      String generatorURL,
      String fingerprint
  ) {}
}