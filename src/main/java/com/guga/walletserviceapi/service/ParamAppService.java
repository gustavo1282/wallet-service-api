package com.guga.walletserviceapi.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.model.ParamApp;
import com.guga.walletserviceapi.repository.ParamAppRepository;

import lombok.RequiredArgsConstructor;

@EnableCaching
@RequiredArgsConstructor
@Service
public class ParamAppService {

    private final ParamAppRepository paramAppRepository;

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
    @SuppressWarnings("null")
    @Transactional
    public ParamApp updateByName(String paramName, ParamApp paramAppUpdate) {
        
        Optional<ParamApp> existsParamApp = paramAppRepository.findByName(paramName);

        // 3. Se não encontrar, lança exceção (ou NullPointerException, dependendo do design)
        ParamApp paramApp = existsParamApp.orElseThrow(() -> 
            new ResourceBadRequestException("Parâmetro não encontrado com o identificador fornecido: " + paramName)
        );

        applyRuleUpdate(paramApp, paramAppUpdate);

        return paramAppRepository.save(paramApp);
    }

    private void applyRuleUpdate(ParamApp paramApp, ParamApp valueUpdate) {

        // // paramApp.setStatus(paramAppUpdate.getStatus());
        // paramApp.setValueBoolean(false);
        // paramApp.setValueDate(null);
        // paramApp.setValueDateTime(null);
        // paramApp.setValueInteger(null);
        // paramApp.setValueLong(null);
        // paramApp.setValueString(null);

        // if (paramApp.getName().startsWith("seq")) {
        //     paramApp.setValueLong( paramApp.getValueLong() +1L );
        // } 
        // else {
        //     if (valueUpdate instanceof Integer) {
        //         paramApp.setValueInteger((Integer)valueUpdate);
        //     } else if (valueUpdate instanceof Long) {
        //         paramApp.setValueLong((Long)valueUpdate);
        //     } else if (valueUpdate instanceof Boolean) {
        //         paramApp.setValueBoolean((Boolean)valueUpdate);
        //     } else if (valueUpdate instanceof LocalDate) {
        //         paramApp.setValueDate((LocalDate)valueUpdate);
        //     } else if (valueUpdate instanceof LocalDateTime) {
        //         paramApp.setValueDateTime((LocalDateTime)valueUpdate);
        //     } else {
        //         paramApp.setValueString((String)valueUpdate);
        //     }
        // }
    }


    // --- R (READ) ---

    // Busca todos os parâmetros
    public List<ParamApp> findAll(Pageable pageable) {
        Page<ParamApp> page = paramAppRepository.findAll(pageable);
        if (page.isEmpty() || page.getContent().isEmpty()) {
            throw new ResourceNotFoundException("Nenhum parâmetro encontrado para os critérios informados.");
        }
        return page.getContent();
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

    
    @Cacheable(value = "params_app", key = "'minAmountToDeposit'")
    protected BigDecimal getMinAmountToDeposit() {
        Optional<ParamApp> paramApp = paramAppRepository.findByName(ParamApp.LIMIT_MIN_TO_DEPOSIT);
        if (!paramApp.isPresent()) {
            throw new ResourceBadRequestException("Parâmetro de configuração não encontrado: " + ParamApp.LIMIT_MIN_TO_DEPOSIT);
        }
        return paramApp.get().getValueBigDecimal();        
    }

    @Cacheable(value = "params_app", key = "'minAmountToTransfer'")
    protected BigDecimal getMinAmountToTransfer() {
        Optional<ParamApp> paramApp = paramAppRepository.findByName(ParamApp.LIMIT_MIN_TO_TRANSFER);
        if (!paramApp.isPresent()) {
            throw new ResourceBadRequestException("Parâmetro de configuração não encontrado: " + ParamApp.LIMIT_MIN_TO_TRANSFER);
        }
        return paramApp.get().getValueBigDecimal();
    }


}