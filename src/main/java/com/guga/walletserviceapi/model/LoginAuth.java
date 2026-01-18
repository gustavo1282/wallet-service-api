package com.guga.walletserviceapi.model;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.converter.LoginAuthTypeConverter;
import com.guga.walletserviceapi.model.converter.LoginRoleConverter;
import com.guga.walletserviceapi.model.converter.StatusConverter;
import com.guga.walletserviceapi.model.enums.LoginAuthType;
import com.guga.walletserviceapi.model.enums.LoginRole;
import com.guga.walletserviceapi.model.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonPropertyOrder({
    "id", "login", "status", "loginAuthType", "customerId", "walletId", "accessKey", "lastLoginAt", "createdAt", "updatedAt"
})
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "tb_login_auth")
public class LoginAuth {

    @Id
    @Column(name = "id", nullable = false, unique = true)
    @JsonProperty("id")
    private Long id;

    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false, length = 2)
    private Status status;

    @NotNull(message = "Customer cannot be null")
    @Column(name = "customer_id_fk", nullable = false)
    private Long customerId;


    @NotNull(message = "Wallet cannot be null")
    @Column(name = "wallet_id_fk", nullable = false, unique = true)
    private Long walletId;


    @Convert(converter = LoginAuthTypeConverter.class)
    @Column(name = "loginAuthType", nullable = false, length = 2)
    private LoginAuthType loginAuthType;


    @Column(name = "login", nullable = false, length = 80)
    private String login;


    @Column(name = "access_key", nullable = false, length = 256)
    private String accessKey;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "last_login_at", nullable = true)
    private LocalDateTime lastLoginAt;

    
    @Convert(converter = LoginRoleConverter.class)
    @Column(name = "role", length = 180)
    private List<LoginRole> role;

}
