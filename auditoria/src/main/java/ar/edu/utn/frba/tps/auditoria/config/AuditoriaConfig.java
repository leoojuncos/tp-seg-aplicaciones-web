package ar.edu.utn.frba.tps.auditoria.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({MessagingProperties.class, AuditoriaProperties.class})
public class AuditoriaConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
