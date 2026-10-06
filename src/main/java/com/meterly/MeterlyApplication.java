package com.meterly;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MeterlyApplication {

    public static void main(String[] args) {
        // Billing runs in UTC everywhere, whatever the machine's timezone. This also avoids
        // PostgreSQL rejecting legacy zone names like "Asia/Calcutta" that the JDBC driver would send.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MeterlyApplication.class, args);
    }
}
