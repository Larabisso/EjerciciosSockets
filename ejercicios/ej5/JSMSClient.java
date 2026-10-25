/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 5: Cliente CLI con recepción asíncrona
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Usa dos hilos:
 *  - Hilo principal: lee lo que escribe la persona (System.in) y lo envía.
 *  - Hilo ServerListener: escucha todo el tiempo al servidor y muestra lo que llega,
 *    así los mensajes de otros usuarios aparecen aunque no estés escribiendo.
 *
 * Ejecución: java -cp out ejercicio5.JSMSClient [host] [puerto]
 */
public class JSMSClient {

    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : "localhost";
        int puerto = args.length >= 2 ? Integer.parseInt(args[1]) : 8080;

        try (Socket socket = new Socket(host, puerto);
             PrintWriter haciaServidor = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            BufferedReader desdeServidor = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            ServerListener listener = new ServerListener(desdeServidor);
            Thread hiloListener = new Thread(listener, "server-listener");
            hiloListener.setDaemon(true);
            hiloListener.start();

            Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8);
            while (listener.estaActivo() && teclado.hasNextLine()) {
                String linea = teclado.nextLine();
                if (linea.isBlank()) {
                    continue;
                }
                haciaServidor.println(linea);
                if (linea.trim().equalsIgnoreCase("QUIT")) {
                    hiloListener.join(2000);   // esperamos el BYE antes de cerrar
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("No se pudo conectar con " + host + ":" + puerto + " -> " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Cliente finalizado.");
    }

    /** Hilo dedicado a recibir mensajes del servidor. */
    private static class ServerListener implements Runnable {
        private final BufferedReader entrada;
        private volatile boolean activo = true;

        ServerListener(BufferedReader entrada) {
            this.entrada = entrada;
        }

        boolean estaActivo() {
            return activo;
        }

        @Override
        public void run() {
            try {
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    System.out.println("  " + linea);
                    if (linea.startsWith("BYE") || linea.startsWith("SHUTDOWN") || linea.startsWith("408")) {
                        break;
                    }
                }
            } catch (IOException e) {
                // el socket se cerró
            }
            activo = false;
            System.out.println("[Conexion con el servidor terminada. Presiona Enter para salir]");
        }
    }
}
