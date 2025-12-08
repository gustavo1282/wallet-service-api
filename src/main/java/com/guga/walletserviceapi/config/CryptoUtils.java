package com.guga.walletserviceapi.config;

//@Component
public class CryptoUtils {
//
//    // --- 1. Hashing para Senhas de Login (BCrypt) ---
//    // Utilize o Bean do Spring Security (PasswordEncoderFactories) ou este aqui:
//    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//
//    /**
//     * Hashing para Senhas de Login (BCrypt)
//     * @param rawPassword
//     * @return
//     */
//    public String hashPassword(String rawPassword) {
//        return passwordEncoder.encode(rawPassword);
//    }
//
//    /***
//     * Verifica se a senha bruta corresponde à senha codificada
//     * @param rawPassword
//     * @param encodedPassword
//     * @return
//     */
//    public boolean checkPassword(String rawPassword, String encodedPassword) {
//        return passwordEncoder.matches(rawPassword, encodedPassword);
//    }
//
//    // --- 2. Criptografia Simétrica para Dados Sensíveis (AES-256) ---
//    // Requer uma chave secreta de 256 bits (32 caracteres)
//    //private static final String AES_SECRET_KEY = "sua_chave_secreta_aes_de_32_bytes"; // Obtenha isso do Vault/variável de ambiente!
//
//    @Value("${jwt.secret}")
//    private String AES_SECRET_KEY;
//
//    /***
//     * Criptografa os dados Sensíveis (RG, CPF, Email) usando AES-256
//     * @param data
//     * @return
//     * @throws Exception
//     */
//    public String encryptData(String data) throws Exception {
//        SecretKey key = new SecretKeySpec(AES_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
//        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//        cipher.init(Cipher.ENCRYPT_MODE, key);
//        byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
//        return Base64.getEncoder().encodeToString(encryptedBytes);
//    }
//
//    /***
//     * Descriptografa os dados Sensíveis (RG, CPF, Email) usando AES-256
//     * @param encryptedData
//     * @return
//     * @throws Exception
//     */
//    public String decryptData(String encryptedData) throws Exception {
//        SecretKey key = new SecretKeySpec(AES_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
//        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
//        cipher.init(Cipher.DECRYPT_MODE, key);
//        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
//        return new String(decryptedBytes, StandardCharsets.UTF_8);
//    }
//
//    
//    /***
//     * Use Base64 sempre que precisar tratar dados não textuais (binários) como texto simples para fins de transmissão ou armazenamento em sistemas que não suportam binários nativamente.
//     * Codificação Base64 (Não é Criptografia)
//     * Próprio JWT - Header (Cabeçalho) | Payload (Corpo) utilizam Base64 | 
//     *               Embutindo Imagens | Transformação APIs REST
//     * @param data
//     * @return
//     */
//    public String encodeToBase64(String data) {
//        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
//    }
//
//    /***
//     * Decodificação Base64 (Não é Criptografia)
//     * @param base64Data
//     * @return
//     */
//    public String decodeFromBase64(String base64Data) {
//        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
//        return new String(decodedBytes, StandardCharsets.UTF_8);
//    }

}