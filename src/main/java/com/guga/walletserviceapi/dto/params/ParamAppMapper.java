package com.guga.walletserviceapi.dto.params;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.guga.walletserviceapi.model.ParamApp;

@Mapper(componentModel = "spring")
public interface ParamAppMapper {

    ParamAppResponseDTO toDto(ParamApp entity);

    ParamApp toEntity(ParamAppCreateDTO dto);

    @Mapping(target = "name", source = "paramName")
    ParamApp toEntity(String paramName, ParamAppUpdateDTO dto);
}