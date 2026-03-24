package com.restaurant.ui;

import com.restaurant.dto.PopularDishDto;
import com.restaurant.dto.SalesReportRowDto;
import com.restaurant.dto.TableStatusDto;
import com.restaurant.entity.Restaurant;
import com.restaurant.service.ReportService;
import com.restaurant.service.RestaurantService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportsController implements Initializable {

    private final ReportService     reportService;
    private final RestaurantService restaurantService;

    // ---- Sales report ----
    @FXML private TextField      fldFrom;
    @FXML private TextField      fldTo;
    @FXML private TableView<SalesReportRowDto> salesTable;
    @FXML private TableColumn<SalesReportRowDto, String> srRestaurant;
    @FXML private TableColumn<SalesReportRowDto, String> srDate;
    @FXML private TableColumn<SalesReportRowDto, Number> srOrders;
    @FXML private TableColumn<SalesReportRowDto, Number> srItems;
    @FXML private TableColumn<SalesReportRowDto, String> srRevenue;
    @FXML private Label          lblTotalRevenue;

    // ---- Top dishes ----
    @FXML private TextField      fldTopN;
    @FXML private TableView<PopularDishDto> topDishesTable;
    @FXML private TableColumn<PopularDishDto, Number> tdRank;
    @FXML private TableColumn<PopularDishDto, String> tdName;
    @FXML private TableColumn<PopularDishDto, String> tdCategory;
    @FXML private TableColumn<PopularDishDto, String> tdPrice;
    @FXML private TableColumn<PopularDishDto, Number> tdOrdered;
    @FXML private TableColumn<PopularDishDto, String> tdRevenue;

    // ---- Table occupancy ----
    @FXML private ComboBox<Restaurant>        cbFreeTabRestaurant;
    @FXML private TextField                   fldFreeFrom;
    @FXML private TextField                   fldFreeTo;
    @FXML private TableView<TableStatusDto>   freeTablesTable;
    @FXML private TableColumn<TableStatusDto, Number> ftNum;
    @FXML private TableColumn<TableStatusDto, Number> ftCap;
    @FXML private TableColumn<TableStatusDto, String> ftRest;
    @FXML private TableColumn<TableStatusDto, String> ftStatus;

    @FXML private Label reportErrorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Sales
        srRestaurant.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRestaurant()));
        srDate      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSaleDate().toString()));
        srOrders    .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getOrdersCount().intValue()));
        srItems     .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getItemsSold().intValue()));
        srRevenue   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTotalRevenue().toPlainString()));

        // Top dishes — динамічний номер рядка
        tdRank    .setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });
        tdName    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        tdCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        tdPrice   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrice().toPlainString()));
        tdOrdered .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getTimesOrdered().intValue()));
        tdRevenue .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTotalRevenue().toPlainString()));

        // Table occupancy — комбобокс
        List<Restaurant> restaurants = restaurantService.findAll();
        cbFreeTabRestaurant.setItems(FXCollections.observableArrayList(restaurants));
        cbFreeTabRestaurant.setCellFactory(lv -> restCell());
        cbFreeTabRestaurant.setButtonCell(restCell());
        if (!restaurants.isEmpty()) cbFreeTabRestaurant.setValue(restaurants.get(0));

        ftNum .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().table().getTableNumber()));
        ftCap .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().table().getCapacity()));
        ftRest.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().table().getRestaurant().getName()));

        // Стовпець "Статус" — Circle-індикатор (не перефарбовується при виділенні рядка)
        ftStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().occupied() ? "Зайнято" : "Вільно"));
        ftStatus.setCellFactory(col -> new TableCell<>() {
            private final Circle dot = new Circle(6);
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setStyle(""); // скидаємо будь-які залишкові стилі при переробці клітинки
                if (empty || s == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean occupied = "Зайнято".equals(s);
                    dot.setFill(occupied ? Color.valueOf("#e05252") : Color.valueOf("#27ae60"));
                    setGraphic(dot);
                    setText("  " + s);
                    setStyle("-fx-font-weight: bold;");
                }
            }
        });

        // Дефолтні дати
        fldFrom.setText(LocalDate.now().withDayOfMonth(1).toString());
        fldTo  .setText(LocalDate.now().toString());

        // Дефолтний інтервал для столиків
        fldFreeFrom.setText(LocalDateTime.now().withSecond(0).withNano(0).toString().replace("T", " "));
        fldFreeTo  .setText(LocalDateTime.now().plusHours(2).withSecond(0).withNano(0).toString().replace("T", " "));
    }

    // ---- Sales report -------------------------------------------------------

    @FXML
    private void handleSalesReport() {
        reportErrorLabel.setText("");
        try {
            LocalDate from = LocalDate.parse(fldFrom.getText().trim());
            LocalDate to   = LocalDate.parse(fldTo.getText().trim());
            if (to.isBefore(from)) {
                reportErrorLabel.setText("Кінцева дата не може бути раніше початкової.");
                return;
            }
            List<SalesReportRowDto> rows = reportService.getSalesReport(from, to);
            salesTable.setItems(FXCollections.observableArrayList(rows));

            BigDecimal total = rows.stream()
                    .map(SalesReportRowDto::getTotalRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotalRevenue.setText(total.toPlainString() + " грн");

        } catch (DateTimeParseException e) {
            reportErrorLabel.setText("Невірний формат дати. Приклад: 2026-01-01");
        } catch (Exception e) {
            log.error("Помилка звіту про продажі", e);
            reportErrorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    // ---- Top dishes ---------------------------------------------------------

    @FXML
    private void handleTopDishes() {
        reportErrorLabel.setText("");
        try {
            int n = Integer.parseInt(fldTopN.getText().trim());
            if (n <= 0) throw new NumberFormatException();
            topDishesTable.setItems(FXCollections.observableArrayList(reportService.getTopDishes(n)));
        } catch (NumberFormatException e) {
            reportErrorLabel.setText("Вкажіть позитивне ціле число.");
        } catch (Exception e) {
            log.error("Помилка топ-страв", e);
            reportErrorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    // ---- Table occupancy: зараз ---------------------------------------------

    @FXML
    private void handleTablesNow() {
        reportErrorLabel.setText("");
        Restaurant r = cbFreeTabRestaurant.getValue();
        if (r == null) { reportErrorLabel.setText("Оберіть заклад."); return; }
        try {
            List<TableStatusDto> rows = reportService.getTableOccupancy(r.getId());
            freeTablesTable.setItems(FXCollections.observableArrayList(rows));
            long free = rows.stream().filter(t -> !t.occupied()).count();
            long busy = rows.size() - free;
            reportErrorLabel.setStyle("-fx-text-fill: #27ae60;");
            reportErrorLabel.setText("Зараз: зайнятих " + busy + ", вільних " + free);
        } catch (Exception e) {
            log.error("Помилка зайнятості столиків", e);
            reportErrorLabel.setStyle("-fx-text-fill: #e05252;");
            reportErrorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    // ---- Table occupancy: за інтервалом ------------------------------------

    @FXML
    private void handleFindFreeTables() {
        reportErrorLabel.setText("");
        reportErrorLabel.setStyle("");
        Restaurant r = cbFreeTabRestaurant.getValue();
        if (r == null) { reportErrorLabel.setText("Оберіть заклад."); return; }
        try {
            LocalDateTime from = LocalDateTime.parse(fldFreeFrom.getText().trim().replace(" ", "T"));
            LocalDateTime to   = LocalDateTime.parse(fldFreeTo  .getText().trim().replace(" ", "T"));
            if (to.isBefore(from)) {
                reportErrorLabel.setText("Кінцева дата не може бути раніше початкової.");
                return;
            }
            List<TableStatusDto> rows = reportService.getTableOccupancyInPeriod(r.getId(), from, to);
            freeTablesTable.setItems(FXCollections.observableArrayList(rows));
            long free = rows.stream().filter(t -> !t.occupied()).count();
            long busy = rows.size() - free;
            reportErrorLabel.setStyle("-fx-text-fill: #27ae60;");
            reportErrorLabel.setText("За інтервалом: зайнятих " + busy + ", вільних " + free);
        } catch (DateTimeParseException e) {
            reportErrorLabel.setText("Формат: рррр-мм-дд ГГ:хх  — напр. 2026-03-04 18:00");
        } catch (Exception e) {
            log.error("Помилка пошуку зайнятості за інтервалом", e);
            reportErrorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    // ---- Helper -------------------------------------------------------------

    /**
     * Викликається іззовні (напр. після звільнення столика) —
     * оновлює таблицю "Зайнятість" якщо вже обрано заклад і дані вже відображались.
     */
    public void refreshOccupancyNow() {
        if (cbFreeTabRestaurant.getValue() != null
                && !freeTablesTable.getItems().isEmpty()) {
            handleTablesNow();
        }
    }

    private ListCell<Restaurant> restCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "" : r.getName());
            }
        };
    }
}
