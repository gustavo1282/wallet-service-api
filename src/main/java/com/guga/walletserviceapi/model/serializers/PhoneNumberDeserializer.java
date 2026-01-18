package com.guga.walletserviceapi.model.serializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class PhoneNumberDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null) return null;

        // 1. Remove tudo que NÃO for número
        String numbers = value.replaceAll("\\D", "");

        // 2. Aplica a formatação se tiver 11 dígitos (Celular com DDD)
        if (numbers.length() == 11) {
            return String.format("(%s) %s-%s", 
                numbers.substring(0, 2), 
                numbers.substring(2, 7), 
                numbers.substring(7));
        }
        
        // 3. Aplica a formatação se tiver 10 dígitos (Fixo com DDD)
        if (numbers.length() == 10) {
            return String.format("(%s) %s-%s", 
                numbers.substring(0, 2), 
                numbers.substring(2, 6), 
                numbers.substring(6));
        }

        // Se não tiver o tamanho esperado, retorna o original (vai falhar na validação @Pattern depois, o que é correto)
        return value; 
    }
}
