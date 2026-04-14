package com.guga.walletserviceapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class ResourceUserNotAuthenticatedException  extends AuthenticationException  {

    public ResourceUserNotAuthenticatedException() {
        super("Acesso negado: Usuário não encontrado no contexto JWT.");
    }

    public ResourceUserNotAuthenticatedException(String msg) {
        super(msg);
    }

    public ResourceUserNotAuthenticatedException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
