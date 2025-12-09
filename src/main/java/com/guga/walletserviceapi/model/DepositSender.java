package com.guga.walletserviceapi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.guga.walletserviceapi.helpers.GlobalHelper;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonPropertyOrder({
    "senderId", "createdAt", "fullName", "cpf", "amount", "terminalId"
})

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_deposit_sender")
@EqualsAndHashCode(of = "senderId")
public class DepositSender {

    @Id
    @Column(name = "deposit_sender_id", nullable = false)
    @JsonProperty("senderId")
    @Access(AccessType.FIELD)
    private Long senderId;

    //@CsvBindByName(column = "terminalId")
    @Column(name = "terminal_id", nullable = true)
    private String terminalId;

    //@CsvBindByName(column = "fullName")
    //@NotBlank(message = "FullName is required.")
    @Column(name = "full_name", nullable = true, length = 30)
    private String fullName;

    //@CsvBindByName(column = "cpf")
    @Column(name = "cpf", nullable = true, length = 20)
    private String cpf;

    //@CsvBindByName(column = "amount")
    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = true)
    private BigDecimal amount;

    //@CsvCustomBindByName(column = "createdAt", converter = LocalDateTimeCsvConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "created_at", nullable = true)
    private LocalDateTime createdAt;

}
