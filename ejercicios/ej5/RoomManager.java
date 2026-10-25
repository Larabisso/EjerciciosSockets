/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 5: Gestor de salas y usuarios (versión final del sistema JSMS)
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio5;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centraliza todo el estado compartido del servidor:
 *  - usuariosConectados: nombre -> handler (garantiza nombres únicos)
 *  - salas:              nombre de sala -> handlers suscriptos
 *
 * Ningún método usa "synchronized": la seguridad entre hilos la dan
 * las operaciones atómicas de ConcurrentHashMap y CopyOnWriteArrayList.
 */
public class RoomManager {

    private final Map<String, ClientHandler> usuariosConectados = new ConcurrentHashMap<>();
    private final Map<String, List<ClientHandler>> salas = new ConcurrentHashMap<>();

    /* ===================== USUARIOS ===================== */

    /** @return true si el nombre estaba libre y quedó registrado (putIfAbsent es atómico). */
    public boolean registrarUsuario(String nombre, ClientHandler cliente) {
        return usuariosConectados.putIfAbsent(nombre, cliente) == null;
    }

    public void eliminarUsuario(String nombre, ClientHandler cliente) {
        if (nombre != null) {
            usuariosConectados.remove(nombre, cliente);
        }
    }

    public Collection<ClientHandler> todosLosClientes() {
        return usuariosConectados.values();
    }

    /* ======================= SALAS ======================= */

    public void unirseASala(String nombreSala, ClientHandler cliente) {
        salas.computeIfAbsent(nombreSala, clave -> new CopyOnWriteArrayList<>()).add(cliente);
    }

    public void salirDeSala(String nombreSala, ClientHandler cliente) {
        if (nombreSala == null) {
            return;
        }
        salas.computeIfPresent(nombreSala, (clave, miembros) -> {
            miembros.remove(cliente);
            return miembros.isEmpty() ? null : miembros;   // sala vacía = se elimina
        });
    }

    public int cantidadEnSala(String nombreSala) {
        List<ClientHandler> miembros = salas.get(nombreSala);
        return miembros == null ? 0 : miembros.size();
    }

    public void broadcast(String nombreSala, String mensaje, ClientHandler emisor) {
        List<ClientHandler> miembros = salas.get(nombreSala);
        if (miembros == null) {
            return;
        }
        for (ClientHandler miembro : miembros) {
            if (miembro != emisor) {
                miembro.enviarMensajeDirecto(mensaje);
            }
        }
    }
}
