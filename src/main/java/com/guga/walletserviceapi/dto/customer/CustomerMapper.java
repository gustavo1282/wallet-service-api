package com.guga.walletserviceapi.dto.customer;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.guga.walletserviceapi.model.Customer;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    //@Mapping(target = "fullName", expression = "java(entity.getFirstName() + \" \" + entity.getLastName())")
    CustomerResponseDTO toDto(Customer entity);

    Customer toEntity(CustomerCreateDTO dto);

    Customer toEntity(CustomerUpdateDTO dto);

    @Mapping(target = "customerId", ignore = true) // ID não deve ser atualizado via DTO
    @Mapping(target = "cpf", ignore = true) // CPF não deve ser atualizado via DTO
    @Mapping(target = "documentId", ignore = true) // DocumentId não deve ser atualizado via DTO
    @Mapping(target = "birthDate", ignore = true) // BirthDate não deve ser atualizado via DTO
    void updateEntityFromDto(CustomerUpdateDTO dto, @MappingTarget Customer entity);
}
