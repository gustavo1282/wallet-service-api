package com.guga.walletserviceapi.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.guga.walletserviceapi.model.converter.LocalDateCsvConverter;
import com.guga.walletserviceapi.model.converter.LocalDateTimeCsvConverter;
import com.guga.walletserviceapi.model.converter.StatusConverter;
import com.guga.walletserviceapi.model.enums.Status;
import com.opencsv.bean.CsvCustomBindByName;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// @Data
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "customerId")
@Entity
@Table(name = "tb_customer")
public class Customer {

    @Id
    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;


    @NotBlank(message = "Document ID field is required")
    @Column(name = "documentId", nullable = false, length = 20)
    private String documentId;
    

    @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF field is required")
    @Column(name = "cpf", unique = true, nullable = false, length = 20)
    private String cpf;


    @CsvCustomBindByName(column = "birthDate", converter = LocalDateCsvConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE)
    @Column(name = "birth_Date", nullable = false)
    private LocalDate birthDate;

    
    @NotBlank(message = "FirstName is required.")
    @Column(name = "first_Name", nullable = false, length = 30)
    private String firstName;

    

    @NotBlank(message = "LastName is Required.")
    @Column(name = "last_Name", nullable = false, length = 30)
    private String lastName;

    
    @Email(message = "Email should be valid")
    @Column(name = "email", unique = true, nullable = false, length = 80)
    private String email;

    
    @NotBlank(message = "Fullname is required")
    @Column(name = "full_name", nullable = false, length = 80)
    private String fullName;

    
    @NotNull(message = "Phone number cannot be null")
    @Pattern(regexp = "^\\(\\d{2}\\)\\s*9?\\d{4}-\\d{4}$", message = "Phone number must be in the format")
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    
    @CsvCustomBindByName(column = "createdAt", converter = LocalDateTimeCsvConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    
    @CsvCustomBindByName(column = "updatedAt", converter = LocalDateTimeCsvConverter.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = GlobalHelper.PATTERN_FORMAT_DATE_TIME)
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false, length = 2)
    private Status status;
    
    
    @Column(name = "login_auth_id_fk",  nullable = true, insertable = true, updatable = true)
    private Long loginAuthId;


    //@Schema(description = "LoginAuth associated with the Customer", accessMode = Schema.AccessMode.READ_ONLY)
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "login_auth_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    //private LoginAuth loginAuth;


}
