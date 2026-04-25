/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package p2p;

import blockchain.utils.BlockChain;

/**
 * Interface que define os métodos para lidar com eventos numa rede P2P,
 * como exceções, mensagens, transações, mineração e atualizações da blockchain.
 * 
 * 
 * @author Catarina - Miguel
 */
public interface P2Plistener {

    public void onException(Exception ex, String message);

    public void onMessage(String title, String message);

    public void onStartRemote(String message);

    public void onConect(String address);

    public void onTransaction(String transaction);

    public void onStartMining(String message, int zeros);

    public void onStopMining(String message, int nonce);

    public void onNounceFound(String message, int nonce);
    
    public void onBlockchainUpdate(BlockChain b);

}
