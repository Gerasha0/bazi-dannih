package com.restaurant;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входу програми.
 * Запускає JavaFX Application, яка всередині ініціалізує Spring Boot контекст.
 */
@SpringBootApplication
public class RestaurantApplication {

    public static void main(String[] args) {
        // Делегуємо запуск JavaFX — воно правильно налаштує JavaFX потоки
        Application.launch(JavaFxApp.class, args);
    }
}
