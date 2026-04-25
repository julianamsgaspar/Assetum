/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package blockchain.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Representa uma blockchain composta por uma lista de blocos, implementando
 * funcionalidades para adicionar, carregar, salvar e validar blocos.
 *
 * @author Catarina - Miguel
 */
public class BlockChain implements Serializable {

    // Lista de blocos na cadeia, utilizando uma estrutura segura para threads
    CopyOnWriteArrayList<Block> chain;

    /**
     * Construtor que inicializa uma blockchain vazia
     */
    public BlockChain() {
        chain = new CopyOnWriteArrayList<>();
    }

    /**
     * Construtor que tenta carregar a blockchain de um ficheiro.
     *
     * @param fileName Nome do ficheiro a ser carregado
     */
    public BlockChain(String fileName) {
        try {
            // Tenta carregar a blockchain a partir do ficheiro especificado
            load(fileName);
        } catch (Exception e) {
            // Caso ocorra uma falha, inicializa uma blockchain vazia
            chain = new CopyOnWriteArrayList<>();
        }
    }

    /**
     * Obtém o hash do último bloco da cadeia.
     *
     * @return Hash do último bloco ou um valor padrão caso a cadeia esteja
     * vazia.
     */
    public String getLastBlockHash() {
        // Caso a cadeia esteja vazia (Genesis block)
        if (chain.isEmpty()) {
            return String.format("%08d", 0);
        }
        // Retorna o hash do último bloco
        return chain.get(chain.size() - 1).currentHash;
    }

    /**
     * Obtém o último bloco da cadeia.
     *
     * @return O último bloco ou null se a cadeia estiver vazia.
     */
    public Block getLastBlock() {
        // Caso a cadeia esteja vazia (Genesis block)
        if (chain.isEmpty()) {
            return null;
        }
        return chain.get(chain.size() - 1);
    }

    /**
     * Adiciona um novo bloco à blockchain, verificando a sua validade e
     * integridade.
     *
     * @param newBlock Bloco a ser adicionado
     * @throws Exception Caso o bloco seja duplicado, inválido ou o hash
     * anterior não corresponda.
     */
    public void add(Block newBlock) throws Exception {
        if (chain.contains(newBlock)) {
            throw new Exception("Duplicated Block");
        }
        //verify block
        if (!newBlock.isValid()) {
            throw new Exception("Invalid Block");
        }
        //verify link
        if (getLastBlockHash().compareTo(newBlock.previousHash) != 0) {
            throw new Exception("Previous hash not combine");
        }
        // Adiciona o bloco à cadeia
        chain.add(newBlock);
    }

    /**
     * Obtém um bloco da cadeia pelo índice.
     *
     * @param index Índice do bloco a ser obtido
     * @return O bloco correspondente ao índice
     */
    public Block get(int index) {
        return chain.get(index);
    }

    /**
     * Obtém o tamanho atual da blockchain.
     *
     * @return Número de blocos na cadeia
     */
    public int getSize() {
        return chain.size();
    }

    /**
     * Representação em texto da blockchain, incluindo o tamanho e a descrição
     * de cada bloco.
     *
     * @return String contendo os detalhes da cadeia.
     */
    public String toString() {
        StringBuilder txt = new StringBuilder();
        txt.append("Blochain size = " + chain.size() + "\n");
        for (Block block : chain) {
            txt.append(block.toString() + "\n");
        }
        return txt.toString();
    }

    /**
     * Obtém a lista de blocos da blockchain.
     *
     * @return Lista de blocos
     */
    public List<Block> getChain() {
        return chain;
    }

    /**
     * Salva a blockchain num ficheiro.
     *
     * @param fileName Nome do ficheiro onde a cadeia será salva
     * @throws Exception Caso ocorra um erro ao salvar o ficheiro
     */
    public void save(String fileName) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
            out.writeObject(chain);
        }
    }

    /**
     * Carrega a blockchain a partir de um ficheiro.
     *
     * @param fileName Nome do ficheiro a ser carregado
     * @throws Exception Caso ocorra um erro ao carregar o ficheiro
     */
    public void load(String fileName) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            this.chain = (CopyOnWriteArrayList<Block>) in.readObject();
        }
    }

    /**
     * Valida a integridade da blockchain verificando os hashes e os blocos.
     *
     * @return True se a cadeia for válida, False caso contrário
     */
    public boolean isValid() {
        // Valida cada bloco individualmente
        for (Block block : chain) {
            if (!block.isValid()) {
                return false;
            }
        }
        // Valida os links entre os blocos
        for (int i = 1; i < chain.size(); i++) {
            //previous hash !=  hash of previous
            if (chain.get(i).previousHash.compareTo(chain.get(i - 1).currentHash) != 0) {
                return false;
            }
        }
        return true;
    }

}
