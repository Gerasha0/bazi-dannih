package com.restaurant.ui;

import com.restaurant.entity.Restaurant;
import com.restaurant.entity.RestaurantTable;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.repository.RestaurantTableRepository;
import com.restaurant.service.RestaurantService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantsController implements Initializable {

    private final RestaurantService         restaurantService;
    private final RestaurantTableRepository tableRepository;

    @FXML private TextField  searchField;
    @FXML private TableView<Restaurant> tableView;
    @FXML private TableColumn<Restaurant, Number>  colId;
    @FXML private TableColumn<Restaurant, String>  colName;
    @FXML private TableColumn<Restaurant, String>  colAddress;
    @FXML private TableColumn<Restaurant, String>  colPhone;
    @FXML private TableColumn<Restaurant, String>  colHours;

    @FXML private TextField fldName;
    @FXML private TextField fldAddress;
    @FXML private TextField fldPhone;
    @FXML private TextField fldEmail;
    @FXML private TextField fldOpen;
    @FXML private TextField fldClose;
    @FXML private Label     errorLabel;

    @FXML private TableView<RestaurantTable>         tablesView;
    @FXML private TableColumn<RestaurantTable, Number> tColNum;
    @FXML private TableColumn<RestaurantTable, Number> tColCap;

    private Restaurant editingRestaurant = null;

    // -------------------------------------------------------
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(
                c.getValue().getId()));
        colName   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colAddress.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        colPhone  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPhone() != null ? c.getValue().getPhone() : "—"));
        colHours  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOpeningTime() + " – " + c.getValue().getClosingTime()));

        tColNum.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(
                c.getValue().getTableNumber()));
        tColCap.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(
                c.getValue().getCapacity()));

        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    populateForm(selected);
                    loadTables(selected);
                });

        loadData(restaurantService.findAll());
    }

    private void loadData(List<Restaurant> list) {
        tableView.setItems(FXCollections.observableArrayList(list));
        clearForm();
        tablesView.getItems().clear();
    }

    private void loadTables(Restaurant r) {
        if (r == null) { tablesView.getItems().clear(); return; }
        tablesView.setItems(FXCollections.observableArrayList(
                tableRepository.findByRestaurantIdOrderByTableNumber(r.getId())));
    }

    private void populateForm(Restaurant r) {
        if (r == null) return;
        editingRestaurant = r;
        fldName   .setText(r.getName());
        fldAddress.setText(r.getAddress());
        fldPhone  .setText(r.getPhone()  != null ? r.getPhone()  : "");
        fldEmail  .setText(r.getEmail()  != null ? r.getEmail()  : "");
        fldOpen   .setText(r.getOpeningTime() != null ? r.getOpeningTime().toString() : "");
        fldClose  .setText(r.getClosingTime() != null ? r.getClosingTime().toString() : "");
        errorLabel.setText("");
    }

    private void clearForm() {
        editingRestaurant = null;
        fldName.clear(); fldAddress.clear(); fldPhone.clear();
        fldEmail.clear(); fldOpen.clear(); fldClose.clear();
        errorLabel.setText("");
        tableView.getSelectionModel().clearSelection();
    }

    // ===== Обробники =====

    @FXML
    private void handleSearch() {
        String q = searchField.getText().trim();
        List<Restaurant> result = q.isEmpty()
                ? restaurantService.findAll()
                : restaurantService.search(q);
        loadData(result);
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadData(restaurantService.findAll());
    }

    @FXML
    private void handleAdd() {
        clearForm();
        fldName.requestFocus();
    }

    @FXML
    private void handleDelete() {
        Restaurant selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiUtils.warn("Нічого не вибрано", "Будь ласка, оберіть заклад у таблиці.");
            return;
        }
        Optional<ButtonType> confirm = UiUtils.confirm(
                "Видалення закладу",
                "Видалити «" + selected.getName() + "»?\nЦе видалить всі пов'язані дані.");
        if (confirm.filter(b -> b == ButtonType.OK).isPresent()) {
            try {
                restaurantService.delete(selected.getId());
                loadData(restaurantService.findAll());
            } catch (Exception e) {
                UiUtils.error("Помилка видалення", e.getMessage());
            }
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");
        try {
            String name    = fldName.getText().trim();
            String address = fldAddress.getText().trim();
            String phone   = fldPhone.getText().trim();
            String email   = fldEmail.getText().trim();
            String openStr = fldOpen.getText().trim();
            String closeStr= fldClose.getText().trim();

            if (name.isEmpty() || address.isEmpty() || openStr.isEmpty() || closeStr.isEmpty()) {
                errorLabel.setText("Поля «Назва», «Адреса», «Відкриття», «Закриття» є обов'язковими.");
                return;
            }

            LocalTime openTime;
            LocalTime closeTime;
            try {
                openTime  = LocalTime.parse(openStr);
                closeTime = LocalTime.parse(closeStr);
            } catch (DateTimeParseException e) {
                errorLabel.setText("Невірний формат часу. Приклад: 09:00");
                return;
            }

            if (!closeTime.isAfter(openTime)) {
                errorLabel.setText("Час закриття повинен бути після часу відкриття.");
                return;
            }

            Restaurant r = editingRestaurant != null
                    ? editingRestaurant
                    : new Restaurant();

            r.setName(name);
            r.setAddress(address);
            r.setPhone(phone.isEmpty()  ? null : phone);
            r.setEmail(email.isEmpty()  ? null : email);
            r.setOpeningTime(openTime);
            r.setClosingTime(closeTime);

            restaurantService.save(r);
            loadData(restaurantService.findAll());
            UiUtils.info("Збережено", "Заклад «" + name + "» успішно збережено.");

        } catch (RestaurantAppException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка збереження ресторану", e);
            errorLabel.setText("Непередбачена помилка: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        clearForm();
    }

    @FXML
    private void handleAddTable() {
        if (editingRestaurant == null) {
            UiUtils.warn("Не обрано заклад", "Спочатку оберіть заклад у таблиці ліворуь."); return;
        }
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Новий столик");
        dlg.setHeaderText("Додати столик до «" + editingRestaurant.getName() + "»");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField fNum = new TextField(); fNum.setPromptText("Номер стола");
        TextField fCap = new TextField(); fCap.setPromptText("Місць (місткість)");
        Label errLbl = new Label(); errLbl.setStyle("-fx-text-fill:red;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8); grid.setPadding(new Insets(14));
        grid.add(new Label("Номер стола *:"), 0, 0); grid.add(fNum, 1, 0);
        grid.add(new Label("Місткість *:"), 0, 1); grid.add(fCap, 1, 1);
        grid.add(errLbl, 0, 2, 2, 1);
        dlg.getDialogPane().setContent(grid);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            try {
                int n = Integer.parseInt(fNum.getText().trim());
                int c = Integer.parseInt(fCap.getText().trim());
                if (n <= 0 || c <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                errLbl.setText("Введіть позитивні числа."); evt.consume();
            }
        });

        dlg.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                RestaurantTable t = new RestaurantTable();
                t.setRestaurant(editingRestaurant);
                t.setTableNumber(Integer.parseInt(fNum.getText().trim()));
                t.setCapacity(Integer.parseInt(fCap.getText().trim()));
                tableRepository.save(t);
                loadTables(editingRestaurant);
            } catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
        });
    }

    @FXML
    private void handleDeleteTable() {
        RestaurantTable sel = tablesView.getSelectionModel().getSelectedItem();
        if (sel == null) { UiUtils.warn("Нічого не вибрано", "Оберіть столик у списку."); return; }
        UiUtils.confirm("Видалення",
                "Видалити Стіл №" + sel.getTableNumber() + "?").filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try { tableRepository.delete(sel); loadTables(editingRestaurant); }
            catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
        });
    }
}
