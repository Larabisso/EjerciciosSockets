/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 2: Servidor Multihilo y Concurrencia
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * El hilo principal funciona como "acceptor": solo acepta conexiones y
 * le pasa cada socket a un hilo del pool para que lo atienda.
 * Así el servidor no se bloquea atendiendo a un solo cliente.
 */
public class MultiClientServer {

    private static final int PUERTO = 8080;
    private static final int TAMANIO_POOL = 10;

    private static final ExecutorService pool = Executors.newFixedThreadPool(TAMANIO_POOL);
    private static final AtomicInteger contadorClientes = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("Servidor Multihilo listo en el puerto " + PUERTO
                + " | hilos disponibles: " + TAMANIO_POOL);

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socket = servidor.accept();
                int numero = contadorClientes.incrementAndGet();
                pool.execute(new ClientHandler(socket, numero));
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
