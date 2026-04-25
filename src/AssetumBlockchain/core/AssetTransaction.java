/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AssetumBlockchain.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 *
 * @author macie
 */

/*
 * Classe: AssetTransaction
 * Pacote: AssetumBlockchain
 *
 * Função:
 *     Representa uma transação de ativo digital na blockchain Assetum.
 *     Cada transação pode ser de dois tipos:
 *          - REGISTER → criação de um ativo (ex.: um imóvel ou veículo)
 *          - TRANSFER → transferência de propriedade entre utilizadores
 *
 *     Esta classe define o formato de dados imutável que será guardado
 *     dentro dos blocos, validado pelo ledger e verificado pela política
 *     (AssetTypePolicy).
 *
 * Características principais:
 *     - Estrutura totalmente serializável (Serializable)
 *     - Campos públicos e finais (imutáveis após criação)
 *     - Compatível com sistemas de assinatura digital ou modo simples
 *     - Método auxiliar para gerar bytes canónicos (para hashing/assinatura)
 */
/**
 * Representa uma transação de ativo dentro da blockchain Assetum.
 */
public final class AssetTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    // Tipos de transação suportados
    public enum Type {
        REGISTER, // registo inicial de um novo ativo
        TRANSFER   // transferência de propriedade existente
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Campos principais da transação
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    public final Type type;            // tipo da transação
    public final String assetType;     // categoria do ativo ("REAL_ESTATE", etc.)
    public final byte[] assetId;       // identificador único do ativo (hash)
    public final PublicKey fromOwner;  // proprietário anterior (null em REGISTER)
    public final PublicKey toOwner;    // novo proprietário
    public final long timestampMillis; // instante da criação (epoch millis)

    // Apenas no REGISTER — contém os metadados codificados (ex.: artigo, freguesia, ano)
    public final byte[] metadataEncoded; // formato binário canónico; null no TRANSFER

    // Campos opcionais de assinatura (usados em modo de autenticação ECDSA)
    public byte[] authoritySignature;  // assinatura da autoridade (REGISTER)
    public byte[] ownerSignature;      // assinatura do proprietário (TRANSFER)

    private byte[] signature;

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Métodos de criação estáticos (Factory methods)
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Cria uma transação do tipo REGISTER (registo inicial de ativo)
     *
     * @param assetType tipo de ativo (ex.: "REAL_ESTATE")
     * @param assetId identificador único calculado pelo tipo
     * @param toOwner chave pública do novo proprietário
     * @param ts timestamp em milissegundos
     * @param metadataEncoded metadados canónicos do ativo
     * @return objeto AssetTransaction pronto a incluir no bloco
     */
    public static AssetTransaction register(String assetType, byte[] assetId,
            PublicKey toOwner, long ts, byte[] metadataEncoded) {

        return new AssetTransaction(
                Type.REGISTER, assetType, assetId,
                null, toOwner, ts, metadataEncoded
        );
    }

    /**
     * Cria uma transação do tipo TRANSFER (mudança de proprietário)
     *
     * @param assetType tipo de ativo
     * @param assetId identificador único
     * @param fromOwner proprietário atual
     * @param toOwner novo proprietário
     * @param ts timestamp
     * @return nova transação pronta a ser validada/minerada
     */
    public static AssetTransaction transfer(String assetType, byte[] assetId,
            PublicKey fromOwner, PublicKey toOwner, long ts) {

        return new AssetTransaction(
                Type.TRANSFER, assetType, assetId,
                fromOwner, toOwner, ts, null
        );
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Construtor privado (usado pelos factory methods)
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    private AssetTransaction(Type type, String assetType, byte[] assetId,
            PublicKey fromOwner, PublicKey toOwner,
            long ts, byte[] metadataEncoded) {

        this.type = type;
        this.assetType = assetType;
        this.assetId = assetId;
        this.fromOwner = fromOwner;
        this.toOwner = toOwner;
        this.timestampMillis = ts;
        this.metadataEncoded = metadataEncoded;
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Métodos auxiliares de assinatura e serialização
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Gera os bytes que devem ser assinados pela autoridade (REGISTER). Para
     * já, podes deixar como placeholder ou reutilizar o mesmo formato do owner
     * se vieres a ter uma "autoridade" que assina os registos.
     */
    public byte[] signingBytesForAuthority() {
        // Exemplo minimo: tipo, assetType, assetId, toOwner, timestamp
        try (var out = new java.io.ByteArrayOutputStream()) {
            out.write(0x01);                            // versão
            out.write((byte) type.ordinal());           // tipo
            writeString(out, assetType);
            out.write(assetId);
            writeBytes(out, toOwner == null ? null : toOwner.getEncoded());
            out.write(java.nio.ByteBuffer.allocate(8).putLong(timestampMillis).array());
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gera bytes canónicos representando a transação, usados para hashing ou
     * assinatura pelo proprietário (TRANSFER e, se quiseres, também REGISTER).
     *
     * @return vetor de bytes representando os campos relevantes
     */
    public byte[] signingBytesForOwner() {
        try (var out = new java.io.ByteArrayOutputStream()) {
            out.write(0x01);                            // versão do formato
            out.write((byte) type.ordinal());           // tipo (0=REGISTER, 1=TRANSFER)
            writeString(out, assetType);                // tipo de ativo
            out.write(assetId);                         // bytes do assetId

            // fromOwner e toOwner em formato X.509
            writeBytes(out, fromOwner == null ? null : fromOwner.getEncoded());
            writeBytes(out, toOwner == null ? null : toOwner.getEncoded());

            // timestamp consistente (usa sempre o mesmo campo da classe!)
            out.write(java.nio.ByteBuffer.allocate(8).putLong(timestampMillis).array());

            // OPCIONAL: se quiseres autenticar também os metadados cifrados
            if (metadataEncoded != null) {
                writeBytes(out, metadataEncoded);
            } else {
                writeBytes(out, null);
            }

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public byte[] getSignature() {
        return signature;
    }

// ---------- ASSINAR TRANSAÇÃO ----------
    public void sign(PrivateKey priv) throws Exception {
        // Suporta RSA e EC, para alinhar com o modelo de autenticação do nó
        // (no projeto de referência é comum usar EC).
        String alg = (priv != null && "EC".equalsIgnoreCase(priv.getAlgorithm()))
                ? "SHA256withECDSA"
                : "SHA256withRSA";
        Signature sig = Signature.getInstance(alg);
        sig.initSign(priv);
        sig.update(signingBytesForOwner());
        this.signature = sig.sign();
    }

    // ---------- VERIFICAR ASSINATURA ----------
    public boolean verifySignature(PublicKey pub) throws Exception {
        if (pub == null || signature == null) {
            return false;
        }
        String alg = ("EC".equalsIgnoreCase(pub.getAlgorithm()))
                ? "SHA256withECDSA"
                : "SHA256withRSA";
        Signature sig = Signature.getInstance(alg);
        sig.initVerify(pub);
        sig.update(signingBytesForOwner());
        return sig.verify(signature);
    }

    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    // :: Funções utilitárias privadas para escrita binária
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Escreve uma String no fluxo em formato [2 bytes comprimento][dados UTF-8]
     */
    private static void writeString(java.io.OutputStream out, String s) throws java.io.IOException {
        byte[] b = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        out.write((b.length >>> 8) & 0xFF);
        out.write(b.length & 0xFF);
        out.write(b);
    }

    /**
     * Escreve um vetor de bytes no formato [2 bytes comprimento][dados]. Se o
     * vetor for nulo, escreve 0.
     */
    private static void writeBytes(java.io.OutputStream out, byte[] b) throws java.io.IOException {
        if (b == null) {
            out.write(0);
            out.write(0);
            return;
        }
        out.write((b.length >>> 8) & 0xFF);
        out.write(b.length & 0xFF);
        out.write(b);
    }
}
