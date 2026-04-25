package AssetumBlockchain.core;

import java.util.Objects;
import java.util.*;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author macie
 */

/*
 * Classe: Asset
 * Pacote: AssetumBlockchain
 *
 * Função:
 *     Representa um ativo digital único (ex.: imóvel, veículo, diploma)
 *     na blockchain Assetum. Cada ativo possui:
 *        - Um tipo (definido pela política)
 *        - Um conjunto de metadados descritivos
 *        - Um identificador único (assetId) derivado dos metadados
 *
 *     O objeto Asset é imutável (immutable) e determinístico:
 *       - Os metadados são armazenados num TreeMap ordenado
 *       - O assetId é calculado pela política (AssetTypePolicy)
 *
 * Contexto:
 *     Serve como estrutura base para criar transações REGISTER
 *     e para aplicar políticas de validação no ledger.
 */

/**
 * Classe que representa um ativo registável na blockchain.
 * Cada Asset é único e definido pelos seus metadados e política associada.
 */
public final class Asset {

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Campos principais
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private final String type;                 // Tipo do ativo (ex.: "REAL_ESTATE", "VEHICLE", etc.)
    private final Map<String,String> metadata; // Metadados canónicos (ex.: artigo, freguesia, ano, etc.)
    private final byte[] assetId;              // Identificador único (hash derivado da policy)

    // Construtor privado → apenas criado via método estático create()
    private Asset(String type, Map<String,String> metadata, byte[] assetId) {
        this.type = Objects.requireNonNull(type);
        // Metadados imutáveis e ordenados para consistência entre nós
        this.metadata = Collections.unmodifiableMap(new TreeMap<>(metadata));
        this.assetId = Objects.requireNonNull(assetId);
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Criação de ativos
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Cria um novo ativo genérico a partir de uma policy e metadados.
     * - A policy define como o assetId será calculado.
     * - Se a policy requerer um “salt” para unicidade, ele é automaticamente adicionado.
     *
     * @param policy implementação de AssetTypePolicy (ex.: RealEstatePolicy)
     * @param meta mapa de metadados (ex.: artigo, freguesia, ano)
     * @return novo objeto Asset imutável e identificado
     */
    public static Asset create(AssetTypePolicy policy, Map<String,String> meta) {
        Objects.requireNonNull(policy);
        Objects.requireNonNull(meta);

        // Usa TreeMap para garantir ordem lexicográfica (determinismo)
        Map<String,String> m = new TreeMap<>(meta);

        // Opcional: adiciona salt se não existir (para garantir unicidade)
        m.putIfAbsent("_salt", Base64.getEncoder().encodeToString(randomBytes(16)));

        // Calcula o ID do ativo usando a política correspondente
        byte[] id = policy.computeId(m);

        // Cria instância imutável
        return new Asset(policy.getType(), m, id);
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Getters públicos
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /** Retorna o tipo do ativo (definido pela policy)
     * @return  */
    public String getType() { return type; }

    /** Retorna os metadados (mapa imutável)
     * @return  */
    public Map<String,String> getMetadata() { return metadata; }

    /** Retorna o identificador único em bytes
     * @return  */
    public byte[] getAssetId() { return assetId; }

    /** Retorna o identificador único em formato hexadecimal legível
     * @return  */
    public String assetIdHex() { return hex(assetId); }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Métodos auxiliares privados
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /** Gera bytes aleatórios (usado para o campo opcional "_salt") */
    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        new java.security.SecureRandom().nextBytes(b);
        return b;
    }

    /** Converte um vetor de bytes num texto hexadecimal */
    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Representação textual (útil para logs e debug)
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    @Override
    public String toString() {
        return "Asset{type=" + type +
               ", assetId=" + assetIdHex() +
               ", metadata=" + metadata + "}";
    }
}
