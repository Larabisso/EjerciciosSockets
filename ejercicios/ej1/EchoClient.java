/*
 * Guía de Ejercitación - Sockets TCP en Java
 * Ejercicio 1 (Consigna): Cliente de Eco
 * Alumna: Lara Bisso - Curso 6°14
 */
package ejercicio1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Cliente que se conecta a localhost:8080, toma líneas del teclado (System.in),
 * las envía al servidor y muestra el eco que recibe.
 */
public class EchoClient {

    private static final String HOST = "localhost";
    private static final int PUERTO = 8080;

    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8);

        try (Socket socket = new Socket(HOST, PUERTO);
             BufferedReader desdeServidor = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter haciaServidor = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("Conectada a " + HOST + ":" + PUERTO
                    + " (puerto local efimero: " + socket.getLocalPort() + ")");
            System.out.println("Escribi un mensaje. Para terminar escribi CHAO.");

            boolean seguir = true;
            while (seguir && teclado.hasNextLine()) {
                System.out.print("Vos > ");
                String mensaje = teclado.nextLine();

                haciaServidor.println(mensaje);              // println agrega el \n que espera readLine()
                String respuesta = desdeServidor.readLine();

                if (respuesta == null) {
                    System.out.println("El servidor cerro la conexion.");
                    seguir = false;
                } else {
                    System.out.println("Servidor > " + respuesta);
                    seguir = !mensaje.trim().equalsIgnoreCase("CHAO");
                }
            }
        } catch (ConnectException e) {
            System.err.println("No se pudo conectar. ¿Esta corriendo el EchoServer?");
        } catch (IOException e) {
            System.err.println("Error de comunicacion: " + e.getMessage());
        }
    }
}
