/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 5: Servidor JSMS completo con apagado limpio
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio5;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Java Socket Messaging System - servidor final.
 *
 * Reúne todo lo de la guía: pool de hilos (Ej. 2), protocolo (Ej. 3),
 * salas con broadcast (Ej. 4) y resiliencia + Shutdown Hook (Ej. 5).
 *
 * Ejecución: java -cp out ejercicio5.JSMSServer
 * Timeout opcional para probar: java -Djsms.timeout=5000 -cp out ejercicio5.JSMSServer
 */
public class JSMSServer {

    private static final int PUERTO = 8080;
    private static final int HILOS = 25;
    private static final int TIMEOUT_MS = Integer.getInteger("jsms.timeout", 30000);

    private final RoomManager roomManager = new RoomManager();
    private final ExecutorService pool = Executors.newFixedThreadPool(HILOS);
    /** Todas las conexiones abiertas, incluso las que todavía no hicieron CONNECT. */
    private final Set<ClientHandler> conexiones = ConcurrentHashMap.newKeySet();

    private ServerSocket serverSocket;
    private volatile boolean enEjecucion = true;

    public static void main(String[] args) {
        new JSMSServer().arrancar();
    }

    public void arrancar() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::apagar, "apagado-jsms"));

        try {
            serverSocket = new ServerSocket(PUERTO);
            System.out.println("==============================================");
            System.out.println(" JSMS - Java Socket Messaging System");
            System.out.println(" Puerto: " + PUERTO + " | Hilos: " + HILOS + " | Timeout: " + TIMEOUT_MS + " ms");
            System.out.println(" Ctrl+C para apagar de forma ordenada");
            System.out.println("==============================================");

            while (enEjecucion) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, roomManager, TIMEOUT_MS);
                conexiones.add(handler);
                pool.execute(() -> {
                    try {
                        handler.run();
                    } finally {
                        conexiones.remove(handler);
                    }
                });
            }
        } catch (SocketException e) {
            // Si estamos apagando, accept() se interrumpe porque cerramos el ServerSocket: es normal
            if (enEjecucion) {
                System.err.println("Error en el socket del servidor: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }

    /** Graceful Shutdown: avisa a los clientes, deja de aceptar y espera a los hilos. */
    private void apagar() {
        enEjecucion = false;
        System.out.println("\nApagando servidor JSMS...");

        for (ClientHandler c : conexiones) {
            c.enviarMensajeDirecto("SHUTDOWN El servidor se esta apagando. Gracias por usar JSMS!");
        }
        System.out.println("Se notifico a " + conexiones.size() + " cliente(s).");

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignorada) {
            // nada para hacer
        }

        for (ClientHandler c : conexiones) {
            c.cerrar();   // desbloquea el readLine() de cada hilo
        }

        pool.shutdown();
        try {
            if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Servidor detenido.");
    }
}
