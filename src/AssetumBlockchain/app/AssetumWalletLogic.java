package AssetumBlockchain.app;

import AssetumBlockchain.core.AssetLedger;
import AssetumBlockchain.core.AssetTransaction;
import AssetumBlockchain.core.User;
import blockchain.utils.Block;
import java.rmi.RemoteException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import p2p.IremoteP2P;
import utils.Serializer;

/** Lógica da wallet: tx, batch mining, leitura de blockchain e ledger. */
public class AssetumWalletLogic {

    private final IremoteP2P remote;
    private final String nodeAddress;
    private final String username;
    private final String password;

    private final User user;
    private final List<String> pendingLocal = new ArrayList<>();

    private final int batchSize;
    private final int powZeros;

    public AssetumWalletLogic(IremoteP2P remote, String nodeAddress, String username, String password, int batchSize, int powZeros) throws Exception {
        this.remote = Objects.requireNonNull(remote, "remote");
        this.nodeAddress = nodeAddress;
        this.username = username;
        this.password = password;
        this.batchSize = batchSize;
        this.powZeros = powZeros;

        this.user = new User(username);
        this.user.load(password);
    }

    public IremoteP2P remote() { return remote; }
    public String nodeAddress() { return nodeAddress; }
    public String username() { return username; }

    public PrivateKey privateKey() { return user.getPriv(); }

    public void submitTx(AssetTransaction tx) throws Exception {
        submitEncodedTxString(encode(tx));
    }


public void submitEncodedTxString(String payload) throws Exception {
    remote.addTransaction(payload);
    pendingLocal.add(payload);
}


    public boolean mineIfReadyAndPublish() throws Exception {
        if (pendingLocal.size() < batchSize) return false;
        List<String> pack = new ArrayList<>(pendingLocal.subList(0, batchSize));
        pendingLocal.subList(0, batchSize).clear();

        String prev = remote.getBlockchainLastHash();
        Block b = new Block(prev, pack);
        long nonce = remote.mine(b.getMinerData(), powZeros);
        b.setNonce((int) nonce, powZeros);
        remote.addBlock(b);
        return true;
    }

    public List<String> fetchBlockchainTxStrings() throws RemoteException {
        List<String> txs = remote.getBlockchainTransactions();
        return txs == null ? List.of() : txs;
    }

    public AssetLedger buildLedger() throws RemoteException {
        AssetLedger ledger = new AssetLedger();
        for (String s : fetchBlockchainTxStrings()) {
            AssetTransaction tx = tryDecode(s);
            if (tx != null) if (tx.type == AssetTransaction.Type.REGISTER) { ledger.applyRegister(tx); }
            else if (tx.type == AssetTransaction.Type.TRANSFER) { ledger.applyTransfer(tx); }
        }
        return ledger;
    }

    public static String encode(AssetTransaction tx) throws Exception {
        byte[] bytes = Serializer.objectToByteArray(tx);
        return "ASSETUMTX:" + Base64.getEncoder().encodeToString(bytes);
    }

    public static AssetTransaction tryDecode(String s) {
        try {
            if (s == null || !s.startsWith("ASSETUMTX:")) return null;
            String b64 = s.substring("ASSETUMTX:".length());
            Object o = Serializer.byteArrayToObject(Base64.getDecoder().decode(b64));
            return (o instanceof AssetTransaction) ? (AssetTransaction) o : null;
        } catch (Exception e) {
            return null;
        }
    }
}
