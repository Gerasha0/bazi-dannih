package com.restaurant.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class MainController implements Initializable {

    private final ApplicationContext  appContext;
    private final OrdersController    ordersController;
    private final EmployeesController employeesController;
    private final ReservationsController reservationsController;

    @FXML private StackPane    contentPane;
    @FXML private Label        headerTitle;
    @FXML private Label        statusLabel;

    @FXML private ToggleButton btnRestaurants;
    @FXML private ToggleButton btnMenu;
    @FXML private ToggleButton btnOrders;
    @FXML private ToggleButton btnReservations;
    @FXML private ToggleButton btnEmployees;
    @FXML private ToggleButton btnCustomers;
    @FXML private ToggleButton btnReports;

    // ordered map: button → [section title, fxml path, loaded node]
    private record Section(String title, String fxml) {}
    private final Map<ToggleButton, Section>  sectionMeta  = new LinkedHashMap<>();
    private final Map<ToggleButton, Node>     sectionNodes = new LinkedHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Register sections
        sectionMeta.put(btnRestaurants, new Section("Заклади",     "/fxml/RestaurantsView.fxml"));
        sectionMeta.put(btnMenu,        new Section("Меню",         "/fxml/MenuView.fxml"));
        sectionMeta.put(btnOrders,      new Section("Замовлення",   "/fxml/OrdersView.fxml"));
        sectionMeta.put(btnReservations, new Section("Бронювання", "/fxml/ReservationsView.fxml"));
        sectionMeta.put(btnEmployees,   new Section("Персонал",     "/fxml/EmployeesView.fxml"));
        sectionMeta.put(btnCustomers,   new Section("Клієнти",      "/fxml/CustomersView.fxml"));
        sectionMeta.put(btnReports,     new Section("Звіти",        "/fxml/ReportsView.fxml"));

        // Load all FXML sections into StackPane (hidden by default)
        sectionMeta.forEach((btn, sec) -> {
            Node node = loadPane(sec.fxml());
            node.setVisible(false);
            node.setManaged(false);
            sectionNodes.put(btn, node);
            contentPane.getChildren().add(node);
        });

        // Create toggle group for exclusive selection
        ToggleGroup navGroup = new ToggleGroup();
        sectionMeta.keySet().forEach(btn -> {
            btn.setToggleGroup(navGroup);
            btn.setOnAction(e -> {
                if (!btn.isSelected()) btn.setSelected(true); // prevent deselect
                activateSection(btn);
            });
        });

        // Refresh callbacks when certain sections become active
        btnOrders.setOnAction(e -> {
            btnOrders.setSelected(true);
            activateSection(btnOrders);
            ordersController.refreshRestaurantCombo();
        });
        btnReservations.setOnAction(e -> {
            btnReservations.setSelected(true);
            activateSection(btnReservations);
            reservationsController.refreshRestaurantCombo();
        });
        btnEmployees.setOnAction(e -> {
            btnEmployees.setSelected(true);
            activateSection(btnEmployees);
            employeesController.refreshRestaurantCombo();
        });

        // Show first section
        btnRestaurants.setSelected(true);
        activateSection(btnRestaurants);

        setStatus("Підключено до бази даних. Система готова.");
    }

    private void activateSection(ToggleButton active) {
        sectionNodes.forEach((btn, node) -> {
            boolean show = btn == active;
            node.setVisible(show);
            node.setManaged(show);
        });
        if (headerTitle != null) {
            Section sec = sectionMeta.get(active);
            if (sec != null) headerTitle.setText(sec.title());
        }
    }

    private Node loadPane(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(appContext::getBean);
            return loader.load();
        } catch (IOException e) {
            log.error("Не вдалося завантажити {}", fxmlPath, e);
            Label err = new Label("⚠  Помилка завантаження: " + fxmlPath);
            err.setStyle("-fx-text-fill: #e05252; -fx-font-size: 14;");
            return err;
        }
    }

    public void setStatus(String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) statusLabel.setText(message);
        });
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Про програму");
        alert.setHeaderText("Restaurant Manager");
        alert.setContentText(
                "Курсова робота\n" +
                "Розробка БД та ІС для мережі закладів харчування\n\n" +
                "Java 21 · Spring Boot 3 · Spring Data JPA · MySQL · JavaFX 21"
        );
        alert.showAndWait();
    }
}
