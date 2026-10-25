/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 5: ClientHandler robusto (timeouts, desconexiones y liberación de recursos)
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio5;

import ejercicio3.Command;
import ejercicio3.ProtocolException;
import ejercicio3.ProtocolParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Maneja la sesión completa de un cliente del sistema JSMS.
 *
 * Robustez (Ejercicio 5):
 *  - setSoTimeout: si el cliente no manda nada en TIMEOUT_MS, readLine() lanza
 *    SocketTimeoutException. La primera vez se le manda un aviso; si sigue
 *    inactivo otro período completo, se lo desconecta.
 *  - SocketException: cliente que se cayó o cerró de golpe (connection reset).
 *  - finally: SIEMPRE se lo saca de la sala y del registro de usuarios.
 */
public class ClientHandler implements Runnable {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Socket socket;
    private final RoomManager roomManager;
    private final int timeoutMs;
    private final ProtocolParser parser = new ProtocolParser();

    private volatile PrintWriter salida;
    private volatile String usuario;
    private volatile String salaActual;

    public ClientHandler(Socket socket, RoomManager roomManager, int timeoutMs) {
        this.socket = socket;
        this.roomManager = roomManager;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void run() {
        log("Nueva conexion " + socket.getRemoteSocketAddress());

        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(timeoutMs);
            socket.setKeepAlive(true);
            salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            enviarMensajeDirecto("INFO Bienvenida/o a JSMS. Comandos: CONNECT <usuario>, JOIN <sala>, MSG <texto>, QUIT");
            bucleDeLectura(entrada);

        } catch (SocketException e) {
            log("Desconexion abrupta de " + nombre() + " (" + e.getMessage() + ")");
        } catch (IOException e) {
            log("Error de I/O con " + nombre() + ": " + e.getMessage());
        } finally {
            liberarRecursos();
        }
    }

    private void bucleDeLectura(BufferedReader entrada) throws IOException {
        boolean avisoDeInactividadEnviado = false;

        while (true) {
            String linea;
            try {
                linea = entrada.readLine();
            } catch (SocketTimeoutException e) {
                if (!avisoDeInactividadEnviado) {
                    enviarMensajeDirecto("WARN Estas inactivo. Si no escribis nada se cerrara la conexion.");
                    avisoDeInactividadEnviado = true;
                    continue;                                   // le damos otro período de gracia
                }
                log("Timeout: " + nombre() + " estuvo inactivo demasiado tiempo");
                enviarMensajeDirecto("408 TIMEOUT Conexion cerrada por inactividad");
                return;
            }

            if (linea == null) {                                // EOF: el cliente cerró
                log(nombre() + " cerro la conexion");
                return;
            }
            avisoDeInactividadEnviado = false;                  // volvió a dar señales de vida

            try {
                if (!procesar(parser.parsear(linea))) {
                    return;                                     // QUIT
                }
            } catch (ProtocolException e) {
                enviarMensajeDirecto("400 ERROR " + e.getMessage());
            }
        }
    }

    /** @return false si la sesión debe terminar. */
    private boolean procesar(Command comando) {
        switch (comando.getTipo()) {
            case CONNECT:
                conectar(comando.getArgumento());
                return true;
            case JOIN:
                unirse(comando.getArgumento());
                return true;
            case MSG:
                mandarMensaje(comando.getArgumento());
                return true;
            case QUIT:
                enviarMensajeDirecto("BYE Nos vemos " + nombre() + "!");
                return false;
            default:
                return true;
        }
    }

    private void conectar(String nombreElegido) {
        if (usuario != null) {
            enviarMensajeDirecto("401 ERROR Ya estas conectada/o como " + usuario);
        } else if (roomManager.registrarUsuario(nombreElegido, this)) {
            usuario = nombreElegido;
            enviarMensajeDirecto("200 OK Hola " + usuario + "!");
            log("Usuario registrado: " + usuario);
        } else {
            enviarMensajeDirecto("401 ERROR El nombre '" + nombreElegido + "' ya esta en uso, elegi otro");
        }
    }

    private void unirse(String sala) {
        if (usuario == null) {
            enviarMensajeDirecto("401 ERROR Primero identificate con CONNECT <usuario>");
            return;
        }
        if (sala.equals(salaActual)) {
            enviarMensajeDirecto("JOINED " + sala);
            return;
        }
        if (salaActual != null) {
            roomManager.salirDeSala(salaActual, this);
            roomManager.broadcast(salaActual, "INFO " + usuario + " se fue de la sala", this);
        }
        salaActual = sala;
        roomManager.unirseASala(sala, this);
        enviarMensajeDirecto("JOINED " + sala);
        roomManager.broadcast(sala, "INFO " + usuario + " entro a la sala", this);
        log(usuario + " entro a " + sala + " (" + roomManager.cantidadEnSala(sala) + " en la sala)");
    }

    private void mandarMensaje(String texto) {
        if (usuario == null) {
            enviarMensajeDirecto("401 ERROR Primero identificate con CONNECT <usuario>");
        } else if (salaActual == null) {
            enviarMensajeDirecto("404 ERROR No estas en ninguna sala. Usa JOIN <sala>");
        } else {
            String mensaje = "MSG [" + LocalTime.now().format(HORA) + "] " + usuario + ": " + texto;
            enviarMensajeDirecto(mensaje);
            roomManager.broadcast(salaActual, mensaje, this);
        }
    }

    /**
     * Puede ser llamado por varios hilos a la vez (broadcasts de otros clientes).
     * PrintWriter ya sincroniza internamente cada println, así que dos mensajes
     * nunca se mezclan en la misma línea.
     */
    public void enviarMensajeDirecto(String mensaje) {
        PrintWriter w = salida;
        if (w != null) {
            w.println(mensaje);
        }
    }

    /** Usado por el Shutdown Hook para cortar el readLine() bloqueado de este hilo. */
    public void cerrar() {
        try {
            socket.close();
        } catch (IOException ignorada) {
            // ya estaba cerrado
        }
    }

    private void liberarRecursos() {
        if (salaActual != null) {
            roomManager.salirDeSala(salaActual, this);
            roomManager.broadcast(salaActual, "INFO " + usuario + " se desconecto", this);
        }
        roomManager.eliminarUsuario(usuario, this);
        cerrar();
        log("Recursos liberados de " + nombre());
    }

    private String nombre() {
        return usuario != null ? usuario : "(sin nombre)";
    }

    private void log(String texto) {
        System.out.println(LocalTime.now().format(HORA) + " [" + Thread.currentThread().getName() + "] " + texto);
    }
}
