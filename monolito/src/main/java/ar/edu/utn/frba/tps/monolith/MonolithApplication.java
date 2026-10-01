package ar.edu.utn.frba.tps.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MonolithApplication {

    static {
        // El driver JDBC de Postgres manda el TimeZone por defecto de la JVM como parametro
        // de conexion, y en Windows/locales de Argentina a veces resuelve a "America/Buenos_Aires"
        // (alias legacy) en vez de "America/Argentina/Buenos_Aires", que Postgres no reconoce.
        // Fijamos UTC para no depender de la config regional de cada maquina. Va en un bloque
        // estatico (no en main) para que tambien corra en los tests con @SpringBootTest, que
        // arrancan el contexto sin pasar por main().
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(MonolithApplication.class, args);
    }

}
