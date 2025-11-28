package com.guga.walletserviceapi.seeder;

public record SeedDefinition (
    String fileName,
    Class<?> entityClass
)  {}

