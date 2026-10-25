/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 3: Excepción de sintaxis del protocolo
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio3;

/**
 * Se lanza cuando una línea recibida no respeta la sintaxis del protocolo.
 * Es una excepción "chequeada" para obligar a quien parsea a manejar el error
 * (y responderle al cliente) en lugar de cortar la conexión.
 */
public class ProtocolException extends Exception {

    public ProtocolException(String mensaje) {
        super(mensaje);
    }
}
