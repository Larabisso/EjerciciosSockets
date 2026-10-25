/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 2: Manejador de cliente (Runnable)
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Tarea que ejecuta un hilo del pool: atiende a UN cliente
 * con la lógica de eco del Ejercicio 1.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int numeroCliente;

    public ClientHandler(Socket socket, int numeroCliente) {
        this.socket = socket;
        this.numeroCliente = numeroCliente;
    }

    @Override
    public void run() {
        String hilo = Thread.currentThread().getName();
        System.out.printf("[%s] Cliente #%d conectado desde puerto %d%n",
                hilo, numeroCliente, socket.getPort());

        try (socket;
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter salida = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            salida.println("Hola cliente #" + numeroCliente + ", te atiende " + hilo);

            String linea;
            while ((linea = entrada.readLine()) != null) {
                if (linea.trim().equalsIgnoreCase("CHAO")) {
                    salida.println("ECO: CHAO - Hasta luego!");
                    break;
                }
                salida.println("ECO: " + linea);
            }
        } catch (IOException e) {
            System.err.printf("[%s] Problema con cliente #%d: %s%n", hilo, numeroCliente, e.getMessage());
        }

        System.out.printf("[%s] Cliente #%d desconectado, el hilo vuelve al pool%n", hilo, numeroCliente);
    }
}
