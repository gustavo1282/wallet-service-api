package com.guga.walletserviceapi.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.ParamAppRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class ParamAppService {

    @Autowired
    private ParamAppRepository paramAppRepository;

    public ParamApp getNextSequenceId(String paramName) {
        Optional<ParamApp> findParamApp = paramAppRepository.findByName(paramName);

        ParamApp paramApp = findParamApp.orElseThrow(() -> 
            new ResourceBadRequestException("Parâmetro não encontrado com o identificador fornecido: " + paramName)
        );

        if (paramApp.getName().startsWith("seq")) {
            paramApp.setValueLong( paramApp.getValueLong() +1L );
        } 

        return paramAppRepository.save(paramApp);
    }

    
    public ParamApp adjstSequenceId(String paramName, Long value) {
        Optional<ParamApp> findParamApp = paramAppRepository.findByName(paramName);

        ParamApp paramApp = findParamApp.orElseThrow(() -> 
            new ResourceBadRequestException("Parâmetro não encontrado com o identificador fornecido: " + paramName)
        );

        paramApp.setValueLong( value );

        return paramAppRepository.save(paramApp);
    }


    // Busca todos os parâmetros
    public ParamApp save(ParamApp paramAppInput) {
        return paramAppRepository.save(paramAppInput);
    }


    /**
     * Atualiza um parâmetro, buscando pelo ID ou pelo Name se o ID for nulo.
     * @param identifier O ID (Long) ou o Name (String) para buscar o parâmetro existente.
     * @param updatedParam A entidade ParamApp com os novos dados.
     * @return A entidade ParamApp atualizada.
     * @throws RuntimeException se o parâmetro não for encontrado.
     */
    @Transactional
    public ParamApp updateByName(String paramName, ParamApp paramAppUpdate) {
        
        Optional<ParamApp> existsParamApp = paramAppRepository.findByName(paramName);

        // 3. Se não encontrar, lança exceção (ou NullPointerException, dependendo do design)
        ParamApp paramApp = existsParamApp.orElseThrow(() -> 
            new ResourceBadRequestException("Parâmetro não encontrado com o identificador fornecido: " + paramName)
        );

        paramApp.setValueBoolean(paramAppUpdate.isValueBoolean());
        paramApp.setValueDate(paramAppUpdate.getValueDate());
        paramApp.setValueDateTime(paramAppUpdate.getValueDateTime());
        paramApp.setValueInteger(paramAppUpdate.getValueInteger());
        paramApp.setValueLong(paramAppUpdate.getValueLong());
        paramApp.setValueString(paramAppUpdate.getValueString());

        return paramAppRepository.save(paramApp);
    }

    private void applyRuleUpdate(ParamApp paramApp, Object valueUpdate) {
        if (paramApp.getName().startsWith("seq")) {
            paramApp.setValueLong( paramApp.getValueLong() +1L );
        } 
        else {
            if (valueUpdate instanceof Integer) {
                paramApp.setValueInteger((Integer)valueUpdate);
            } else if (valueUpdate instanceof Long) {
                paramApp.setValueLong((Long)valueUpdate);
            } else if (valueUpdate instanceof Boolean) {
                paramApp.setValueBoolean((Boolean)valueUpdate);
            } else if (valueUpdate instanceof LocalDate) {
                paramApp.setValueDate((LocalDate)valueUpdate);
            } else if (valueUpdate instanceof LocalDateTime) {
                paramApp.setValueDateTime((LocalDateTime)valueUpdate);
            } else {
                paramApp.setValueString((String)valueUpdate);
            }
        }
    }


    // --- R (READ) ---

    // Busca todos os parâmetros
    public List<ParamApp> findAll() {
        return paramAppRepository.findAll();
    }

    // Busca por ID
    public ParamApp findById(Long id) {
        return paramAppRepository.findById(id)
                .orElseThrow(() -> 
                    new ResourceNotFoundException("ParamApp not found with id: " + String.valueOf(id))
                );
    }

    // Busca por Name
    public Optional<ParamApp> findByName(String name) {
        return paramAppRepository.findByName(name);
    }


    // --- D (DELETE) ---

    // Exclui um parâmetro pelo ID
    @Transactional
    public int deleteById(Long id) {
        try {
            paramAppRepository.deleteById(id);
            return 0;
        } catch (Exception e) {
            throw new ResourceBadRequestException(e.getMessage());
        }
    }
}