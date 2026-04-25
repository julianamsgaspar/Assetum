/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AssetumBlockchain.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;

/**
 *
 * @author macie
 */
/*
 * Classe: RealEstatePolicy
 * Pacote: AssetumBlockchain
 *
 * Função:
 *   Implementa a AssetTypePolicy para o tipo de ativo "REAL_ESTATE".
 *   - Define campos obrigatórios do metadata
 *   - Calcula o assetId de forma determinística (SHA-256 sobre payload canónico)
 *   - Valida REGISTER (sanidade + autoridade opcional)
 *   - Valida TRANSFER (podes acrescentar regras extra, se precisares)
 */

public final class RealEstatePolicy implements AssetTypePolicy {

    // Campos obrigatórios no metadata (chaves canónicas, sem acentos)
    private static final List<String> REQUIRED = List.of(
            "artigo", // artigo matricial
            "freguesia",
            "conservatoria",
            "ano",
            "_salt" // texto aleatório (ex.: Base64) para unicidade
    );

    // (Opcional) Autoridades permitidas a emitir REGISTER deste tipo
    private final Set<PublicKey> authorities = new HashSet<>();

    public RealEstatePolicy() {
    }

    public RealEstatePolicy(Collection<PublicKey> allowed) {
        if (allowed != null) {
            authorities.addAll(allowed);
        }
    }

    @Override
    public String getType() {
        return "REAL_ESTATE";
    }

    /**
     * Calcula o assetId a partir de um payload canónico:
     * artigo=...|freguesia=...|conservatoria=...|ano=...|salt=... Garante
     * determinismo usando TreeMap (ordem por chave).
     */
    @Override
    public byte[] computeId(Map<String, String> meta) {
        // Normaliza/ordena para canonicidade
        Map<String, String> m = new TreeMap<>(meta);

        // Verifica presença dos obrigatórios
        for (String k : REQUIRED) {
            if (!m.containsKey(k) || m.get(k) == null || m.get(k).isBlank()) {
                throw new IllegalArgumentException("Metadata ausente/ inválido: " + k);
            }
        }

        // Payload canónico (strings simples e estáveis)
        String payload
                = "artigo=" + m.get("artigo") + "|"
                + "freguesia=" + m.get("freguesia") + "|"
                + "conservatoria=" + m.get("conservatoria") + "|"
                + "ano=" + m.get("ano") + "|"
                + "salt=" + m.get("_salt");

        return sha256(payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Valida regras do REGISTER: - formatos mínimos - (opcional) autoridade
     * autorizada
     */
    @Override
    public void validateRegister(Map<String, String> meta, PublicKey authority) {

        // ::: 1) ARTIGO
        require(meta, "artigo", v -> v.matches("^[0-9]{1,6}(/[A-Z])?$"),
                "O número de artigo deve ser numérico (1 a 6 dígitos), opcionalmente seguido de /A, /B, etc.");

        // ::: 2) FREGUESIA
        require(meta, "freguesia", v -> v.matches("^[A-Za-zÀ-ÿ\\s\\-]{3,40}$"),
                "O nome da freguesia deve ter 3 a 40 letras (acentos permitidos).");

        // ::: 3) CONSERVATÓRIA
        require(meta, "conservatoria", v -> v.matches("^[A-Za-zÀ-ÿ\\s\\-]{3,40}$"),
                "A conservatória deve conter apenas letras e hífens (ex: 'Tomar', 'Lisboa-1').");

        // ::: 4) ANO
        require(meta, "ano", v -> v.matches("^(19[0-9]{2}|20[0-9]{2})$"),
                "O ano deve estar entre 1900 e o ano atual (formato YYYY).");

        // ::: 5) SALT (interno, mas válido)
        require(meta, "_salt", v -> v.length() >= 16,
                "Erro interno: salt inválido. Contacte suporte.");

        // ::: 6) AUTORIDADE (caso uses este mecanismo)
        if (!authorities.isEmpty()
                && (authority == null || !authorities.contains(authority))) {

            throw new SecurityException(
                    "A autoridade fornecida não está autorizada a registar imóveis."
            );
        }
    }

    /**
     * Validação de TRANSFER (podes acrescentar regras específicas, como
     * hipoteca/penhora). Para o MVP não impomos restrições extra.
     */
    @Override
    public void validateTransfer(Map<String, String> meta, PublicKey fromOwner) {
        // Sem regras adicionais no MVP.
    }

    @Override
    public Set<PublicKey> allowedAuthorities() {
        return Collections.unmodifiableSet(authorities);
    }

    // ----------------- Helpers -----------------
    private void require(Map<String, String> meta,
            String key,
            java.util.function.Predicate<String> predicate,
            String userMessage) {
        String value = meta.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório em falta: " + key);
        }

        if (!predicate.test(value)) {
            throw new IllegalArgumentException(userMessage);
        }
    }

    /**
     * SHA-256 utilitário (lança RuntimeException se o algoritmo falhar).
     */
    private static byte[] sha256(byte[] in) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return d.digest(in);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
