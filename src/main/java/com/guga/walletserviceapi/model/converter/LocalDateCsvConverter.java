package com.guga.walletserviceapi.model.converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.guga.walletserviceapi.helpers.GlobalHelper;
import com.opencsv.bean.AbstractBeanField;

public class LocalDateCsvConverter extends AbstractBeanField<LocalDate, String> {

    @Override
    protected LocalDate convert(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
    
        String newValue = value
                            .replaceAll("[^\\x20-\\x7E]", "")
                            .replaceAll("[\\p{Cntrl}]", "")
                            .trim();

        if (newValue.length() > 10) {
            newValue = newValue.substring(0, 10);
        }
            
        return LocalDate.parse(
                newValue,
                DateTimeFormatter.ofPattern(GlobalHelper.PATTERN_FORMAT_DATE));
    }

}