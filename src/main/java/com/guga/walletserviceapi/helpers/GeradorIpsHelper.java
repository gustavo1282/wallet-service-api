package com.guga.walletserviceapi.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GeradorIpsHelper {
    


    // Mapa Simples: Estado -> Lista de Cidades Principais
    private static final Map<String, List<String>> MAPA_ESTADOS = Map.of(
        "SP", List.of("São Paulo", "Osasco", "Campinas", "Santos", "São Bernardo do Campo"),
        "RJ", List.of("Rio de Janeiro", "Niterói", "Búzios", "Petrópolis"),
        "MG", List.of("Belo Horizonte", "Uberlândia", "Ouro Preto", "Contagem"),
        "RS", List.of("Porto Alegre", "Gramado", "Caxias do Sul", "Canoas"),
        "PR", List.of("Curitiba", "Londrina", "Maringá", "Foz do Iguaçu")
    );

    // Faixas de IP que o Registro.br costuma designar (Brasil)
    private static final int[] FAIXAS_IP_BR = {177, 179, 186, 187, 189, 200, 201};

    public static IpHelper gerarIpAleatorio() {
        //Faker faker = new Faker(new Locale("pt-BR"));
        Random random = new Random();

        // 1. Sortear uma UF (Estado) do nosso mapa controlado
        List<String> estados = new ArrayList<>(MAPA_ESTADOS.keySet());
        String ufSorteada = estados.get(random.nextInt(estados.size()));

        // 2. Sortear uma cidade que REALMENTE pertence àquela UF
        List<String> cidadesDaUf = MAPA_ESTADOS.get(ufSorteada);
        String cidadeSorteada = cidadesDaUf.get(random.nextInt(cidadesDaUf.size()));

        // 3. Gerar o IP Brasileiro
        String ipBr = FAIXAS_IP_BR[random.nextInt(FAIXAS_IP_BR.length)] + "." + 
                      random.nextInt(256) + "." + 
                      random.nextInt(256) + "." + 
                      random.nextInt(1, 255);

        // Resultado Consistente
        //System.out.println("IP: " + ipBr);
        //System.out.println("Local: " + cidadeSorteada + " / " + ufSorteada);

        return new IpHelper(ipBr, cidadeSorteada, ufSorteada);
    }


    public record IpHelper(
        String ip,
        String cidade,
        //String estado,
        String sigla
    )
    {}

}


