/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package blockchain.utils;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import miner.Miner;

/**
 * Classe Block representa um bloco na blockchain
 * Cada bloco contém uma referência ao hash do bloco anterior, uma Merkle Root 
 * das transações associadas, um nonce (usado na prova de trabalho) e o hash do bloco atual
 * Esta implementação é serializável, para permitir o armazenamento e transmissão
 * Também implementa a interface Comparable para comparação entre blocos com base no hash
 * 
 * @author Catarina - Miguel
 */
public class Block implements Serializable, Comparable<Block> {

    String previousHash; // Hash do bloco anterior
    String merkleRoot;   // Raiz da Merkle Tree gerada a partir das transações deste bloco
    List<String> transactions; // transações do bloco (devem ser guardadas em separado)
    int nonce;           // Número usado na prova de trabalho para encontrar um hash válido
    String currentHash;  // Hash do bloco atual    
    public MerkleTree mk; // Objeto MerkleTree para calcular a raiz a partir das transações

    /**
     * Construtor para inicializar um bloco
     * @param previousHash O hash do bloco anterior na cadeia
     * @param transactions Lista de transações incluídas neste bloco
     */
    public Block(String previousHash, List<String> transactions) {
        this.previousHash = previousHash; // Define o hash do bloco anterior
        this.transactions = transactions; // Define as transações associadas
        mk = new MerkleTree(transactions); // Cria a Merkle Tree com as transações
        this.merkleRoot = mk.getRoot(); // Obtém a raiz da Merkle Tree
    }

    /**
     * Define o nonce e valida o hash gerado
     * @param nonce Valor do nonce a ser usado
     * @param zeros Número de zeros à esquerda necessário para um hash válido
     * @throws Exception Se o hash gerado não começar com o número esperado de zeros
     */
    public void setNonce(int nonce, int zeros) throws Exception {
        this.nonce = nonce;
        this.currentHash = calculateHash(); // Calcula o hash
        String prefix = String.format("%0" + zeros + "d", 0); // Cria o prefixo necessário
        if (!currentHash.startsWith(prefix)) {
            throw new Exception(nonce + " not valid Hash=" + currentHash);
        }
        
    }

    /**
     * Obtém os dados relevantes para o minerador (hash anterior e Merkle Root)
     * @return String com os dados para o cálculo do hash
     */
    public String getMinerData() {
        return previousHash + merkleRoot;
    }

    /**
     * Obtém a Merkle Root do bloco
     * @return String que contém a raiz da Merkle Tree
     */
    public String getMerkleRoot() {
        return merkleRoot;
    }

    /**
     * Obtém a lista de transações associadas ao bloco
     * @return Lista de transações
     */
    public List<String> transactions() {
        return transactions;
    }

    /**
     * Obtém o hash do bloco anterior
     * @return String com o hash do bloco anterior
     */
    public String getPreviousHash() {
        return previousHash;
    }

    /**
     * Obtém o valor do nonce usado no bloco
     * @return Valor inteiro do nonce
     */
    public int getNonce() {
        return nonce;
    }

    
     /**
     * Calcula o hash atual do bloco com base nos seus dados
     * @return String que contém o hash do bloco
     */
    public String calculateHash() {
        return Miner.getHash(getMinerData(), nonce);
    }

    /**
     * Obtém o hash atual do bloco
     * @return String que contém o hash atual do bloco
     */
    public String getCurrentHash() {
        return currentHash;
    }

    /**
     * Representação textual resumida do bloco
     * @return String formatada contendo os dados principais do bloco
     */
    @Override
    public String toString() {
        return 
                String.format("[ %8s", previousHash) + " <- "
                + String.format("%-10s", merkleRoot) + String.format(" %7d ] = ", nonce)
                + String.format("%8s", currentHash);
    }

     /**
     * Obtém o cabeçalho do bloco (hashes, nonce, etc.) como texto
     * @return String que contém o cabeçalho do bloco
     */
    public String getHeaderString() {
        return    "prev Hash: " + previousHash +
                "\nMkt Root : " + merkleRoot+
                "\nnonce    : " + nonce+
                "\ncurr Hash: " + currentHash;
    }
    
    
    /**
     * Obtém as transações do bloco como uma string formatada
     * @return String com as transações separadas por linhas
     */
    public String getTransactionsString() {
        StringBuilder txt = new StringBuilder();
        for (String transaction : transactions) {
            txt.append(transaction+"\n");
        }
        return txt.toString();
    }

     /**
     * Valida o bloco verificando se o hash atual corresponde ao hash calculado
     * @return true se o bloco for válido, false caso contrário
     */
    public boolean isValid() {
        return currentHash.equals(calculateHash());
    }

     /**
     * Gera o hash code do bloco (Implementação básica para uso em coleções)
     * @return Valor inteiro do hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }

    /**
     * Compara dois blocos para verificar igualdade
     * @param obj O outro bloco a ser comparado
     * @return true se os blocos forem iguais, false caso contrário
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Block other = (Block) obj;
        if (this.nonce != other.nonce) {
            return false;
        }
        if (!Objects.equals(this.previousHash, other.previousHash)) {
            return false;
        }
        if (!Objects.equals(this.merkleRoot, other.merkleRoot)) {
            return false;
        }
        return Objects.equals(this.currentHash, other.currentHash);
    }

    
    /**
     * Compara dois blocos com base no hash atual
     * @param o O outro bloco a ser comparado
     * @return Resultado da comparação lexicográfica dos hashes
     */
    @Override
    public int compareTo(Block o) {
        return this.currentHash.compareTo(o.currentHash);
    }

}
