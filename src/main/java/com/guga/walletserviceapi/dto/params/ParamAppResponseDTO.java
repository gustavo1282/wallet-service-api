package com.guga.walletserviceapi.dto.params;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ParamAppResponseDTO(
    Long id,
    String name,
    String description,
    String valueString,
    Long valueLong,
    Integer valueInteger,
    BigDecimal valueBigDecimal,
    LocalDate valueDate,
    LocalDateTime valueDateTime,
    boolean valueBoolean
) 
{ }