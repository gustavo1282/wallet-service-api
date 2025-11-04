package com.guga.walletserviceapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "tb_deposit_terminal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder(toBuilder = true)
public class DepositTerminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_id", nullable = false)
    @EqualsAndHashCode.Include
    private Long depositId;

    @Column(name = "terminalId", length = 12)
    private String terminalId;

    @Column(name = "depositor", length = 40)
    private String depositor;

    @Column(name = "documentId", length = 18)
    private String documentId;

    @Digits(integer = 14, fraction = 2, message = "Amount must have up to 14 integer digits and 2 decimal places")
    @Column(name = "amount", nullable = true)
    private BigDecimal amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
