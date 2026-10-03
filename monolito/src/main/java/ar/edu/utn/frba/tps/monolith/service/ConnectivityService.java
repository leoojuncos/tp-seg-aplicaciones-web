package ar.edu.utn.frba.tps.monolith.service;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;

@Service
public class ConnectivityService {

    private final DataSource dataSource;
    private final ConnectionFactory rabbitConnectionFactory;

    @Autowired
    public ConnectivityService(DataSource dataSource, ConnectionFactory rabbitConnectionFactory) {
        this.dataSource = dataSource;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
    }

    public boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean checkRabbitMQ() {
        try (org.springframework.amqp.rabbit.connection.Connection connection = rabbitConnectionFactory.createConnection()) {
            return connection.isOpen();
        } catch (Exception e) {
            return false;
        }
    }

}
