/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 3: Parser del protocolo de aplicación
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio3;

/**
 * Convierte una línea de texto recibida por la red en un objeto Command.
 *
 * Sintaxis aceptada:
 *   CONNECT <username>   -> username: letras, números, punto o guion bajo (3 a 16)
 *   JOIN <nombre_sala>   -> una sola palabra
 *   MSG <texto>          -> el texto puede tener espacios
 *   QUIT                 -> sin argumentos
 *
 * Si algo no cumple la sintaxis lanza ProtocolException con un mensaje descriptivo.
 */
public class ProtocolParser {

    public Command parsear(String linea) throws ProtocolException {
        if (linea == null || linea.isBlank()) {
            throw new ProtocolException("No se recibio ningun comando");
        }

        String limpia = linea.strip();
        int espacio = limpia.indexOf(' ');

        // Separamos "comando" de "resto" en el primer espacio
        String palabraComando = (espacio == -1) ? limpia : limpia.substring(0, espacio);
        String resto = (espacio == -1) ? "" : limpia.substring(espacio + 1).strip();

        Command.Tipo tipo;
        try {
            tipo = Command.Tipo.valueOf(palabraComando.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("Comando desconocido '" + palabraComando
                    + "'. Validos: CONNECT, JOIN, MSG, QUIT");
        }

        switch (tipo) {
            case CONNECT:
                validarNoVacio(resto, "CONNECT <username>");
                if (!resto.matches("[\\w.]{3,16}")) {
                    throw new ProtocolException("Nombre de usuario invalido. Usa de 3 a 16 caracteres "
                            + "(letras, numeros, punto o guion bajo) y sin espacios");
                }
                return new Command(tipo, resto);

            case JOIN:
                validarNoVacio(resto, "JOIN <nombre_sala>");
                if (resto.contains(" ")) {
                    throw new ProtocolException("El nombre de la sala no puede tener espacios");
                }
                return new Command(tipo, normalizarSala(resto));

            case MSG:
                validarNoVacio(resto, "MSG <texto>");
                return new Command(tipo, resto);

            case QUIT:
                if (!resto.isEmpty()) {
                    throw new ProtocolException("QUIT no recibe argumentos");
                }
                return new Command(tipo, null);

            default:
                throw new ProtocolException("Comando no soportado");
        }
    }

    private void validarNoVacio(String argumento, String usoCorrecto) throws ProtocolException {
        if (argumento.isEmpty()) {
            throw new ProtocolException("Falta el argumento. Uso correcto: " + usoCorrecto);
        }
    }

    /** "General", "general" y "#general" se toman como la misma sala: "#general". */
    private String normalizarSala(String sala) {
        String sinNumeral = sala.startsWith("#") ? sala.substring(1) : sala;
        return "#" + sinNumeral.toLowerCase();
    }
}
