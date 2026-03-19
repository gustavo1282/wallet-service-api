package com.guga.walletserviceapi.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Routers {
    
    public static String CONTEXT_PATH = "wallet-service-api";
    public static String PREFIX = "/api/v1";
    
    public static String AUTH = "/auth";
    public static String CUSTOMERS = "/customers";
    public static String WALLETS = "/wallets";
    public static String TRANSACTIONS = "/transactions";
    public static String WALLET_OPERATOR = "/wallet-operator";

}
