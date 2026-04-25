package utils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 * A classe `RMI` contém métodos auxiliares para manipulação e utilização de
 * objetos remotos no contexto de RMI (Remote Method Invocation). Ela fornece
 * funcionalidades para obter, criar, e parar objetos remotos, além de lidar com
 * endereços de objetos RMI.
 *
 * @author Catarina - Miguel
 */
public class RMI {

    /**
     * Obtém o nome remoto de um objeto no servidor RMI.
     *
     * @param port número da porta em que o servidor RMI está ouvindo
     * @param objectName nome do objeto remoto
     * @return o endereço remoto RMI formatado como string
     * @throws UnknownHostException se o host não for encontrado
     */
    public static String getRemoteName(int port, String objectName) throws UnknownHostException {
        // Obtém o endereço do localhost e retorna o nome remoto formatado     
        return getRemoteName(InetAddress.getLocalHost().getHostAddress(), port, objectName);
    }

    /**
     * Obtém o nome remoto de um objeto.
     *
     * @param host nome do host remoto
     * @param port número da porta em que o servidor RMI está ouvindo
     * @param objectName nome do objeto remoto
     * @return o endereço remoto RMI formatado como string
     */
    public static String getRemoteName(String host, int port, String objectName) {
        // Formata o endereço remoto RMI
        return String.format("//%s:%d/%s", host, port, objectName);
    }

    /**
     * Torna um objeto remoto disponível no servidor.
     *
     * @param remote objeto remoto que será disponibilizado
     * @param port número da porta em que o servidor RMI deve ouvir
     * @param objectName nome do objeto remoto
     * @throws RemoteException se houver um erro na operação remota
     * @throws UnknownHostException se o host não for encontrado
     * @throws MalformedURLException se o endereço RMI for inválido
     */
    public static void startRemoteObject(Remote remote, int port, String objectName)
            throws RemoteException, UnknownHostException, MalformedURLException {
        // Usa o método auxiliar para obter o nome remoto e inicia o objeto remoto
        startRemoteObject(remote, getRemoteName(port, objectName));

    }

    /**
     * Torna um objeto remoto disponível no servidor usando um endereço RMI.
     *
     * @param remote objeto remoto que será disponibilizado
     * @param address endereço do objeto remoto
     * @throws RemoteException se houver um erro na operação remota
     * @throws UnknownHostException se o host não for encontrado
     * @throws MalformedURLException se o endereço RMI for inválido
     */
    public static void startRemoteObject(Remote remote, String address)
            throws RemoteException, UnknownHostException, MalformedURLException {
        // Extrai a porta do endereço RMI
        String port = address.substring(address.indexOf(":") + 1, address.lastIndexOf("/"));
        // Cria o registro da porta
        LocateRegistry.createRegistry(Integer.parseInt(port));
        // Faz o rebind do objeto remoto no endereço especificado
        Naming.rebind(address, remote);
        System.out.println("remote Object " + address + " avaiable.");
    }

    /**
     * Obtém o nome do servidor a partir de um endereço RMI.
     *
     * @param address endereço RMI no formato //server:port/object
     * @return o nome do servidor
     */
    public static String getAdressServer(String address) {
        return address.substring(address.indexOf("//") + 3, address.lastIndexOf(":"));
    }

    /**
     * Obtém o nome do objeto a partir de um endereço RMI.
     *
     * @param address endereço RMI no formato //server:port/object
     * @return o nome do objeto
     */
    public static String getAdressObjectName(String address) {
        return address.substring(address.lastIndexOf("/") + 1, address.length());
    }

    /**
     * Obtém a porta a partir de um endereço RMI.
     *
     * @param address endereço RMI no formato //server:port/object
     * @return a porta do servidor
     */
    public static int getAdressPort(String address) {
        return Integer.parseInt(address.substring(address.indexOf(":") + 1, address.lastIndexOf("/")));
    }

    /**
     * Obtém um objeto remoto a partir de um endereço RMI.
     *
     * @param host nome do host
     * @param port número da porta em que o servidor RMI está ouvindo
     * @param objectName nome do objeto remoto
     * @return o objeto remoto
     * @throws NotBoundException se o objeto não estiver vinculado
     * @throws MalformedURLException se o endereço for inválido
     * @throws RemoteException se ocorrer um erro remoto
     */
    public static Remote getRemote(String host, int port, String objectName)
            throws NotBoundException, MalformedURLException, RemoteException {
        // Realiza a busca do objeto remoto no servidor
        return Naming.lookup(getRemoteName(host, port, objectName));
    }

    /**
     * Obtém um objeto remoto a partir de um endereço RMI completo.
     *
     * @param address endereço RMI no formato //server:port/object
     * @return o objeto remoto
     * @throws NotBoundException se o objeto não estiver vinculado
     * @throws MalformedURLException se o endereço for inválido
     * @throws RemoteException se ocorrer um erro remoto
     */
    public static Remote getRemote(String address)
            throws NotBoundException, MalformedURLException, RemoteException {
        // Realiza a busca do objeto remoto no servidor usando o endereço completo
        return Naming.lookup(address);
    }

    /**
     * Remove um objeto remoto do servidor.
     *
     * @param remote objeto remoto a ser removido
     * @param port número da porta em que o servidor RMI está ouvindo
     * @param objectName nome do objeto remoto
     * @throws RemoteException se ocorrer um erro remoto
     * @throws UnknownHostException se o host não for encontrado
     */
    public static void stopRemoteObject(Remote remote, int port, String objectName)
            throws RemoteException, UnknownHostException {
        // Obtém o endereço do objeto remoto
        String address = getRemoteName(port, objectName);
        // Remove o objeto remoto do servidor
        UnicastRemoteObject.unexportObject(remote, true);
        System.out.println("remote Object :" + address + " NOT avaiable ");
    }

}
