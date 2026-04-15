package com.guga.walletserviceapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.guga.walletserviceapi.model.LoginAuth;

@Repository
public interface LoginAuthRepository extends JpaRepository<LoginAuth, Long> {

  @Query(value = """
      SELECT
        ROW_NUMBER() OVER (ORDER BY RANDOM()) as ORDER_ID,
        au.*
      FROM tb_login_auth au
      INNER JOIN tb_customer c ON c.customer_id = au.customer_id_fk
      WHERE au.role IS NOT NULL
        AND au.login_auth_type IN (1, 2, 3)
        AND ('USER' = ANY(au.role) OR 'ADMIN' = ANY(au.role))
        AND (
          EXISTS (SELECT 1 FROM tb_wallet w WHERE w.wallet_id = au.wallet_id_fk)
          OR
          EXISTS (SELECT 1 FROM tb_transaction t WHERE t.wallet_id = au.wallet_id_fk)
          )
          ORDER BY RANDOM()
          LIMIT 1
          """, nativeQuery = true)
  Optional<LoginAuth> findAnyLoginWithTransactions();
            
  Optional<LoginAuth> findByLogin(String login);

  @Query(value = "SELECT * FROM tb_login_auth ORDER BY RANDOM() LIMIT 1;", nativeQuery = true)
  LoginAuth findAnyLoginOrderRandom();
  
}