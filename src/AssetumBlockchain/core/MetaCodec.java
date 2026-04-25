/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AssetumBlockchain.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/**
 *
 * @author macie
 */
/*
 * Classe: MetaCodec
 * Pacote: AssetumBlockchain
 *
 * Função:
 *     Esta classe implementa um formato binário simples e determinístico
 *     para codificar e descodificar mapas de metadados (Map<String,String>),
 *     garantindo que a ordem das chaves é sempre igual — essencial para
 *     calcular o mesmo hash em todos os nós da blockchain.
 *
 * Características:
 *     - Ordem canónica (TreeMap -> ordenação por chave)
 *     - Codificação binária compacta e independente de plataforma
 *     - Utiliza UTF-8 para todos os textos
 */


public final class MetaCodec {

    /**
     * Codifica um mapa de pares (chave, valor) num vetor de bytes de forma
     * determinística. A ordenação canónica (por chave) garante que a mesma
     * estrutura produz exatamente o mesmo resultado binário em qualquer nó.
     *
     * @param meta Mapa com metadados (ex.: artigo, freguesia, ano, etc.)
     * @return vetor de bytes representando o mapa
     */
    public static byte[] encode(Map<String, String> meta) {
        try {
            // buffer para guardar o resultado binário
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // percorre as entradas do mapa em ordem alfabética das chaves
            for (var e : new TreeMap<>(meta).entrySet()) {
                // escreve primeiro a chave, depois o valor
                writeString(out, e.getKey());
                writeString(out, e.getValue());
            }

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Descodifica um vetor de bytes gerado por encode() e reconstrói o mapa.
     * O formato lido é: [lenChave][bytesChave][lenValor][bytesValor] repetido.
     *
     * @param bytes vetor de bytes com o conteúdo codificado
     * @return mapa reconstruído (TreeMap, ordenado)
     */
    public static Map<String, String> decode(byte[] bytes) {
        try {
            Map<String, String> m = new TreeMap<>();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);

            // enquanto houver dados, lê pares chave/valor
            while (in.available() > 0) {
                String k = readString(in);  // lê chave
                String v = readString(in);  // lê valor
                m.put(k, v);
            }
            return m;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Escreve uma String no fluxo de saída no formato:
     * [2 bytes de comprimento][bytes da string UTF-8]
     */
    private static void writeString(OutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);

        // escreve o comprimento em dois bytes (big-endian)
        out.write((b.length >>> 8) & 0xFF); // parte alta
        out.write(b.length & 0xFF);         // parte baixa

        // escreve o conteúdo da string
        out.write(b);
    }

    /**
     * Lê uma String do fluxo de entrada no formato binário:
     * [2 bytes de comprimento][bytes da string UTF-8]
     */
    private static String readString(InputStream in) throws IOException {
        // lê dois bytes que indicam o comprimento
        int hi = in.read();
        int lo = in.read();
        if (hi < 0 || lo < 0) throw new EOFException();

        int len = (hi << 8) | lo;  // reconstruir valor de 16 bits
        byte[] b = in.readNBytes(len);

        if (b.length != len) throw new EOFException();

        // converte os bytes lidos em String UTF-8
        return new String(b, StandardCharsets.UTF_8);
    }
}
