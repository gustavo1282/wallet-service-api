package com.guga.walletserviceapi.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guga.walletserviceapi.helpers.GlobalHelper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@JsonPropertyOrder({
    "id", "name", "description", "valueString", "valueNumber", "valueDate", "valueDateTime", "valueBoolean"
})
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "tb_param_app")
public class ParamApp {

    public static final String SEQ_CUSTOMER_ID = "seq-customer-id";
    public static final String SEQ_WALLET_ID = "seq-wallet-id";
    public static final String SEQ_TRANSACTION_ID = "seq-transaction-id";
    public static final String SEQ_DEPOSIT_SENDER_ID = "seq-deposit-sender-id";
    public static final String SEQ_MOVEMENT_TRN_ID = "seq-movement-trn-id";
    public static final String SEQ_LOGIN_AUTH_ID = "seq-movement-trn-id";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(name = "name", nullable = false, length = 30, unique = true, updatable = false)
    private String name;
    
    @Column(name = "description", nullable = false, length = 45)
    private String description;
    
    @Column(name = "valueString", nullable = true, length = 25)
    private String valueString;

    @Column(name = "valueLong", nullable = true)
    private Long valueLong;

    @Column(name = "valueInteger", nullable = true)
    private Integer valueInteger;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE)
    @Column(name = "valueDate", nullable = true)
    private LocalDate valueDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "valueDateTime", nullable = true)
    private LocalDateTime valueDateTime;

    @Column(name = "valueBoolean", nullable = true)
    private boolean valueBoolean;


    public static ParamApp newParam(String name, String description, Object value) {
        ParamApp newParam = ParamApp.builder()
            .name(name)
            .description(description)
            .build();

        if (value instanceof Integer) {
            newParam.setValueInteger((Integer)value);
        } else if (value instanceof Long) {
            newParam.setValueLong((Long)value);
        } else if (value instanceof Boolean) {
            newParam.setValueBoolean((Boolean)value);
        } else if (value instanceof LocalDate) {
            newParam.setValueDate((LocalDate)value);
        } else if (value instanceof LocalDateTime) {
            newParam.setValueDateTime((LocalDateTime)value);
        } else {
            newParam.setValueString((String)value);
        }

        return newParam;
    }

}