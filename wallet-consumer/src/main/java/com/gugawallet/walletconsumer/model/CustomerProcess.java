package com.gugawallet.walletconsumer.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "customers")
public class CustomerProcess {

    @Id
    private String id;

    private Long customerId;

    private CustomerStatus status;

    private Integer score;

    private int retryCount;

    private LocalDateTime nextAttemptAt;

    private String lastErrorLog;

    private String processId;

    private LocalDateTime updatedAt;
}
