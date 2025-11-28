package com.guga.walletserviceapi.model.converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.opencsv.bean.AbstractBeanField;

public class LocalDateTimeCsvConverter extends AbstractBeanField<LocalDateTime, String> {

    @Override
    protected LocalDateTime convert(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String newValue = value
                            .replaceAll("[^\\x20-\\x7E]", "")
                            .replaceAll("[\\p{Cntrl}]", "")
                            .trim();
                            
        if (newValue.length() > 19 && newValue.length() >= 23) {
            newValue = newValue.substring(0, 23);
        } 
        else if (newValue.length() == 19) {
            newValue = newValue.substring(0, 19).concat(".000");
        }

        return LocalDateTime.parse(
                newValue,
                DateTimeFormatter.ofPattern(GlobalHelper.PATTERN_FORMAT_DATE_TIME));
    }
}