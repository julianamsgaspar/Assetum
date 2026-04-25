/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package AssetumBlockchain.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import utils.SecurityUtils;
import utils.UtilizadorData;

/**
 *
 * @author macie
 */
public class User {

    private String name;
    private PublicKey pub;
    private PrivateKey priv;
    private Key sim;

    public User(String name) {
        this.name = name;
        this.pub = null;
        this.priv = null;
        this.sim = null;
    }

    public User() throws Exception {
        this("noName");
    }

    /**
     * Gera chaves tal como no projeto inicial:
     * - AES simétrica 256 bits
     * - par EC 256 bits
     */
    public void generateKeys() throws Exception {
        this.sim = SecurityUtils.generateAESKey(256);
        KeyPair kp = SecurityUtils.generateECKeyPair(256);
        this.pub = kp.getPublic();
        this.priv = kp.getPrivate();
    }

    /**
     * Guarda as chaves do utilizador, protegendo PRIV e SIM com password.
     */
    public void save(String password) throws Exception {
        if (priv == null || pub == null || sim == null) {
            throw new IllegalStateException("Keys not generated");
        }
        byte[] privSecret = SecurityUtils.encrypt(priv.getEncoded(), password);
        Files.write(Path.of(this.name + ".priv"), privSecret);

        byte[] simSecret = SecurityUtils.encrypt(sim.getEncoded(), password);
        Files.write(Path.of(this.name + ".sim"), simSecret);

        Files.write(Path.of(this.name + ".pub"), pub.getEncoded());
    }

    /**
     * Carrega as chaves do utilizador a partir dos ficheiros, desencriptando com password.
     */
    public void load(String password) throws Exception {
        byte[] privData = Files.readAllBytes(Path.of(this.name + ".priv"));
        privData = SecurityUtils.decrypt(privData, password);

        byte[] simData = Files.readAllBytes(Path.of(this.name + ".sim"));
        simData = SecurityUtils.decrypt(simData, password);

        byte[] pubData = Files.readAllBytes(Path.of(this.name + ".pub"));

        this.priv = SecurityUtils.getPrivateKey(privData);
        this.pub = SecurityUtils.getPublicKey(pubData);
        this.sim = SecurityUtils.getAESKey(simData);
    }

    public void loadPublic() throws Exception {
        byte[] pubData = Files.readAllBytes(Path.of(this.name + ".pub"));
        this.pub = SecurityUtils.getPublicKey(pubData);
    }

    /**
     * Autentica o utilizador validando que a password desencripta a chave privada.
     */
    public void autentica(String password) throws Exception {
        byte[] privData = Files.readAllBytes(Path.of(this.name + ".priv"));
        // Se password estiver errada, decrypt lança exceção.
        SecurityUtils.decrypt(privData, password);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PublicKey getPub() {
        return pub;
    }

    public void setPub(PublicKey pub) {
        this.pub = pub;
    }

    public PrivateKey getPriv() {
        return priv;
    }

    public void setPriv(PrivateKey priv) {
        this.priv = priv;
    }

    public Key getSim() {
        return sim;
    }

    public void setSim(Key sim) {
        this.sim = sim;
    }

    /**
     * Compatibilidade: grava utilizador a partir de dados recebidos via rede.
     * Atenção: ud.getPrivada() deve já estar cifrada (como no projeto de referência).
     */
    public static void gravarUtilizador(UtilizadorData ud) throws IOException {
        Files.write(Path.of(ud.getNome() + ".pub"), ud.getPub());
        Files.write(Path.of(ud.getNome() + ".priv"), ud.getPrivada());
    }

    public static void gravarUtilizador(String nome, byte[] priv) throws IOException {
        Files.write(Path.of(nome + ".priv"), priv);
    }
}
