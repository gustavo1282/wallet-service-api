package com.guga.walletserviceapi.model;

import com.guga.walletserviceapi.model.converter.StatusConverter;
import com.guga.walletserviceapi.model.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_Id", nullable = false)
    private Long customerId;

    @Pattern(regexp = "\\d+", message = "Phone number must contain only digits")
    @Column(name = "document_Id", unique = true, nullable = false, length = 20)
    private String documentId;

    @Column(name = "birth_Date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "first_Name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "last_Name", nullable = false, length = 30)
    private String lastName;

    @Email(message = "Email should be valid")
    @Column(name = "email", unique = true, nullable = false, length = 80)
    private String email;

    @Column(name = "full_Name", nullable = false, length = 80)
    private String fullName;

    @NotNull(message = "Phone number cannot be null")
    @Pattern(regexp = "\\d+", message = "Phone number must contain only digits")
    @Column(name = "phone_Number", unique = true, nullable = false, length = 14)
    private String phoneNumber;

    @Column(name = "created_At", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_At", nullable = false)
    @CreationTimestamp
    private LocalDateTime updatedAt;

    @Convert(converter = StatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    /**
     * Construtor para realizar uma Cópia Profunda (Deep Copy) do objeto Customer.
     * Copia todos os campos para uma nova instância.
     *
     * @param other O objeto Customer a ser copiado.
     */
    public Customer(Customer other) {
        this.customerId = other.customerId;
        this.documentId = other.documentId;
        this.birthDate = other.birthDate;
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.email = other.email;
        this.fullName = other.fullName;
        this.phoneNumber = other.phoneNumber;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
        this.status = other.status; // Enums são seguras para cópia de referência
    }

    /**
     * Retorna uma cópia profunda (Deep Copy) do Customer.
     * @return Uma nova instância de Customer.
     */
    public Customer cloneCustomer() {
        return new Customer(this);
    }

}
