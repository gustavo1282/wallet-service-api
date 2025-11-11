package com.guga.walletserviceapi.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.guga.walletserviceapi.model.converter.StatusConverter;
import com.guga.walletserviceapi.model.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "tb_customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "customerId")
@Data
@Builder(toBuilder = true)
public class Customer {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_Id", nullable = false)
    private Long customerId;

    @Pattern(regexp = "\\d+", message = "Document ID field is required")
    @Column(name = "document_Id", nullable = false, length = 20)
    private String documentId;

    @Pattern(regexp = "\\d+", message = "CPF field is required")
    @Column(name = "cpf", unique = true, nullable = false, length = 15)
    private String cpf;
    
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
    @Column(name = "full_Name", nullable = false, length = 80)
    private String fullName;

    @NotNull(message = "Phone number cannot be null")
    @Pattern(regexp = "\\d+", message = "Phone number must contain only digits")
    @Column(name = "phone_Number", nullable = false, length = 14)
    private String phoneNumber;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_At", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_At", nullable = false)
    @CreationTimestamp
    private LocalDateTime updatedAt;

    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false, length = 2)
    private Status status;
    
}
