/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package AssetumBlockchain.core;

import java.security.PublicKey;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author macie
 */
/*
 * Interface: AssetTypePolicy
 * Pacote: AssetumBlockchain
 *
 * Função:
 *     Define a política de identificação, validação e autorização
 *     para cada tipo de ativo registado na blockchain Assetum.
 *
 *     Cada tipo de ativo (ex.: "REAL_ESTATE", "VEHICLE", "DIPLOMA")
 *     pode ter regras próprias de:
 *        - Cálculo de ID único (assetId)
 *        - Validação de registo (REGISTER)
 *        - Validação de transferência (TRANSFER)
 *        - Autoridades permitidas a emitir novos registos
 *
 * Contexto:
 *     Esta interface permite que o sistema seja extensível — novos
 *     tipos de bens podem ser adicionados simplesmente criando
 *     classes que implementem esta interface (ex.: RealEstatePolicy,
 *     VehiclePolicy, DiplomaPolicy, etc.).
 */

public interface AssetTypePolicy {

    /**
     * Identifica o tipo de ativo que esta política representa.
     * Exemplo: "REAL_ESTATE", "VEHICLE", "DIPLOMA", etc.
     *
     * @return String com o identificador lógico do tipo de ativo
     */
    String getType(); 

    /**
     * Calcula o identificador único (assetId) do ativo com base nos
     * seus metadados. O resultado é normalmente um hash (SHA-256)
     * gerado a partir dos valores canónicos de 'meta'.
     *
     * Este método garante que dois ativos com os mesmos metadados
     * produzem exatamente o mesmo assetId, assegurando unicidade.
     *
     * @param meta mapa de metadados do ativo (ex.: artigo, freguesia, etc.)
     * @return vetor de bytes representando o ID único do ativo
     */
    byte[] computeId(Map<String,String> meta);

    /**
     * Valida o registo inicial (REGISTER) de um ativo deste tipo.
     * Pode verificar:
     *   - se os metadados obrigatórios estão presentes;
     *   - se os valores são válidos;
     *   - se a autoridade emissora é permitida.
     *
     * @param meta mapa de metadados do ativo
     * @param authority chave pública da autoridade que emite o registo
     * @throws SecurityException ou IllegalArgumentException se inválido
     */
    void validateRegister(Map<String,String> meta, PublicKey authority);

    /**
     * Valida a transferência de propriedade (TRANSFER) de um ativo
     * deste tipo. Pode verificar:
     *   - se o proprietário atual é quem autoriza a transferência;
     *   - se há restrições específicas (por exemplo, imóveis com hipoteca);
     *   - se os metadados permitem transferência (ex.: ativo não bloqueado).
     *
     * @param meta metadados do ativo em causa
     * @param fromOwner chave pública do proprietário atual
     * @throws SecurityException se a transferência não for válida
     */
    void validateTransfer(Map<String,String> meta, PublicKey fromOwner);

    /**
     * Define o conjunto de autoridades autorizadas a criar ativos
     * deste tipo. Exemplo:
     *    - Para "REAL_ESTATE": conservatórias ou cartórios públicos;
     *    - Para "DIPLOMA": universidades certificadas;
     *    - Para "VEHICLE": IMT ou fabricantes.
     *
     * @return conjunto de chaves públicas permitidas; pode estar vazio
     *         se qualquer utilizador puder criar o ativo (modo aberto)
     */
    Set<PublicKey> allowedAuthorities();
}

