package com.university.decanat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Клас для управління підключенням до БД MySQL
 * Використовує Singleton патерн
 */
public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/decanat_lab3?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "decanat_user";
    private static final String PASSWORD = "decanat123";
    
    private static Connection connection = null;
    
    // Приватний конструктор (Singleton)
    private DatabaseConnection() {}
    
    /**
     * Отримати з'єднання з БД
     * @return Connection об'єкт
     * @throws SQLException якщо не вдалося підключитися
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Завантажити MySQL JDBC драйвер
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Підключення до БД встановлено успішно!");
            } catch (ClassNotFoundException e) {
                System.err.println("❌ MySQL JDBC Driver не знайдено!");
                e.printStackTrace();
                throw new SQLException("Драйвер не знайдено", e);
            } catch (SQLException e) {
                System.err.println("❌ Помилка підключення до БД!");
                e.printStackTrace();
                throw e;
            }
        }
        return connection;
    }
    
    /**
     * Закрити з'єднання з БД
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔌 З'єднання з БД закрито");
            } catch (SQLException e) {
                System.err.println("❌ Помилка закриття з'єднання");
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Перевірка з'єднання
     * @return true якщо з'єднання активне
     */
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
