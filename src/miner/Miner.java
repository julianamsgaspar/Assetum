package miner;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import p2p.P2Plistener;

/**
 * Classe responsável por minerar uma mensagem, procurando um nonce que faça o
 * hash da mensagem começar com um número específico de zeros. A mineração é
 * feita através de threads, onde cada uma tenta encontrar o nonce correto de
 * forma paralela.
 *
 * @author Catarina - Miguel
 */
public class Miner {

    //atributos 
    P2Plistener listener;            // Listener dos mineiros
    private MinerThread[] threads;      // Threads de calculo de hashs
    private String message;             //  Mensagem a ser minada 
    private AtomicInteger globalNonce;  // Nonce que valida a mensagem

    /**
     * Construtor que inicializa o listener.
     *
     * @param listener objeto que ouve os eventos de mineração.
     */
    public Miner(P2Plistener listener) {
        this.listener = listener;
    }

    /**
     * Inicia a mineração de uma mensagem.
     *
     * @param message mensagem a ser minada
     * @param zeros número de zeros que o hash precisa ter no início
     * @throws Exception se ocorrer um erro ao iniciar a mineração
     */
    public void startMining(String message, int zeros) throws Exception {
        // Verifica se já está a minerar, e se sim, retorna
        if (isMining()) {
            return; // Sair
        }
        this.message = message;
        // Configura o número de threads (cores) 
        int numCores = 2; //Runtime.getRuntime().availableProcessors();
        threads = new MinerThread[numCores];
        //inicializar o globalNonce
        globalNonce = new AtomicInteger();

        // Executa as threads para iniciar a mineração
        for (int i = 0; i < numCores; i++) {
            threads[i] = new MinerThread(globalNonce, message, zeros);
            threads[i].start();
        }
        // Notifica o listener sobre o início da mineração
        if (listener != null) {
            listener.onStartMining("Start Mining " + numCores + " cores", zeros);
        }

    }

    /**
     * Termina a mineração e atualiza o nonce.
     *
     * @param nonce valor do nonce encontrado
     */
    public void stopMining(int nonce) {
        // Atualiza o nonce global
        globalNonce.set(nonce);
        if (listener != null) {
            listener.onStopMining("Stop Mining" + Thread.currentThread().getName(), nonce);
        }
        //aborta as threads
        if (threads != null) {
            for (MinerThread thread : threads) {
                thread.interrupt();
            }
            threads = null;
        }

    }

    /**
     * Verificar se está a minerar
     *
     * @return true se estiver a minerar, false caso contrário
     */
    public boolean isMining() {
        return threads != null && globalNonce != null && globalNonce.get() <= 0;
    }

    /**
     * Devolve o nonce encontrado durante a mineração.
     *
     * @return nonce
     */
    public int getNonce() {
        return globalNonce.get();
    }

    /**
     * Devolve a mensagem que está a ser minada.
     *
     * @return mensagem
     */
    public String getMessage() {
        return message;
    }

    /**
     * Formata o tempo de mineração.
     *
     * @param miningTime tempo de mineração
     * @return tempo formatado
     */
    public static String getMiningTimeText(long miningTime) {
        return df.format(new Date(miningTime));
    }
    private static final SimpleDateFormat df = new SimpleDateFormat("mm:ss.SSSS");

    /**
     * Aguarda até que o nonce seja encontrado.
     *
     * @return nonce
     * @throws InterruptedException se a thread for interrompida.
     */
    public int waitToNonce() throws InterruptedException {
        for (MinerThread thread : threads) {
            thread.join();
        }
        return globalNonce.get();
    }

    /**
     * Inicia a mineração e aguarda o nonce ser encontrado.
     *
     * @param message mensagem a ser minada
     * @param zeros número de zeros
     * @return nonce encontrado
     * @throws Exception se ocorrer um erro durante a mineração
     */
    public int mine(String message, int zeros) throws Exception {
        startMining(message, zeros);
        return waitToNonce();
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //:::::::::      I N T E G R I T Y         :::::::::::::::::::::::::::::::::    
    ///////////////////////////////////////////////////////////////////////////
    public static String hashAlgorithm = "SHA3-256"; // Algoritmo de hash usado

    /**
     * Calcula a hash da mensagem concatenada com o nonce, em Base64.
     *
     * @param data dados da mensagem
     * @param nonce nonce
     * @return hash da mensagem com o nonce em Base64
     */
    public static String getHash(String data, int nonce) {
        try {
            return getHash(data + nonce);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    /**
     * Calcula a hash da mensagem em Base64.
     *
     * @param data dados da mensagem
     * @return hash em Base64
     * @throws Exception se ocorrer um erro durante o cálculo da hash
     */
    public static String getHash(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
        return Base64.getEncoder().encodeToString(md.digest(data.getBytes()));
    }

    // :::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * Thread responsável por minerar uma mensagem e procurar o nonce.
     */
    private class MinerThread extends Thread {

        //atributos da thread        
        private final AtomicInteger sharedNonce;  // referência para o global nonce
        private final String message;             // mensagem do bloco
        private final int zeros;                  // número de zeros no hash
        private final MessageDigest hasher;       // calculador de  hashs da thread

        /**
         * Construtor da thread para minerar a mensagem.
         *
         * @param globalNonce objeto compartilhado com o nonce global
         * @param message mensagem a ser minada
         * @param zeros número de zeros que o hash precisa ter no início
         * @throws NoSuchAlgorithmException se o algoritmo de hash não for
         * encontrado
         */
        public MinerThread(AtomicInteger globalNonce, String message, int zeros) throws NoSuchAlgorithmException {
            this.sharedNonce = globalNonce;
            this.message = message;
            this.zeros = zeros;

            // Cria um objeto para calcular a hash na thread
            this.hasher = MessageDigest.getInstance(hashAlgorithm);
        }

        @Override
        public void run() {
            try {
                // Notifica o listener sobre o início da mineração na thread
                if (listener != null) {
                    listener.onStartMining("RUN " + Thread.currentThread().getName(), zeros);
                }
                // Prefixo com os zeros no início do hash
                String prefix = String.format("%0" + zeros + "d", 0);
                //enquanto não for encontrado o nonce ( nonce <= 0 )
                while (sharedNonce.get() <= 0) {
                    // Gera um número aleatório e testa-o
                    int number = Math.abs(ThreadLocalRandom.current().nextInt());
                    if (listener != null && number % 368 == 0) {
                        listener.onException(new Exception(number + ""), "number");
                    }

                    // Verifica se o hash começa com o prefixo (zeros)
                    if (getThreadHash(message, number).startsWith(prefix)) {
                        // Atualiza o nonce e termina as threads
                        sharedNonce.set(number);
                        //notifificar os listeners
                        if (listener != null) {
                            listener.onException(new Exception(number + ""), "nonce");
                            listener.onNounceFound(Thread.currentThread().getName(), number);
                        }
                    }
                }
                // Notifica os listeners que a thread terminou
                if (listener != null) {
                    listener.onStopMining(Thread.currentThread().getName(), sharedNonce.get());
                }
            } catch (Exception ex) {
                //alguma coisa deu errado  
                //notificar os listeners a cada 9973 numeros
                if (listener != null) {
                    listener.onStopMining("ERROR " + ex.getMessage(), -1);
                }
            }
        }

        /**
         * Calcula a hash da mensagem concatenada com o nonce, em Base64.
         *
         * @param message mensagem a ser minada
         * @param nonce nonce
         * @return hash da mensagem com o nonce
         * @throws Exception se ocorrer um erro durante o cálculo da hash
         */
        public String getThreadHash(String message, int nonce) throws Exception {
            return Base64.getEncoder().encodeToString(hasher.digest((message + nonce).getBytes()));
        }

    }

}
