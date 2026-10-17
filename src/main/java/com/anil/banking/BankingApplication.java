package com.anil.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BankingApplication {

    public static void main(String[] args) {
        useCurrentTimeZoneName();
        SpringApplication.run(BankingApplication.class, args);
    }

    /**
     * Windows still reports India's time zone by its old name "Asia/Calcutta". Newer PostgreSQL
     * images only know the current name "Asia/Kolkata" and reject the connection, so swap it
     * before the database connection is opened.
     */
    private static void useCurrentTimeZoneName() {
        if ("Asia/Calcutta".equals(TimeZone.getDefault().getID())) {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        }
    }
}
