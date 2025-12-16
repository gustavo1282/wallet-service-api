package com.guga.walletserviceapi.model;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guga.walletserviceapi.helpers.GlobalHelper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@JsonPropertyOrder({
    "sessionId", "sequenceId", "userAgent", "ipAddress", "username"
})
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//@Entity
//@Table(name = "tb_audit_contexts")
//@EqualsAndHashCode(of = "customerId")
@ToString
public class AuditContext {
    private String sessionId;
    private String userAgent;
    private String ipAddress;
    private String username;
    private String traceId;
    private String info;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Builder.Default
    private LocalDateTime createAt = LocalDateTime.now();

    @Builder.Default
    private Long sequenceId = LocalDateTime.now()
        .atZone(ZoneId.of("America/Sao_Paulo")).toInstant().toEpochMilli();

    @Override
    public String toString() {
        return "AuditContext(sessionId=" + sessionId + 
            ", username=" + username + 
            ", sequenceId=" + sequenceId + 
            ", createAt=" + createAt + 
            ", ipAddress=" + ipAddress + 
            ", info=" + info + ")"
            ;
    }

}
