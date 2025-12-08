package com.guga.walletserviceapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.LoginAuth;

@Repository
public interface LoginAuthRepository extends JpaRepository<LoginAuth, Long> {

    Optional<LoginAuth> findByLogin(String login);

}