package p2p;

import  AssetumBlockchain.core.User;
import static AssetumBlockchain.core.User.gravarUtilizador;
import blockchain.utils.Block;
import blockchain.utils.BlockChain;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import miner.Miner;
import utils.RMI;
import utils.UtilizadorData;

/**
 * Classe que implementa um servidor P2P remoto para a rede distribuída,
 * responsável por sincronizar transações, blocos de blockchain e dados de
 * utilizadores. A classe também gerencia a mineração e a comunicação entre os
 * nós da rede.
 *
 * @author Catarina - Miguel
 */
public class OremoteP2P extends UnicastRemoteObject implements IremoteP2P {

    // Definição do nome do ficheiro onde será armazenada a blockchain
    final static String BLOCHAIN_FILENAME = "blockchain.obj";

    // Endereço deste nó na rede
    String address;
    // Lista de nós conectados à rede
    CopyOnWriteArrayList<IremoteP2P> network;
    // Conjunto de eventos curriculares para sincronização
    CopyOnWriteArraySet<String> eventos;
    // Objeto para escutar e gerenciar eventos da rede P2P
    P2Plistener p2pListener;
    // Objeto mineiro concorrente e distribuido
    Miner myMiner;
    // Objeto da blockchain preparada para cesso concorrente
    BlockChain myBlockchain;

    /**
     * Construtor da classe OremoteP2P. Inicializa a rede, a lista de eventos, o
     * minerador e a blockchain.
     *
     * @param address Endereço deste nó na rede
     * @param listener O ouvinte que gerencia os eventos da rede
     * @throws RemoteException Se ocorrer um erro remoto
     */
    public OremoteP2P(String address, P2Plistener listener) throws RemoteException {
        super(RMI.getAdressPort(address)); // Inicializa a comunicação RMI com o endereço do nó
        this.address = address;
        this.network = new CopyOnWriteArrayList<>();
        eventos = new CopyOnWriteArraySet<>();
        this.myMiner = new Miner(listener); // Inicializa o minerador
        this.myBlockchain = new BlockChain(BLOCHAIN_FILENAME); // Inicializa a blockchain
        this.p2pListener = listener;

        listener.onStartRemote("Object " + address + " listening"); // Inicia o ouvinte
    }

    /**
     * Retorna o endereço deste nó na rede.
     *
     * @return O endereço do nó
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public String getAdress() throws RemoteException {
        return address;
    }

    /**
     * Método que verifica se um no está na rede e elimina os que não
     * responderem.
     *
     * @param adress Endereço do nó a verificar
     * @return true se estiver na rede, false caso contrario
     */
    private boolean isInNetwork(String adress) {
        //fazer o acesso iterado pelo fim do array para remover os nos inativos
        for (int i = network.size() - 1; i >= 0; i--) {
            try {
                //se o no responder e o endereço for igual
                if (network.get(i).getAdress().equals(adress)) {
                    // Nó encontrado na rede 
                    return true;
                }
            } catch (RemoteException ex) {
                // Se o nó não responder, removemos da rede
                network.remove(i);
            }
        }
        return false; // Nó não encontrado na rede
    }

    /**
     * Adiciona um novo nó à rede, propagando a adição para os outros nós.
     *
     * @param node O nó a ser adicionado
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public void addNode(IremoteP2P node) throws RemoteException {
        // Se já tiver o nó  ---  não faz nada
        if (isInNetwork(node.getAdress())) {
            return;
        }
        p2pListener.onMessage("Network addNode ", node.getAdress());
        // Adiciona o nó à lista de rede
        network.add(node);

        p2pListener.onConect(node.getAdress());
        // Pede ao nó para adicionar este nó
        node.addNode(this);
        for (IremoteP2P iremoteP2P : network) {
            iremoteP2P.addNode(node); // Propaga a adição para os outros nós
        }

        synchronizeTransactions(node); // Sincroniza transações com o novo nó
        synchronizeBlockchain(); // Sincroniza a blockchain com o novo nó
        node.synchronizeUtilizadores(this.getUsers()); // Sincroniza os utilizadores
    }

    /**
     * Retorna a lista de nós conectados à rede.
     *
     * @return Lista de nós conectados
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public List<IremoteP2P> getNetwork() throws RemoteException {
        return new ArrayList<>(network); // Retorna uma cópia da lista de rede
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::::::::            T R A N S A C T I O N S       ::::::::::::::::::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Retorna o número de transações na rede.
     *
     * @return O tamanho das transações
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public int getTransactionsSize() throws RemoteException {
        return eventos.size();
    }

    /**
     * Adiciona uma transação à rede, propagando-a para os outros nós e
     * sincronizando.
     *
     * @param evento O evento a ser adicionado como transação
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public void addTransaction(String evento) throws RemoteException {
        // Verificar se o evento já existe
        if (eventos.contains(evento)) {
            System.out.println("Evento curricular repetido: " + evento);
            return; // Se o evento já existe, não adicionamos
        }

        // Adicionar o evento à lista local
        eventos.add(evento);
        System.out.println("Evento adicionado localmente: " + evento);

        // Sincronizar com todos os nós da rede
        for (IremoteP2P node : network) {
            try {
                node.addTransaction(evento); // Propagar o evento para outros nós
            } catch (RemoteException e) {
                System.err.println("Erro ao sincronizar transação com o nó: " + node.getAdress());
            }
        }

        // Sincronizar transações para garantir consistência total
        for (IremoteP2P node : network) {
            try {
                node.synchronizeTransactions(this); // Sincronizar estados
            } catch (RemoteException e) {
                System.err.println("Erro ao sincronizar estado com o nó: " + node.getAdress());
            }
        }
    }

    /**
     * Retorna a lista de transações atuais.
     *
     * @return Lista de transações
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public List<String> getTransactions() throws RemoteException {
        return new ArrayList<>(eventos); // Retorna uma cópia da lista de transações
    }

    /**
     * Sincroniza as transações entre este nó e outro nó.
     *
     * @param node O nó com o qual sincronizar
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public void synchronizeTransactions(IremoteP2P node) throws RemoteException {
        //tamanho anterior
        int oldsize = eventos.size();
        p2pListener.onMessage("sinchronizeTransactions", node.getAdress());
        // juntar as transacoes todas (SET elimina as repetidas)
        this.eventos.addAll(node.getTransactions());
        int newSize = eventos.size();
        //se o tamanho for incrementado
        if (oldsize < newSize) {
            p2pListener.onMessage("sinchronizeTransactions", "tamanho diferente");
            //pedir ao no para sincronizar com as nossas
            node.synchronizeTransactions(this);
            p2pListener.onTransaction(address);
            p2pListener.onMessage("sinchronizeTransactions", "node.sinchronizeTransactions(this)");
            //pedir á rede para se sincronizar
            for (IremoteP2P iremoteP2P : network) {
                //se o tamanho for menor
                if (iremoteP2P.getTransactionsSize() < newSize) {
                    p2pListener.onMessage("sinchronizeTransactions", " iremoteP2P.sinchronizeTransactions(this)");
                    iremoteP2P.synchronizeTransactions(this); // Sincroniza com os outros nós da rede
                }
            }
        }

    }

    /**
     * Remove um conjunto de transações da rede.
     *
     * @param myTransactions Lista de transações a remover
     * @throws RemoteException Se ocorrer um erro remoto
     */
    @Override
    public void removeTransactions(List<String> myTransactions) throws RemoteException {
        //remover as transações da lista atual
        eventos.removeAll(myTransactions);
        p2pListener.onTransaction("remove " + myTransactions.size() + "transactions");
        //propagar as remoções
        for (IremoteP2P iremoteP2P : network) {
            //se houver algum elemento em comum nas transações remotas
            if (iremoteP2P.getTransactions().retainAll(eventos)) {
                //remover as transações
                iremoteP2P.removeTransactions(myTransactions);
            }
        }

    }

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::::::::::      M I N E R   :::::::::::::::::::::::::::::::::::::::
    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //////////////////////////////////////////////////////////////////////////////
    /**
     * Inicia o processo de mineração em este nó e em todos os outros nós da
     * rede.
     *
     * @param msg A mensagem a ser minerada
     * @param zeros O número de zeros exigidos no nonce
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public void startMining(String msg, int zeros) throws RemoteException {
        try {
            // Colocar a mineiro a minar localmente
            myMiner.startMining(msg, zeros);
            p2pListener.onStartMining(msg, zeros);

            // Envia o comando para começar a mineração nos outros nós da rede
            for (IremoteP2P iremoteP2P : network) {
                // Caso o nodo não estiver a minar
                if (!iremoteP2P.isMining()) {
                    p2pListener.onStartMining(iremoteP2P.getAdress() + " mining", zeros);
                    // Inicia a mineração nesse nodo
                    iremoteP2P.startMining(msg, zeros);
                }
            }
        } catch (Exception ex) {
            p2pListener.onException(ex, "startMining");
        }

    }

    /**
     * Para o processo de mineração tanto neste nó quanto nos outros nós da
     * rede.
     *
     * @param nonce O valor do nonce a ser distribuído
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public void stopMining(int nonce) throws RemoteException {
        // Para a mineração no nó local
        myMiner.stopMining(nonce);

        // Envia o comando para parar a mineração nos outros nós da rede
        for (IremoteP2P iremoteP2P : network) {
            // Caso o nodo estiver a minar   
            if (iremoteP2P.isMining()) {
                // Para a mineração no nodo 
                iremoteP2P.stopMining(nonce);
            }
        }
    }

    /**
     * Inicia o processo de mineração e retorna o nonce calculado após a
     * mineração.
     *
     * @param msg A mensagem a ser minerada
     * @param zeros O número de zeros exigidos no nonce
     * @return O nonce calculado após a mineração
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public int mine(String msg, int zeros) throws RemoteException {
        try {
            // Inicia a mineração
            startMining(msg, zeros);
            // Espera até o nonce ser calculado
            return myMiner.waitToNonce();
        } catch (InterruptedException ex) {
            p2pListener.onException(ex, "Mine");
            return -1;
        }

    }

    /**
     * Verifica se a mineração está em andamento neste nó.
     *
     * @return Retorna true se a mineração estiver em andamento, caso contrário,
     * false
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public boolean isMining() throws RemoteException {
        return myMiner.isMining();
    }

    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::::::::::::::::: B L O C K C H A I N :::::::::::::::::::::::::::::::::::::::
    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //////////////////////////////////////////////////////////////////////////////
    /**
     * Adiciona um bloco à blockchain, validando se o bloco é válido e se pode
     * ser encaixado. Em seguida, propaga o bloco para a rede.
     *
     * @param b O bloco a ser adicionado
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public void addBlock(Block b) throws RemoteException {
        try {
            // Verifica se o bloco é válido
            if (!b.isValid()) {
                throw new RemoteException("invalid block");
            }
            // Adiciona o bloco à blockchain local se encaixar
            if (myBlockchain.getLastBlockHash().equals(b.getPreviousHash())) {
                myBlockchain.add(b);
                // Salva a blockchain local
                myBlockchain.save(BLOCHAIN_FILENAME);
                p2pListener.onBlockchainUpdate(myBlockchain);
            }
            // Propaga o bloco para a rede
            for (IremoteP2P iremoteP2P : network) {
                //se encaixar na blockcahin dos nodos remotos
                if (!iremoteP2P.getBlockchainLastHash().equals(b.getPreviousHash())
                        || //ou o tamanho da remota for menor
                        iremoteP2P.getBlockchainSize() < myBlockchain.getSize()) {
                    //adicionar o bloco ao nodo remoto
                    iremoteP2P.addBlock(b);
                }
            }
            //se não encaixou)
            if (!myBlockchain.getLastBlockHash().equals(b.getCurrentHash())) {
                //sincronizar a blockchain
                synchronizeBlockchain();
            }
        } catch (Exception ex) {
            p2pListener.onException(ex, "Add bloco " + b);
        }
    }

    /**
     * Retorna o tamanho da blockchain local.
     *
     * @return O número de blocos na blockchain
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public int getBlockchainSize() throws RemoteException {
        return myBlockchain.getSize();
    }

    /**
     * Retorna o hash do último bloco na blockchain local.
     *
     * @return O hash do último bloco
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public String getBlockchainLastHash() throws RemoteException {
        return myBlockchain.getLastBlockHash();
    }

    /**
     * Retorna a instância da blockchain local.
     *
     * @return A blockchain local
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public BlockChain getBlockchain() throws RemoteException {
        return myBlockchain;
    }

    /**
     * Sincroniza a blockchain local com os outros nós da rede.
     *
     * @throws RemoteException Se ocorrer um erro durante a execução remota.
     */
    @Override
    public void synchronizeBlockchain() throws RemoteException {
        // Verifica se a blockchain de algum nó remoto é maior que a local
        for (IremoteP2P iremoteP2P : network) {
            if (iremoteP2P.getBlockchainSize() > myBlockchain.getSize()) {
                BlockChain remote = iremoteP2P.getBlockchain();
                // Verifica se a blockchain remota é válida
                if (remote.isValid()) {
                    //atualizar toda a blockchain
                    myBlockchain = remote;
                    // Atualiza a blockchain local com a blockchain remota
                    p2pListener.onBlockchainUpdate(myBlockchain);
                }
            }
        }
    }

    /**
     * Retorna todas as transações na blockchain local.
     *
     * @return Uma lista de transações na blockchain local
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public List<String> getBlockchainTransactions() throws RemoteException {
        ArrayList<String> allTransactions = new ArrayList<>();
        for (Block b : myBlockchain.getChain()) {
            allTransactions.addAll(b.transactions());
        }
        return allTransactions;
    }

    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::::::::::::::::: LOGIN / REGISTER / USERS :::::::::::::::::::::::::::::::::::::::
    //:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //////////////////////////////////////////////////////////////////////////////
    /**
     * Regista um novo utilizador no sistema, gerando as chaves de criptografia
     * e salvando os dados.
     *
     * @param nome O nome do utilizador a ser registado
     * @param password A senha do utilizador
     * @return Retorna true se o registo foi bem-sucedido, caso contrário, false
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public boolean register(String nome, char[] password) throws RemoteException {
        try {
            // Criação de um novo utilizador com o nome fornecido
            User u = new User(nome);
            // Geração das chaves criptográficas para o utilizador
            u.generateKeys();
            // Salva a senha fornecida (converte o array de chars para String)
            u.save(new String(password));
            // Sincroniza os utilizadores com outros nós na rede
            synchronizeUtilizadores(this.getUsers());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Realiza o login de um utilizador, verificando a senha fornecida.
     *
     * @param nome O nome do utilizador
     * @param password A senha do utilizador
     * @return Retorna true se o login for bem-sucedido, caso contrário, false
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public boolean login(String nome, char[] password) throws RemoteException {
        // Criação de um novo utilizador com o nome fornecido
        User u = new User(nome);
        try {
            // Tenta autenticar o utilizador com a senha fornecida
            u.autentica(new String(password));
        } catch (Exception ex) {
            Logger.getLogger(OremoteP2P.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    /**
     * Retorna uma lista de dados dos utilizadores registrados no sistema.
     *
     * @return Uma lista de objetos UtilizadorData contendo os dados dos
     * utilizadores
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public ArrayList<UtilizadorData> getUsers() throws RemoteException {

        ArrayList<UtilizadorData> listautilizadores = new ArrayList<>();

        // Define o caminho para a root do projeto
        String projectRootPath = System.getProperty("user.dir");

        // Cria um objeto File que representa a root do projeto
        File projectRoot = new File(projectRootPath);

        // Obtém a lista de ficheiros e diretórios
        File[] files = projectRoot.listFiles();

        if (files != null) {
            // Itera sobre os arquivos para verificar os ficheiros de utilizador
            for (File file : files) {
                // Verifica se é um ficheiro .priv
                if (file.isFile() && file.getName().endsWith(".priv")) {
                    String username = file.getName().replace(".priv", ""); // Extrai o nome do usuário                    
                    // Define o caminho para o arquivo .pub relacionado
                    File pubFile = new File(projectRoot, username + ".pub");
                    //File simFile = new File(projectRoot, username + ".sim");

                    // Verifica se o arquivo .pub existe
                    if (pubFile.exists() && pubFile.isFile()) {
                        try {
                            // Lê os conteúdos dos arquivos
                            byte[] privada = Files.readAllBytes(file.toPath());
                            byte[] pub = Files.readAllBytes(pubFile.toPath());

                            // Cria o objeto UtilizadorData e adiciona à lista
                            listautilizadores.add(new UtilizadorData(username, privada, pub));

                        } catch (Exception ex) {
                            System.out.println("Erro ao ler ficheiros para o usuário: " + username);
                            Logger.getLogger(OremoteP2P.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else {
                        // Informa que os arquivos relacionados não foram encontrados
                        System.out.println("Arquivos relacionados não encontrados para o usuário: " + username);
                    }
                }
            }
        } else {
            System.out.println("Não foi possível listar os ficheiros.");
        }

        // Devolve o array com todos os utilizadores
        return listautilizadores;
    }

    /**
     * Sincroniza os dados dos utilizadores entre todos os nós da rede.
     *
     * @param lista A lista de utilizadores a ser sincronizada
     * @throws RemoteException Se ocorrer um erro durante a execução remota
     */
    @Override
    public void synchronizeUtilizadores(ArrayList<UtilizadorData> lista) throws RemoteException {

        try {
            // Obtém a lista atual de utilizadores
            ArrayList<UtilizadorData> au = getUsers();
            // Verifica se a lista recebida é maior que a lista local
            if (au.size() < lista.size()) {
                // Se houver novos utilizadores, grava-os
                for (UtilizadorData ud : lista) {
                    if (!au.contains(ud)) {
                        gravarUtilizador(ud); // Grava o utilizador novo
                    }
                }

                // Propaga a atualização para todos os nós da rede
                for (IremoteP2P iremoteP2P : network) {
                    iremoteP2P.synchronizeUtilizadores(lista);
                }
            } else {
                // Se as listas já forem iguais, não faz nada
                return;
            }
        } catch (IOException ex) {
            Logger.getLogger(OremoteP2P.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
