package com.restaurant.ui;

import com.restaurant.entity.Customer;
import com.restaurant.entity.Reservation;
import com.restaurant.entity.ReservationStatus;
import com.restaurant.entity.Restaurant;
import com.restaurant.entity.RestaurantTable;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.repository.RestaurantTableRepository;
import com.restaurant.service.CustomerService;
import com.restaurant.service.ReservationService;
import com.restaurant.service.RestaurantService;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationsController implements Initializable {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ReservationService        reservationService;
    private final RestaurantService         restaurantService;
    private final CustomerService           customerService;
    private final RestaurantTableRepository tableRepository;

    @FXML private ComboBox<Restaurant> cbRestaurant;
    @FXML private ComboBox<String>     cbStatus;

    @FXML private TableView<Reservation>            reservationsTable;
    @FXML private TableColumn<Reservation, Number>  colId;
    @FXML private TableColumn<Reservation, String>  colRestaurant;
    @FXML private TableColumn<Reservation, String>  colTable;
    @FXML private TableColumn<Reservation, String>  colCustomer;
    @FXML private TableColumn<Reservation, String>  colFrom;
    @FXML private TableColumn<Reservation, String>  colTo;
    @FXML private TableColumn<Reservation, Number>  colParty;
    @FXML private TableColumn<Reservation, String>  colStatus;
    @FXML private TableColumn<Reservation, String>  colNotes;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Фільтри
        cbRestaurant.getItems().add(null);
        cbRestaurant.getItems().addAll(restaurantService.findAll());
        cbRestaurant.setCellFactory(lv -> restCell());
        cbRestaurant.setButtonCell(restCell());

        cbStatus.getItems().add("Всі");
        for (ReservationStatus s : ReservationStatus.values()) {
            cbStatus.getItems().add(s.getDisplayName());
        }
        cbStatus.setValue("Всі");

        // Колонки
        colId        .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()));
        colRestaurant.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getRestaurant().getName()));
        colTable     .setCellValueFactory(c -> new SimpleStringProperty(
                "Стіл " + c.getValue().getTable().getTableNumber()
                + " (" + c.getValue().getTable().getCapacity() + " м.)"));
        colCustomer  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCustomer() != null
                        ? c.getValue().getCustomer().getFullName() : "—"));
        colFrom      .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getReservationTime().format(DT_FMT)));
        colTo        .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEndTime().format(DT_FMT)));
        colParty     .setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getPartySize()));
        colStatus    .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus().getDisplayName()));
        colNotes     .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getNotes() != null ? c.getValue().getNotes() : ""));

        // Підсвітка рядків по статусу
        reservationsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Reservation item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-res-confirmed", "row-res-cancelled", "row-res-completed");
                if (!empty && item != null) {
                    switch (item.getStatus()) {
                        case CONFIRMED -> getStyleClass().add("row-res-confirmed");
                        case CANCELLED -> getStyleClass().add("row-res-cancelled");
                        case COMPLETED -> getStyleClass().add("row-res-completed");
                        default -> {}
                    }
                }
            }
        });

        refresh();
    }

    /** Викликається при переході на вкладку — оновлює список закладів у фільтрі. */
    public void refreshRestaurantCombo() {
        Integer selId = cbRestaurant.getValue() != null ? cbRestaurant.getValue().getId() : null;
        cbRestaurant.getItems().clear();
        cbRestaurant.getItems().add(null);
        List<Restaurant> list = restaurantService.findAll();
        cbRestaurant.getItems().addAll(list);
        if (selId != null) {
            list.stream().filter(r -> r.getId().equals(selId)).findFirst()
                    .ifPresent(cbRestaurant::setValue);
        }
        refresh();
    }

    @FXML private void handleFilter()  { refresh(); }
    @FXML private void handleRefresh() {
        cbRestaurant.setValue(null);
        cbStatus.setValue("Всі");
        refresh();
    }

    private void refresh() {
        List<Reservation> all = reservationService.findAll();
        Restaurant r = cbRestaurant.getValue();
        if (r != null) {
            all = all.stream()
                    .filter(rv -> rv.getRestaurant().getId().equals(r.getId()))
                    .toList();
        }
        String sf = cbStatus.getValue();
        if (sf != null && !"Всі".equals(sf)) {
            all = all.stream()
                    .filter(rv -> rv.getStatus().getDisplayName().equals(sf))
                    .toList();
        }
        reservationsTable.setItems(FXCollections.observableArrayList(all));
    }

    private Reservation selected() {
        return reservationsTable.getSelectionModel().getSelectedItem();
    }

    // ── Нове бронювання ────────────────────────────────────────────────────
    @FXML
    private void handleNew() {
        List<Restaurant> restaurants = restaurantService.findAll();
        if (restaurants.isEmpty()) {
            UiUtils.warn("Немає закладів", "Спочатку додайте заклад.");
            return;
        }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Нове бронювання");
        dlg.setHeaderText("Заповніть дані бронювання:");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Restaurant>      cbR    = new ComboBox<>();
        ComboBox<RestaurantTable> cbT    = new ComboBox<>();
        ComboBox<Customer>        cbC    = new ComboBox<>();
        TextField fldFrom  = new TextField();
        TextField fldTo    = new TextField();
        TextField fldParty = new TextField("2");
        TextField fldNotes = new TextField();
        Label     errLbl   = new Label();

        fldFrom.setPromptText("дд.мм.рррр гг:хх");
        fldTo.setPromptText("дд.мм.рррр гг:хх");
        errLbl.setStyle("-fx-text-fill:red;");

        cbR.setItems(FXCollections.observableArrayList(restaurants));
        cbR.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Restaurant r2, boolean empty) {
                super.updateItem(r2, empty);
                setText(empty || r2 == null ? "" : r2.getName());
            }
        });
        cbR.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Restaurant r2, boolean empty) {
                super.updateItem(r2, empty);
                setText(empty || r2 == null ? "— Оберіть заклад —" : r2.getName());
            }
        });
        cbR.setPrefWidth(220);

        cbT.setPromptText("— Оберіть стіл —"); cbT.setPrefWidth(220);
        cbT.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(RestaurantTable t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "" : t.toString());
            }
        });
        cbT.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(RestaurantTable t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "— Оберіть стіл —" : t.toString());
            }
        });

        cbC.setItems(FXCollections.observableArrayList(customerService.findAll()));
        cbC.setPromptText("— Без клієнта —"); cbC.setPrefWidth(220);
        cbC.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Customer c2, boolean empty) {
                super.updateItem(c2, empty);
                setText(empty || c2 == null ? "" : c2.getFullName());
            }
        });
        cbC.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Customer c2, boolean empty) {
                super.updateItem(c2, empty);
                setText(empty || c2 == null ? "— Без клієнта —" : c2.getFullName());
            }
        });

        // При виборі закладу підгружаємо його столики
        cbR.valueProperty().addListener((obs, oldR, newR) -> {
            cbT.getItems().clear(); cbT.setValue(null);
            if (newR != null) {
                cbT.setItems(FXCollections.observableArrayList(
                        tableRepository.findByRestaurantIdOrderByTableNumber(newR.getId())));
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Заклад *:"),      0, 0); grid.add(cbR,     1, 0);
        grid.add(new Label("Стіл *:"),        0, 1); grid.add(cbT,     1, 1);
        grid.add(new Label("Клієнт:"),        0, 2); grid.add(cbC,     1, 2);
        grid.add(new Label("Від * (дд.мм.рррр гг:хх):"), 0, 3); grid.add(fldFrom,  1, 3);
        grid.add(new Label("До *  (дд.мм.рррр гг:хх):"), 0, 4); grid.add(fldTo,    1, 4);
        grid.add(new Label("К-сть гостей:"),  0, 5); grid.add(fldParty, 1, 5);
        grid.add(new Label("Нотатки:"),       0, 6); grid.add(fldNotes, 1, 6);
        grid.add(errLbl,                      0, 7, 2, 1);
        dlg.getDialogPane().setContent(grid);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            if (cbR.getValue() == null) {
                errLbl.setText("Оберіть заклад!"); evt.consume(); return;
            }
            if (cbT.getValue() == null) {
                errLbl.setText("Оберіть стіл!"); evt.consume(); return;
            }
            if (fldFrom.getText().isBlank() || fldTo.getText().isBlank()) {
                errLbl.setText("Вкажіть час 'Від' та 'До'!"); evt.consume(); return;
            }
            try {
                LocalDateTime.parse(fldFrom.getText().trim(), DT_FMT);
                LocalDateTime.parse(fldTo.getText().trim(), DT_FMT);
            } catch (DateTimeParseException e) {
                errLbl.setText("Формат: дд.мм.рррр гг:хх (напр. 22.03.2026 18:00)");
                evt.consume();
            }
        });

        dlg.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                LocalDateTime from = LocalDateTime.parse(fldFrom.getText().trim(), DT_FMT);
                LocalDateTime to   = LocalDateTime.parse(fldTo.getText().trim(), DT_FMT);
                int party = 2;
                try { party = Integer.parseInt(fldParty.getText().trim()); }
                catch (NumberFormatException ignore) {}
                Integer cId = cbC.getValue() != null ? cbC.getValue().getId() : null;
                reservationService.createReservation(
                        cbR.getValue().getId(), cbT.getValue().getId(),
                        cId, from, to, party,
                        fldNotes.getText().trim().isEmpty() ? null : fldNotes.getText().trim());
                refresh();
                UiUtils.info("Бронювання створено",
                        "Стіл " + cbT.getValue().getTableNumber() + " заброньовано з "
                        + from.format(DT_FMT) + " до " + to.format(DT_FMT));
            } catch (RestaurantAppException e) {
                UiUtils.error("Помилка", e.getMessage());
            }
        });
    }

    // ── Скасувати ──────────────────────────────────────────────────────────
    @FXML
    private void handleCancel() {
        Reservation r = selected();
        if (r == null) {
            UiUtils.warn("Нічого не вибрано", "Оберіть бронювання у таблиці.");
            return;
        }
        if (r.getStatus() == ReservationStatus.CANCELLED) {
            UiUtils.warn("Вже скасовано", "Це бронювання вже має статус «Скасовано».");
            return;
        }
        UiUtils.confirm("Скасування", "Скасувати бронювання #" + r.getId() + "?")
                .filter(bt -> bt == ButtonType.OK)
                .ifPresent(bt -> {
                    try { reservationService.cancel(r.getId()); refresh(); }
                    catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
                });
    }

    // ── Завершити ──────────────────────────────────────────────────────────
    @FXML
    private void handleComplete() {
        Reservation r = selected();
        if (r == null) {
            UiUtils.warn("Нічого не вибрано", "Оберіть бронювання у таблиці.");
            return;
        }
        if (r.getStatus() == ReservationStatus.COMPLETED) {
            UiUtils.warn("Вже завершено", "Це бронювання вже має статус «Завершено».");
            return;
        }
        UiUtils.confirm("Завершення", "Позначити бронювання #" + r.getId() + " як завершене?")
                .filter(bt -> bt == ButtonType.OK)
                .ifPresent(bt -> {
                    try { reservationService.complete(r.getId()); refresh(); }
                    catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
                });
    }

    // ── Видалити ───────────────────────────────────────────────────────────
    @FXML
    private void handleDelete() {
        Reservation r = selected();
        if (r == null) {
            UiUtils.warn("Нічого не вибрано", "Оберіть бронювання у таблиці.");
            return;
        }
        UiUtils.confirm("Видалення", "Видалити бронювання #" + r.getId() + "?")
                .filter(bt -> bt == ButtonType.OK)
                .ifPresent(bt -> {
                    try { reservationService.delete(r.getId()); refresh(); }
                    catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
                });
    }

    // ── Допоміжне ─────────────────────────────────────────────────────────
    private ListCell<Restaurant> restCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "— Всі заклади —" : r.getName());
            }
        };
    }
}
