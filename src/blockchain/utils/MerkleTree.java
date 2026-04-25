package blockchain.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Esta classe implementa uma Merkle Tree, uma estrutura de dados que permite
 * verificar a integridade e validade de conjuntos de dados de forma eficiente.
 * A classe inclui funcionalidades para construção, validação, prova de
 * integridade de elementos e serialização.
 *
 *
 * @author Catarina - Miguel
 */
public final class MerkleTree implements Serializable {

    // Lista que representa a árvore de hashes em níveis
    private List<List<String>> hashTree;

    // Lista que contém os elementos originais da Merkle Tree
    List elements;

    /**
     * Construtor que cria uma Merkle Tree com base num array de dados
     *
     * @param arrayOfData Array que contém os dados que serão armazenados
     */
    public MerkleTree(Object[] arrayOfData) {
        this(Arrays.asList(arrayOfData));

    }

    /**
     * Construtor que cria uma Merkle Tree com base numa lista de dados
     *
     * @param listOfData Lista que contém os dados para a Merkle Tree
     */
    public MerkleTree(List listOfData) {
        this(); // Inicializa as listas internas
        // Adiciona os dados fornecidos
        elements.addAll(listOfData);
        //calcula lista de hashs dos elementos
        List<String> hashT = new ArrayList<>();
        for (Object elem : listOfData) {
            // Converte o elemento para string e calcula o hash correspondente
            hashT.add(getHashValue(elem.toString()));
        }
        // Constrói a árvore a partir dos hashes
        makeTree(hashT);
    }

    /**
     * Construtor que cria uma Merkle Tree vazia
     */
    public MerkleTree() {
        hashTree = new ArrayList<>(); // Inicializa a lista de níveis
        elements = new ArrayList<>(); // Inicializa a lista de elementos
    }

    /**
     * Obtém a raiz da árvore (o hash no topo da Merkle Tree)
     *
     * @return String com o hash da raiz da árvore.
     */
    public String getRoot() {
        // Retorna o hash do topo da árvore
        return hashTree.get(0).get(0);
    }

    /**
     * Constrói a Merkle Tree a partir de uma lista de hashes
     *
     * @param hashList Lista de hashes que será usada para construir a árvore
     */
    public void makeTree(List<String> hashList) {
        // Adiciona o nível mais baixo à árvore
        hashTree.add(0, hashList);
        //top of tree -> terminate
        if (hashList.size() <= 1) {
            return; // Se há apenas um elemento, a árvore está completa (topo)
        }
        //Fazer o próximo nível
        List<String> newLevel = new ArrayList<>();
        //iterate list 2 by 2
        for (int i = 0; i < hashList.size(); i += 2) {
            //primeiro elemento
            String data = hashList.get(i);
            //caso exista outro elemento
            if (i + 1 < hashList.size()) {
                //concatena o elemento à direita  
                data = data + hashList.get(i + 1);
            }
            //calcula o hash dos elementos concatenados
            String hash = getHashValue(data);
            // Adiciona o hash concatenado ao novo nível
            newLevel.add(hash);
        }
        // Chama recursivamente para construir o próximo nível
        makeTree(newLevel);
    }

    /**
     * Obtém a prova de integridade de um dado elemento da árvore.
     *
     * @param data Elemento para o qual a prova será gerada
     * @return Lista de hashes que representam a prova de integridade
     */
    public List<String> getProof(Object data) {
        // Lista para armazenar a prova
        List<String> proof = new ArrayList<>();
        // Índice do elemento na lista de dados
        int index = elements.indexOf(data);
        if (index < 0) {
            return proof; // Retorna lista vazia se o elemento não for encontrado
        }
        // Calcula a prova
        return getProof(index, hashTree.size() - 1, proof);
    }

    /**
     * Calcula a prova de integridade para um dado elemento.
     *
     * @param index Índice do elemento no nível atual
     * @param level Nível atual da árvore
     * @param proof Lista que contém a prova acumulada
     * @return Lista atualizada com a prova de integridade
     */
    private List<String> getProof(int index, int level, List<String> proof) {

        if (level > 0) { // not the top
            if (index % 2 == 0) { // Índice par              
                //se existir elementos à direita
                if (index + 1 < hashTree.get(level).size()) {
                    //adiciona elemento 
                    proof.add(hashTree.get(level).get(index + 1));
                } else {
                    //adiciona hash do elemento
                    proof.add(hashTree.get(level).get(index));
                }
            } else { // Índice ímpar
                proof.add(hashTree.get(level).get(index - 1));
            }
            // Avança para o próximo nível
            return getProof(index / 2, level - 1, proof);
        } else {
            // Adiciona a raiz da árvore à prova
            proof.add(getRoot());
            return proof;
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::::::::::::::::   V A L I D A T E    T R E E  ::::::::::::::::
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Verifica a validade da prova de um elemento
     *
     * @param data Dados do elemento
     * @param proof Lista de provas associadas ao elemento
     * @return Retorna true se a prova for válida, caso contrário, false
     */
    public static boolean isProofValid(Object data, List<String> proof) {

        // Verifica se a lista de provas está vazia
        if (proof.isEmpty()) {
            return false;
        }
        // Obtém o hash do elemento atual
        String currentHash = getHashValue(data.toString());
        // Verifica a validade da prova recursivamente, começando do índice 0
        return isProofValid(currentHash, proof, 0);
    }

    /**
     * Método recursivo que verifica a validade da prova, usando o hash atual e
     * a lista de provas
     *
     * @param currentHash O hash atual do dado
     * @param proof Lista de provas
     * @param indexOfList Índice da lista de provas
     * @return Retorna true se a prova for válida, false caso contrário
     */
    public static boolean isProofValid(String currentHash, List<String> proof, int indexOfList) {
        // Se chegamos ao topo da árvore (último nível)
        if (indexOfList == proof.size() - 1) {
            return currentHash.equals(proof.get(proof.size() - 1)); // Verifica se o hash corresponde à prova final
        }

        // Concatenar o hash atual com o próximo na lista de provas (lado direito)
        String newHash = getHashValue(currentHash + proof.get(indexOfList));
        // Verifica o próximo nível 
        if (isProofValid(newHash, proof, indexOfList + 1)) {
            return true;
        }
        // Concatenar o hash atual com o próximo na lista de provas (lado esquerdo)
        newHash = getHashValue(proof.get(indexOfList) + currentHash);
        // Verifica o próximo nível 
        return isProofValid(newHash, proof, indexOfList + 1);

    }

    /**
     * verifica se a merkle tree é válida
     *
     * @return Retorna true caso seja válida, caso contrário false
     */
    public boolean isValid() {
        // Verifica se os hashes dos elementos na base da árvore são consistentes
        for (int i = 0; i < this.elements.size(); i++) {
            if (!getHashValue(this.elements.get(i).toString()).equals(hashTree.get(hashTree.size() - 1).get(i))) {
                return false; // Se algum hash não coincidir, a árvore não é válida
            }
        }
        //verifica os niveis da árvore
        for (int level = 0; level < hashTree.size() - 1; level++) {
            // Verifica cada índice do nível          
            for (int index = 0; index < hashTree.get(level).size(); index++) {
                // Obter o valor da folha à esquerda
                String dataLeafs = hashTree.get(level + 1).get(index * 2);
                // Se houver uma folha à direita
                if (index * 2 + 1 < hashTree.get(level + 1).size()) {
                    // Concatenar os hashes das folhas
                    dataLeafs = dataLeafs + hashTree.get(level + 1).get(index * 2 + 1);
                }
                // Calcula o hash das folhas concatenadas
                String hash = getHashValue(dataLeafs);
                // Verifica se o hash calculado corresponde ao valor esperado no nível superior
                if (hashTree.get(level).get(index).equals(hash)) {
                    return false;
                }
            }
        }
        // Se todas as verificações passarem, a árvore é válida
        return true;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::::::::::::::::  T O   S T R I N G        ::::::::::::::::
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Retorna uma representação em string da Merkle Three
     *
     * @return Representação em string da árvore Merkle
     */
    @Override
    public String toString() {
        return toTree();
    }

    /**
     * Converte a árvore Merkle para uma string formatada
     *
     * @return String formatada representando a Merkle Three
     */
    public String toTree() {
        int SIZE = 9; // Tamanho mínimo de cada elemento na árvore
        for (Object elem : elements) {
            // Calcula o tamanho máximo do texto que pode ser exibido na árvore
            if (elem.toString().length() > SIZE) {
                SIZE = elem.toString().length();
            }
        }
        // Construção da representação da árvore      
        StringBuilder txt = new StringBuilder();
        for (int i = 0; i < hashTree.size(); i++) {
            // Espaços no início de cada linha para centralizar os elementos
            int ini = (int) Math.pow(2, hashTree.size() - i - 1) - 1;
            int middle = (int) Math.pow(2, hashTree.size() - i) - 1;
            // Adiciona espaços no início da linha
            if (ini > 0) {
                txt.append(String.format("%" + ini * SIZE + "s", ""));
            }
            // Adiciona os elementos da linha
            for (String hash : hashTree.get(i)) {
                //elemento
                txt.append(centerString(hash, SIZE));
                //espaços
                txt.append(String.format("%" + middle * SIZE + "s", ""));
            }
            txt.append("\n");

        }
        // Adiciona os elementos da base da árvore
        for (Object elem : elements) {
            txt.append(centerString(elem.toString(), SIZE));
            txt.append(String.format("%" + SIZE + "s", ""));
        }

        return txt.toString();
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::::::::::::::::   S A V E   /    L O A D      ::::::::::::::::
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Salva a Merkle Three em um ficheiro
     *
     * @param fileName Nome do ficheiro onde a árvore será salva
     * @throws FileNotFoundException Se o ficheiro não for encontrado
     * @throws IOException Se ocorrer um erro de I/O ao salvar o ficheiro
     */
    public void saveToFile(String fileName) throws FileNotFoundException, IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
            out.writeObject(this);
        }
    }

    /**
     * Carrega uma Merkle Three a partir de um ficheiro
     *
     * @param fileName Nome do ficheiro de onde a árvore será carregada
     * @return A Merkle Three carregada
     * @throws FileNotFoundException Se o ficheiro não for encontrado
     * @throws IOException Se ocorrer um erro de I/O ao carregar o ficheiro
     * @throws ClassNotFoundException Se a classe MerkleTree não for encontrada
     */
    public static MerkleTree loadFromFile(String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            return (MerkleTree) in.readObject();
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::::::          E N C A P S U L A M E N T O                      :::::::::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::    
    ///////////////////////////////////////////////////////////////////////////
    // Métodos para obter os elementos e a árvore de hashes
    public List<List<String>> getHashTree() {
        return hashTree;
    }

    /**
     * Retorna os elementos da árvore Merkle.
     *
     * @return Lista de elementos
     */
    public List getElements() {
        return elements;
    }

    /**
     * Retorna os elementos da árvore Merkle como uma string formatada.
     *
     * @return String com todos os elementos da árvore
     */
    public String getElementsString() {
        StringBuilder txt = new StringBuilder();
        for (Object obj : elements) {
            txt.append(obj.toString() + "\n");
        }
        return txt.toString().trim();
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::::::                                                           :::::::::
    //::::::                         U T I L S                         :::::::::
    //::::::                                                           :::::::::
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Centraliza uma string numa largura especificada
     *
     * @param text Texto a ser centralizado
     * @param len Comprimento total da string resultante
     * @return String centralizada
     */
    public static String centerString(String text, int len) {
        String out = String.format("%" + len + "s%s%" + len + "s", "", text, "");
        float mid = (out.length() / 2);
        float start = mid - (len / 2);
        float end = start + len;
        return out.substring((int) start, (int) end);
    }

    /**
     * Converte um número inteiro em hexadecimal.
     *
     * @param i Número inteiro
     * @return String representando o número em hexadecimal
     */
    public static String intToHex(int i) {
        return Integer.toString(i, 16).toUpperCase();
    }

    /**
     * Calcula o valor de hash de uma string de dados usando o método
     * `hashCode()`.
     *
     * @param data Dados para calcular o hash
     * @return Valor do hash calculado
     */
    public static String getHashValue(String data) {
        return intToHex(Math.abs(data.hashCode()));
    }

}
