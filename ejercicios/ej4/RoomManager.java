/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 4: Gestor central de salas
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio4;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Estado compartido por TODOS los hilos del servidor.
 *
 * - ConcurrentHashMap: varios hilos pueden leer/escribir el mapa de salas a la vez
 *   sin bloquear la tabla completa.
 * - CopyOnWriteArrayList: cada sala es una lista de suscriptores. Se recorre muchísimo
 *   (cada broadcast) y se modifica poco (al entrar/salir), que es justo el caso ideal.
 */
public class RoomManager {

    private final Map<String, List<ClientHandler>> salas = new ConcurrentHashMap<>();

    public void unirseASala(String nombreSala, ClientHandler cliente) {
        // computeIfAbsent es atómico: si dos hilos crean la misma sala a la vez, se crea una sola
        salas.computeIfAbsent(nombreSala, clave -> new CopyOnWriteArrayList<>()).add(cliente);
    }

    public void salirDeSala(String nombreSala, ClientHandler cliente) {
        if (nombreSala == null) {
            return;
        }
        // computeIfPresent se ejecuta de forma atómica para esa clave:
        // sacamos al cliente y, si la sala quedó vacía, devolver null la borra del mapa
        salas.computeIfPresent(nombreSala, (clave, miembros) -> {
            miembros.remove(cliente);
            return miembros.isEmpty() ? null : miembros;
        });
    }

    public void broadcast(String nombreSala, String mensaje, ClientHandler emisor) {
        List<ClientHandler> miembros = salas.get(nombreSala);
        if (miembros == null) {
            return;
        }
        for (ClientHandler miembro : miembros) {
            if (miembro != emisor) {          // no le devolvemos el mensaje a quien lo mandó
                miembro.enviarMensajeDirecto(mensaje);
            }
        }
    }
}
