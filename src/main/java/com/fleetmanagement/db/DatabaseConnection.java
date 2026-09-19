package com.fleetmanagement.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // ------------------------------------------------------------
    // Database credentials and connection details
    // ------------------------------------------------------------
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String URL = "jdbc:mysql://localhost:3306/fleet_management_v2";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Hitesh@1228";

    // ------------------------------------------------------------
    // Singleton instance
    // ------------------------------------------------------------
    private static DatabaseConnection instance;
    private Connection connection;

    // ------------------------------------------------------------
    // Private constructor to prevent external instantiation
    // ------------------------------------------------------------
    private DatabaseConnection() {
        try {
            // Load the MySQL JDBC driver
            Class.forName(DRIVER);

            // Establish the connection
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

            System.out.println("Database Connection Successful!");

        } catch (ClassNotFoundException e) {
            System.out.println("Error: MySQL JDBC Driver not found. Please check your classpath/library setup.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Error: Unable to connect to the database. Please check your URL, username, or password.");
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------
    // Public method to get the single instance of this class
    // ------------------------------------------------------------
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // ------------------------------------------------------------
    // Public method to get the active Connection object
    // ------------------------------------------------------------
    public Connection getConnection() {
        try {
            // Reconnect if the connection is null or has been closed
            if (connection == null || connection.isClosed()) {
                Class.forName(DRIVER);
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Database Connection Successful!");
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Error: MySQL JDBC Driver not found. Please check your classpath/library setup.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Error: Unable to connect to the database. Please check your URL, username, or password.");
            e.printStackTrace();
        }
        return connection;
    }

    // ------------------------------------------------------------
    // Utility method to close the connection
    // ------------------------------------------------------------
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database Connection Closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error: Unable to close the database connection.");
            e.printStackTrace();
        }
    }
}