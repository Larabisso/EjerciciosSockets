/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 1: Fundamentos de Conexión TCP y Servidor de Eco
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Servidor de eco mono-hilo: atiende a un único cliente y le devuelve
 * cada línea recibida. La sesión termina cuando el cliente envía CHAO.
 */
public class EchoServer {

    public static final int PUERTO = 8080;

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("[EchoServer] Esperando un cliente en el puerto " + PUERTO + "...");

            // accept() es bloqueante: el programa queda detenido acá hasta que alguien se conecte
            Socket conexion = servidor.accept();
            System.out.println("[EchoServer] Cliente conectado: " + conexion.getRemoteSocketAddress());

            atenderCliente(conexion);

        } catch (IOException e) {
            System.err.println("[EchoServer] Error: " + e.getMessage());
        }
        System.out.println("[EchoServer] Fin del servidor.");
    }

    private static void atenderCliente(Socket conexion) throws IOException {
        try (conexion;
             BufferedReader lector = new BufferedReader(
                     new InputStreamReader(conexion.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter escritor = new PrintWriter(
                     new OutputStreamWriter(conexion.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String recibido = lector.readLine();
            while (recibido != null) {                     // null significa que el cliente cerró
                if (recibido.trim().equalsIgnoreCase("CHAO")) {
                    escritor.println("ECO: CHAO - Hasta luego!");
                    break;
                }
                escritor.println("ECO: " + recibido);      // autoFlush activo: se envía en el acto
                recibido = lector.readLine();
            }
        }
    }
}
