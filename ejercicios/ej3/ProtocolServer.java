/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 3: Servidor que aplica el protocolo (sin salas compartidas todavía)
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor multihilo (Ej. 2) que interpreta los comandos con ProtocolParser.
 * Ante un error de sintaxis responde "400 ERROR ..." y la conexión sigue abierta.
 * Se prueba con: nc localhost 8080
 */
public class ProtocolServer {

    private static final int PUERTO = 8080;
    private static final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        try (ServerSocket servidor = new ServerSocket(PUERTO)) {
            System.out.println("Servidor con protocolo (Ejercicio 3) en puerto " + PUERTO);
            while (true) {
                Socket socket = servidor.accept();
                pool.execute(new Sesion(socket));
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /** Estado de la conversación con un cliente. */
    private static class Sesion implements Runnable {
        private final Socket socket;
        private final ProtocolParser parser = new ProtocolParser();
        private String usuario;
        private String sala;

        Sesion(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (socket;
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                String linea;
                boolean activa = true;
                while (activa && (linea = in.readLine()) != null) {
                    try {
                        Command comando = parser.parsear(linea);
                        activa = ejecutar(comando, out);
                    } catch (ProtocolException e) {
                        out.println("400 ERROR " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.err.println("Cliente desconectado: " + e.getMessage());
            }
        }

        private boolean ejecutar(Command comando, PrintWriter out) {
            switch (comando.getTipo()) {
                case CONNECT:
                    if (usuario != null) {
                        out.println("401 ERROR Ya iniciaste sesion como " + usuario);
                    } else {
                        usuario = comando.getArgumento();
                        out.println("200 OK");
                    }
                    return true;
                case JOIN:
                    if (usuario == null) {
                        out.println("401 ERROR Primero identificate con CONNECT <username>");
                    } else {
                        sala = comando.getArgumento();
                        out.println("JOINED " + sala);
                    }
                    return true;
                case MSG:
                    if (usuario == null) {
                        out.println("401 ERROR Primero identificate con CONNECT <username>");
                    } else if (sala == null) {
                        out.println("404 ERROR No estas en ninguna sala. Usa JOIN <nombre_sala>");
                    } else {
                        out.println("MSG " + usuario + ": " + comando.getArgumento());
                    }
                    return true;
                case QUIT:
                    out.println("BYE");
                    return false;
                default:
                    return true;
            }
        }
    }
}
