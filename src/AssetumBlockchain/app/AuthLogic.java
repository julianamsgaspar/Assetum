package AssetumBlockchain.app;

import java.rmi.Remote;
import p2p.IremoteP2P;
import utils.RMI;

/** Lógica de ligação/autenticação separada da UI. */
public class AuthLogic {

    public IremoteP2P connect(String nodeAddress) throws Exception {
        Remote r = RMI.getRemote(nodeAddress);
        if (!(r instanceof IremoteP2P)) {
            throw new IllegalStateException("Remote object is not IremoteP2P: " + r);
        }
        return (IremoteP2P) r;
    }

    public boolean register(IremoteP2P remote, String user, char[] pass) throws Exception {
        return remote.register(user, pass);
    }

    public boolean login(IremoteP2P remote, String user, char[] pass) throws Exception {
        return remote.login(user, pass);
    }
}
