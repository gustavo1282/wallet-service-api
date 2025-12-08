package com.guga.walletserviceapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.ParamApp;

@Repository
public interface ParamAppRepository extends JpaRepository<ParamApp, Long> { 

    Optional<ParamApp> findByName(String name);

    /**
     * Retorna a primeira (Top) entidade ParamsApp ordenada pelo ID de forma decrescente.
     */
    Optional<ParamApp> findTopByOrderByIdDesc();

}