package com.guga.walletserviceapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.guga.walletserviceapi.dto.webhook.AlertmanagerWebhook;
import com.guga.walletserviceapi.service.AlertProcessor;

@WebMvcTest(controllers = AlertWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
//@Import({A.class})
class AlertWebhookControllerTests extends BaseControllerTest {

    @MockitoBean
    private AlertProcessor alertProcessor;

    @Test
    @DisplayName("Deve processar webhook do Alertmanager com sucesso")
    void receive_ok() throws Exception {

        // 1. Criar o objeto de alerta individual (Record interno)
        AlertmanagerWebhook.Alert alert = new AlertmanagerWebhook.Alert(
            "firing",
            Map.of("alertname", "HighCpuUsage", "severity", "critical", "service", "wallet-api"),
            Map.of("summary", "CPU acima de 90%", "description", "O pod está operando com carga crítica"),
            OffsetDateTime.parse("2026-04-06T12:00:00Z"), // Converte String para OffsetDateTime
            null, // endsAt pode ser nulo se estiver 'firing'
            "http://prometheus.local",
            "abc123fingerprint"
        );


        // 2. Criar o Webhook completo (Record pai) que o Controller recebe
        AlertmanagerWebhook payload = new AlertmanagerWebhook(
            "firing",
            List.of(alert), // O Alert vai dentro de uma lista
            Map.of("alertname", "HighCpuUsage"),
            Map.of("severity", "critical"),
            Map.of("summary", "CPU acima de 90%"),
            "http://alertmanager.local",
            "4"
        );

        doNothing().when(alertProcessor).process(any(AlertmanagerWebhook.class));

        // Act & Assert
        performRequest(HttpMethod.POST, "/alerts/webhook", payload, null)
            .andExpect(status().isOk());

        verify(alertProcessor, times(1)).process(any(AlertmanagerWebhook.class));
    }

    @Test
    @DisplayName("Deve retornar 200 mesmo com lista de alertas vazia (delegação para o service)")
    void receive_emptyAlerts_ok() throws Exception {
        // Arrange
        // 1. Criar o objeto de alerta individual
        AlertmanagerWebhook.Alert alert = new AlertmanagerWebhook.Alert(
            "resolved", // Status alterado
            Map.of("alertname", "HighCpuUsage", "severity", "critical", "service", "wallet-api"),
            Map.of("summary", "CPU normalizada", "description", "O pod voltou ao consumo normal"),
            OffsetDateTime.parse("2026-04-06T12:00:00Z"), // Início do incidente
            OffsetDateTime.parse("2026-04-06T12:15:00Z"), // Fim do incidente (obrigatório em resolved)
            "http://prometheus.local",
            "abc123fingerprint"
        );

        // 2. Envelopar no Webhook record (Padrão esperado pelo Controller)
        AlertmanagerWebhook payload = new AlertmanagerWebhook(
            "resolved",
            List.of(alert),
            Map.of("alertname", "HighCpuUsage"),
            Map.of("severity", "critical"),
            Map.of("summary", "CPU normalizada"),
            "http://alertmanager.local",
            "4"
        );

        // Act & Assert
        performRequest(HttpMethod.POST, "/alerts/webhook", payload, null)
            .andExpect(status().isOk());

        verify(alertProcessor, times(1)).process(any(AlertmanagerWebhook.class));
    }

    @Test
    @DisplayName("Deve retornar 400 ao enviar JSON malformado")
    void receive_badRequest_malformedJson() throws Exception {
        // Act & Assert
        // Enviando uma String que não é um JSON válido
        performRequest(HttpMethod.POST, 
            "/alerts/webhook",
            "{ invalid-json }",
            null)
            .andExpect(status().isBadRequest());
    }

    @Override
    public void loadMockData() {
        // Não é necessário carregar sementes de banco para este controller de webhook
    }
}
