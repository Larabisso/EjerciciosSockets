/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 4: Servidor de chat con salas y broadcast
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio4;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Igual que el servidor del Ej. 2, pero todos los ClientHandler comparten
 * UNA sola instancia de RoomManager. Se prueba abriendo dos "nc localhost 8080".
 */
public class ChatServer {

    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        RoomManager roomManager = new RoomManager();
        ExecutorService pool = Executors.newFixedThreadPool(20);

        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor de chat (Ejercicio 4) en puerto " + PUERTO);
            while (true) {
                Socket socket = servidor.accept();
                pool.execute(new ClientHandler(socket, roomManager));
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
