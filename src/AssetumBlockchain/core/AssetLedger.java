/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
 /*
 * Classe: AssetLedger
 * Pacote: AssetumBlockchain
 *
 * Função:
 *   Mantém o estado lógico (ledger) dos ativos da Assetum:
 *     - Mapa de proprietário atual por (tipo + assetId)
 *     - Mapa de metadados por (tipo + assetId)
 *
 *   Expõe duas famílias de operações:
 *     1) validate*  → valida transações sem alterar estado (dry-run)
 *     2) apply*     → aplica transações e altera o estado (commit)
 *
 *   Usa AssetTypePolicy para regras específicas por tipo (ex.: REAL_ESTATE).
 */
package AssetumBlockchain.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.security.PublicKey;
import java.util.TreeMap;
import java.util.Map;
import utils.SecurityUtils;

public final class AssetLedger {

    // Policies registadas por tipo (ex.: "REAL_ESTATE" → RealEstatePolicy)
    private final Map<String, AssetTypePolicy> policies = new HashMap<>();
    // Estado vivo (resultado de aplicar os blocos/txs em ordem)
    private final Map<String, PublicKey> liveOwner = new HashMap<>();                  // key -> dono atual
    private final Map<String, Map<String, String>> liveMeta = new HashMap<>();         // key -> metadados canónicos

    private static final String META_PASSWORD = "ASSETUM-META-2025";

    // Regista/atualiza a policy de um tipo de ativo
    public void registerPolicy(AssetTypePolicy policy) {
        Objects.requireNonNull(policy, "policy");
        policies.put(policy.getType(), policy);
    }

    // Constrói a chave interna: "TIPO#assetIdHex"
    private static String key(String type, byte[] assetId) {
        StringBuilder sb = new StringBuilder(type).append('#');
        for (byte b : assetId) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Obtém a policy para um tipo ou lança erro se não existir
    private AssetTypePolicy requirePolicy(String assetType) {
        AssetTypePolicy p = policies.get(assetType);
        if (p == null) {
            throw new IllegalStateException("Policy não registada para tipo: " + assetType);
        }
        return p;
    }

    // =========================================================================
    // Validações "dry-run" (não alteram estado) — usadas em submitTx/mempool
    // =========================================================================
    /**
     * Valida um REGISTER: - decodifica metadados (canónicos) - recomputa
     * assetId e compara - aplica regras da policy (campos
     * obrigatórios/autoridade) - verifica unicidade (ativo ainda não existe)
     *
     * @param tx
     */
    public synchronized void validateRegister(AssetTransaction tx) {
        if (tx.type != AssetTransaction.Type.REGISTER) {
            throw new IllegalArgumentException("Esperava REGISTER");
        }

        AssetTypePolicy policy = requirePolicy(tx.assetType);

        try {
            // 1) Desencriptar bytes dos metadados
            byte[] metaPlain = SecurityUtils.decrypt(tx.metadataEncoded, META_PASSWORD);

            // 2) Decodificar para Map<String,String>
            Map<String, String> meta = MetaCodec.decode(metaPlain);

            // 3) Recalcular o ID a partir dos metadados
            byte[] recomputed = policy.computeId(meta);
            if (!Arrays.equals(recomputed, tx.assetId)) {
                throw new SecurityException("assetId inválido: não corresponde aos metadados");
            }

            // 4) Regras específicas do tipo de ativo (campos obrigatórios, formatos, etc.)
            policy.validateRegister(meta, /*authority*/ null);

            // 5) Unicidade: ainda não pode existir
            String k = key(tx.assetType, tx.assetId);
            if (liveOwner.containsKey(k)) {
                throw new IllegalStateException("Ativo já existe no ledger: " + k);
            }

        } catch (Exception e) {
            // Pode falhar na desencriptação ou decode
            throw new SecurityException("Falha ao processar metadados cifrados da transação REGISTER", e);
        }
    }

    /**
     * Valida um TRANSFER: - verifica existência do ativo - confirma que
     * fromOwner é o dono atual - aplica regras da policy para transferência
     * (assinaturas/pseudo-assinaturas podem ser verificadas aqui)
     *
     * @param tx
     */
    public synchronized void validateTransfer(AssetTransaction tx) {
        if (tx.type != AssetTransaction.Type.TRANSFER) {
            throw new IllegalArgumentException("Esperava TRANSFER");
        }
        AssetTypePolicy policy = requirePolicy(tx.assetType);

        // (0) VERIFICAR ASSINATURA DIGITAL
    try {
        if (!tx.verifySignature(tx.fromOwner)) {
            throw new SecurityException("Assinatura digital inválida na TRANSFER");
        }
    } catch (Exception e) {
        throw new SecurityException("Erro ao verificar assinatura", e);
    }
    
        // (1) Verificar se o ativo existe
    String k = key(tx.assetType, tx.assetId);
    PublicKey currentOwner = liveOwner.get(k);
    if (currentOwner == null) {
        throw new IllegalStateException("Ativo inexistente: " + k);
    }
    
        // (2) Verificar que o fromOwner é realmente o dono atual
    if (!Arrays.equals(currentOwner.getEncoded(), tx.fromOwner.getEncoded())) {
        throw new SecurityException("fromOwner não é o dono atual do ativo");
    }

        Map<String, String> meta = liveMeta.get(k);
        policy.validateTransfer(meta, tx.fromOwner);

   
    }

    // =========================================================================
    // Aplicação (commit) — chamar após o bloco com a tx ser aceite/minerado
    // =========================================================================
    /**
     * Commit do REGISTER: cria entrada de proprietário e guarda metadados.
     *
     * @param tx
     */
    public synchronized void applyRegister(AssetTransaction tx) {
        if (tx.type != AssetTransaction.Type.REGISTER) {
            throw new IllegalArgumentException("Esperava REGISTER");
        }

        try {
            // 1) Desencriptar metadados que vêm na transação
            byte[] metaPlain = SecurityUtils.decrypt(tx.metadataEncoded, META_PASSWORD);

            // 2) Decodificar para Map<String,String>
            Map<String, String> meta = MetaCodec.decode(metaPlain);

            // 3) Guardar no estado do ledger
            String k = key(tx.assetType, tx.assetId);
            liveOwner.put(k, tx.toOwner);
            // TreeMap → ordem canónica/determinística das chaves
            liveMeta.put(k, new TreeMap<>(meta));

        } catch (Exception e) {
            throw new SecurityException("Falha ao aplicar REGISTER com metadados cifrados", e);
        }
    }

    /**
     * Commit do TRANSFER: atualiza o proprietário do ativo.
     *
     * @param tx
     */
    public synchronized void applyTransfer(AssetTransaction tx) {
        if (tx.type != AssetTransaction.Type.TRANSFER) {
            throw new IllegalArgumentException("Esperava TRANSFER");
        }
        String k = key(tx.assetType, tx.assetId);
        if (!liveOwner.containsKey(k)) {
            throw new IllegalStateException("Ativo inexistente no ledger: " + k);
        }
        liveOwner.put(k, tx.toOwner);
    }

    // =========================================================================
    // Consultas (leitura de estado atual)
    // =========================================================================
    /**
     * Devolve o dono atual de (type, assetId) ou null se não existir.
     *
     * @param type
     * @param assetId
     * @return
     */
    public synchronized PublicKey ownerOf(String type, byte[] assetId) {
        return liveOwner.get(key(type, assetId));
    }

    /**
     * Devolve os metadados canónicos de (type, assetId) ou null.
     *
     * @param type
     * @param assetId
     * @return
     */
    public synchronized Map<String, String> metadataOf(String type, byte[] assetId) {
        Map<String, String> m = liveMeta.get(key(type, assetId));
        return (m == null) ? null : new TreeMap<>(m);
    }
}
