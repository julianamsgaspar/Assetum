package AssetumBlockchain.app;

import AssetumBlockchain.core.AssetLedger;
import AssetumBlockchain.core.AssetTransaction;
import AssetumBlockchain.core.User;
import blockchain.utils.Block;
import java.rmi.RemoteException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import p2p.IremoteP2P;

/**
 * Camada de lógica do cliente: mantém sessão autenticada, mempool local
 * e operações de submit/refresh, separada da UI (JFrame).
 */
public class AssetumClientSession {

    private final IremoteP2P remote;
    private final String username;
    private final String password;

    private final User user; // carrega chaves do disco (modelo do projeto de referência)
    private final List<String> localPendingTx = new ArrayList<>();

    public AssetumClientSession(IremoteP2P remote, String username, String password) throws Exception {
        this.remote = Objects.requireNonNull(remote, "remote");
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");

        // User do projeto inicial: lê .priv/.pub e desencripta com password
        this.user = new User(username);
        this.user.load(password); // método existe no stub? garantimos abaixo com patch se necessário
    }

    public String getUsername() { return username; }

    public PublicKey getPublicKey() { return user.getPub(); }

    public PrivateKey getPrivateKey() { return user.getPriv(); }

    public IremoteP2P getRemote() { return remote; }

    /** Envia tx ao nó e adiciona ao buffer local. */
    public void submitTransaction(AssetTransaction tx) throws Exception {
        String payload = encode(tx);
        remote.addTransaction(payload);
        localPendingTx.add(payload);
    }

    /**
     * Implementa a regra "4 tx => criar bloco + mine + addBlock"
     * de forma independente da UI.
     */
    public boolean mineAndPublishIfReady(int zeros) throws Exception {
        if (localPendingTx.size() < 4) return false;

        List<String> pack = new ArrayList<>(localPendingTx.subList(0, 4));
        // remove do buffer local
        localPendingTx.subList(0, 4).clear();

        String prev = remote.getBlockchainLastHash();
        Block b = new Block(prev, pack);
        long nonce = remote.mine(b.getMinerData(), zeros);
        b.setNonce((int) nonce, zeros);
        remote.addBlock(b);
        return true;
    }

    public List<AssetTransaction> fetchBlockchainTransactionsDecoded() throws RemoteException {
        List<String> all = remote.getBlockchainTransactions();
        if (all == null) return Collections.emptyList();
        List<AssetTransaction> out = new ArrayList<>();
        for (String s : all) {
            AssetTransaction tx = tryDecode(s);
            if (tx != null) out.add(tx);
        }
        return out;
    }

    public AssetLedger buildLedgerFromBlockchain() throws RemoteException {
        AssetLedger ledger = new AssetLedger();
        for (AssetTransaction tx : fetchBlockchainTransactionsDecoded()) {
            if (tx.type == AssetTransaction.Type.REGISTER) { ledger.applyRegister(tx); }
            else if (tx.type == AssetTransaction.Type.TRANSFER) { ledger.applyTransfer(tx); }
        }
        return ledger;
    }

    public static String encode(AssetTransaction tx) throws Exception {
        byte[] bytes = utils.Serializer.objectToByteArray(tx);
        return "ASSETUMTX:" + Base64.getEncoder().encodeToString(bytes);
    }

    public static AssetTransaction tryDecode(String s) {
        try {
            if (s == null || !s.startsWith("ASSETUMTX:")) return null;
            String b64 = s.substring("ASSETUMTX:".length());
            byte[] bytes = Base64.getDecoder().decode(b64);
            Object o = utils.Serializer.byteArrayToObject(bytes);
            return (o instanceof AssetTransaction) ? (AssetTransaction) o : null;
        } catch (Exception e) {
            return null;
        }
    }
}
