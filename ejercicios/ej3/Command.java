/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 3: Objeto Command
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio3;

/**
 * Representa un comando del protocolo ya analizado:
 * qué acción es (tipo) y con qué dato (argumento).
 */
public class Command {

    /** Comandos que define el protocolo JSMS. */
    public enum Tipo { CONNECT, JOIN, MSG, QUIT }

    private final Tipo tipo;
    private final String argumento;

    public Command(Tipo tipo, String argumento) {
        this.tipo = tipo;
        this.argumento = argumento;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getArgumento() {
        return argumento;
    }

    @Override
    public String toString() {
        return argumento == null ? tipo.name() : tipo.name() + " [" + argumento + "]";
    }
}
