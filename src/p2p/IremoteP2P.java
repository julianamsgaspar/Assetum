/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package p2p;

import blockchain.utils.Block;
import blockchain.utils.BlockChain;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import utils.UtilizadorData;

/**
 * A interface IremoteP2P define os métodos remotos que permitem a comunicação entre
 * os nós de uma rede peer-to-peer (P2P). Esta interface será utilizada para
 * interagir com objetos remotos de uma rede distribuída, facilitando a troca
 * de informações relacionadas a transações, mineração e blockchain.
 * 
 * @author Catarina - Miguel
 */
public interface IremoteP2P extends Remote {

    //:::: N E T WO R K  :::::::::::
    public String getAdress() throws RemoteException;

    public void addNode(IremoteP2P node) throws RemoteException;

    public List<IremoteP2P> getNetwork() throws RemoteException;

    //::::::::::: T R A N S A C T IO N S  :::::::::::
    public int getTransactionsSize() throws RemoteException;

    public void addTransaction(String evento) throws RemoteException;

    public List<String> getTransactions() throws RemoteException;

    public void removeTransactions(List<String> transactions) throws RemoteException;

    public void synchronizeTransactions(IremoteP2P node) throws RemoteException;

    //::::::::::::::::: M I N E R :::::::::::::::::::::::::::::::::::::::::::
    public void startMining(String msg, int zeros) throws RemoteException;

    public void stopMining(int nonce) throws RemoteException;

    public boolean isMining() throws RemoteException;

    public int mine(String msg, int zeros) throws RemoteException;

    //::::::::::::::::: B L O C K C H A I N :::::::::::::::::::::::::::::::::::::::::::
    public void addBlock(Block b) throws RemoteException;

    public int getBlockchainSize() throws RemoteException;

    public String getBlockchainLastHash() throws RemoteException;

    public BlockChain getBlockchain() throws RemoteException;

    public void synchronizeBlockchain() throws RemoteException;

    public void synchronizeUtilizadores(ArrayList<UtilizadorData> lista) throws RemoteException;

    public List<String> getBlockchainTransactions() throws RemoteException;

    //::::::::::::::::: LOGIN & REGISTO :::::::::::::::::::::::::::::::::::::::::::
    public boolean register(String nome, char[] password) throws RemoteException;

    public boolean login(String nome, char[] password) throws RemoteException;

    public List<UtilizadorData> getUsers() throws RemoteException;

}
