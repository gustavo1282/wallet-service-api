package com.guga.walletserviceapi.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger LOGGER = LogManager.getLogger(ParamAppService.class);

    private final ParamAppRepository paramAppRepository;

    @Transactional(rollbackFor = Exception.class)
    public ParamApp getNextSequenceId(String paramName) {
        LOGGER.info("PARAMAPP_SERVICE_NEXT_SEQ_ENTRY | paramName={}", paramName);

        ParamApp paramApp = paramAppRepository.findByName(paramName)
            .orElseThrow(() -> new ResourceBadRequestException("Parametro nao encontrado com o identificador fornecido: " + paramName));

        if (paramApp.getName().startsWith("seq")) {
            paramApp.setValueLong(paramApp.getValueLong() + 1L);
            LOGGER.info("PARAMAPP_SERVICE_NEXT_SEQ_DECISION | paramName={} incremented=true newValue={}",
                paramName, paramApp.getValueLong());
        } else {
            LOGGER.info("PARAMAPP_SERVICE_NEXT_SEQ_DECISION | paramName={} incremented=false", paramName);
        }

        ParamApp saved = paramAppRepository.save(paramApp);
        LOGGER.info("PARAMAPP_SERVICE_NEXT_SEQ_SUCCESS | paramName={} valueLong={}", saved.getName(), saved.getValueLong());
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public ParamApp adjstSequenceId(String paramName, Long value) {
        LOGGER.info("PARAMAPP_SERVICE_ADJUST_SEQ_ENTRY | paramName={} value={}", paramName, value);

        ParamApp paramApp = paramAppRepository.findByName(paramName)
            .orElseThrow(() -> new ResourceBadRequestException("Parametro nao encontrado com o identificador fornecido: " + paramName));

        paramApp.setValueLong(value);
        ParamApp saved = paramAppRepository.save(paramApp);

        LOGGER.info("PARAMAPP_SERVICE_ADJUST_SEQ_SUCCESS | paramName={} valueLong={}", saved.getName(), saved.getValueLong());
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public ParamApp save(ParamApp paramAppInput) {
        LOGGER.info("PARAMAPP_SERVICE_SAVE_ENTRY | name={}", paramAppInput.getName());
        ParamApp saved = paramAppRepository.save(paramAppInput);
        LOGGER.info("PARAMAPP_SERVICE_SAVE_SUCCESS | id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    @SuppressWarnings("null")
    @Transactional(rollbackFor = Exception.class)
    public ParamApp updateByName(String paramName, ParamApp paramAppUpdate) {
        LOGGER.info("PARAMAPP_SERVICE_UPDATE_ENTRY | paramName={}", paramName);

        ParamApp paramApp = paramAppRepository.findByName(paramName)
            .orElseThrow(() -> new ResourceBadRequestException("Parametro nao encontrado com o identificador fornecido: " + paramName));

        applyRuleUpdate(paramApp, paramAppUpdate);
        ParamApp saved = paramAppRepository.save(paramApp);

        LOGGER.info("PARAMAPP_SERVICE_UPDATE_SUCCESS | id={} name={}", saved.getId(), saved.getName());
        return saved;
    }

    private void applyRuleUpdate(ParamApp paramApp, ParamApp valueUpdate) {
        // Regras especificas de update podem ser aplicadas aqui.
    }

    @Transactional(readOnly = true)
    public List<ParamApp> findAll(Pageable pageable) {
        LOGGER.info("PARAMAPP_SERVICE_LIST_ENTRY | page={} size={} sort={}",
            pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<ParamApp> page = paramAppRepository.findAll(pageable);
        if (page.isEmpty() || page.getContent().isEmpty()) {
            LOGGER.warn("PARAMAPP_SERVICE_LIST_DECISION | decision=EMPTY_RESULT");
            throw new ResourceNotFoundException("Nenhum parametro encontrado para os criterios informados.");
        }

        LOGGER.info("PARAMAPP_SERVICE_LIST_SUCCESS | rows={}", page.getNumberOfElements());
        return page.getContent();
    }

    @Transactional(readOnly = true)
    public ParamApp findById(Long id) {
        LOGGER.info("PARAMAPP_SERVICE_GET_BY_ID_ENTRY | id={}", id);

        ParamApp found = paramAppRepository.findById(id).orElse(null);
        if (found == null) {
            LOGGER.warn("PARAMAPP_SERVICE_GET_BY_ID_DECISION | id={} decision=NOT_FOUND", id);
            throw new ResourceNotFoundException("ParamApp not found with id: " + String.valueOf(id));
        }

        LOGGER.info("PARAMAPP_SERVICE_GET_BY_ID_SUCCESS | id={} name={}", found.getId(), found.getName());
        return found;
    }

    @Transactional(readOnly = true)
    public Optional<ParamApp> findByName(String name) {
        LOGGER.info("PARAMAPP_SERVICE_GET_BY_NAME_ENTRY | name={}", name);
        return paramAppRepository.findByName(name);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long id) {
        LOGGER.info("PARAMAPP_SERVICE_DELETE_ENTRY | id={}", id);
        try {
            paramAppRepository.deleteById(id);
            LOGGER.info("PARAMAPP_SERVICE_DELETE_SUCCESS | id={}", id);
            return 0;
        } catch (Exception e) {
            LOGGER.warn("PARAMAPP_SERVICE_DELETE_DECISION | id={} decision=DELETE_FAILED reason={}", id, e.getMessage());
            throw new ResourceBadRequestException(e.getMessage());
        }
    }

    @Cacheable(value = "params_app", key = "'minAmountToDeposit'")
    @Transactional(readOnly = true)
    protected BigDecimal getMinAmountToDeposit() {
        LOGGER.info("PARAMAPP_SERVICE_GET_MIN_DEPOSIT_ENTRY");

        Optional<ParamApp> paramApp = paramAppRepository.findByName(ParamApp.LIMIT_MIN_TO_DEPOSIT);
        if (!paramApp.isPresent()) {
            LOGGER.warn("PARAMAPP_SERVICE_GET_MIN_DEPOSIT_DECISION | decision=NOT_FOUND");
            throw new ResourceBadRequestException("Parametro de configuracao nao encontrado: " + ParamApp.LIMIT_MIN_TO_DEPOSIT);
        }

        LOGGER.info("PARAMAPP_SERVICE_GET_MIN_DEPOSIT_SUCCESS | value={}", paramApp.get().getValueBigDecimal());
        return paramApp.get().getValueBigDecimal();
    }

    @Cacheable(value = "params_app", key = "'minAmountToTransfer'")
    @Transactional(readOnly = true)
    protected BigDecimal getMinAmountToTransfer() {
        LOGGER.info("PARAMAPP_SERVICE_GET_MIN_TRANSFER_ENTRY");

        Optional<ParamApp> paramApp = paramAppRepository.findByName(ParamApp.LIMIT_MIN_TO_TRANSFER);
        if (!paramApp.isPresent()) {
            LOGGER.warn("PARAMAPP_SERVICE_GET_MIN_TRANSFER_DECISION | decision=NOT_FOUND");
            throw new ResourceBadRequestException("Parametro de configuracao nao encontrado: " + ParamApp.LIMIT_MIN_TO_TRANSFER);
        }

        LOGGER.info("PARAMAPP_SERVICE_GET_MIN_TRANSFER_SUCCESS | value={}", paramApp.get().getValueBigDecimal());
        return paramApp.get().getValueBigDecimal();
    }
}
