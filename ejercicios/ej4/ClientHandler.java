/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 4: ClientHandler integrado con RoomManager (broadcast)
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio4;

import ejercicio3.Command;
import ejercicio3.ProtocolException;
import ejercicio3.ProtocolParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende a un cliente. Al procesar MSG <texto> llama a RoomManager.broadcast
 * para que el mensaje llegue en tiempo real al resto de la sala.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RoomManager roomManager;
    private final ProtocolParser parser = new ProtocolParser();

    private PrintWriter salida;
    private String usuario;
    private String salaActual;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try (socket;
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String linea;
            boolean activa = true;
            while (activa && (linea = entrada.readLine()) != null) {
                try {
                    activa = ejecutar(parser.parsear(linea));
                } catch (ProtocolException e) {
                    enviarMensajeDirecto("400 ERROR " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Conexion perdida con " + usuario + ": " + e.getMessage());
        } finally {
            roomManager.salirDeSala(salaActual, this);   // nada de "mensajes fantasma"
        }
    }

    private boolean ejecutar(Command comando) {
        switch (comando.getTipo()) {
            case CONNECT:
                usuario = comando.getArgumento();
                enviarMensajeDirecto("200 OK");
                return true;

            case JOIN:
                if (usuario == null) {
                    enviarMensajeDirecto("401 ERROR Primero identificate con CONNECT <username>");
                    return true;
                }
                roomManager.salirDeSala(salaActual, this);
                salaActual = comando.getArgumento();
                roomManager.unirseASala(salaActual, this);
                enviarMensajeDirecto("JOINED " + salaActual);
                return true;

            case MSG:
                if (usuario == null || salaActual == null) {
                    enviarMensajeDirecto("401 ERROR Necesitas CONNECT y JOIN antes de mandar mensajes");
                    return true;
                }
                String mensaje = "MSG " + usuario + ": " + comando.getArgumento();
                enviarMensajeDirecto(mensaje);
                roomManager.broadcast(salaActual, mensaje, this);
                return true;

            case QUIT:
                enviarMensajeDirecto("BYE");
                return false;

            default:
                return true;
        }
    }

    /** Lo llaman otros hilos durante el broadcast. PrintWriter sincroniza cada println internamente. */
    public void enviarMensajeDirecto(String mensaje) {
        if (salida != null) {
            salida.println(mensaje);
        }
    }
}
