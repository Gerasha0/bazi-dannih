package com.restaurant.ui;

import com.restaurant.entity.*;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.repository.PositionRepository;
import com.restaurant.service.EmployeeService;
import com.restaurant.service.RestaurantService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeesController implements Initializable {

    private final EmployeeService    employeeService;
    private final RestaurantService  restaurantService;
    private final PositionRepository positionRepository;

    @FXML private ComboBox<Restaurant> cbRestaurant;
    @FXML private TextField            searchField;
    @FXML private TableView<Employee>  tableView;
    @FXML private TableColumn<Employee, Number> colId;
    @FXML private TableColumn<Employee, String> colLastName;
    @FXML private TableColumn<Employee, String> colFirstName;
    @FXML private TableColumn<Employee, String> colPosition;
    @FXML private TableColumn<Employee, String> colRestaurant;
    @FXML private TableColumn<Employee, String> colSalary;
    @FXML private TableColumn<Employee, String> colActive;

    @FXML private TextField           fldFirstName;
    @FXML private TextField           fldLastName;
    @FXML private ComboBox<Position>  fldPosition;
    @FXML private ComboBox<Restaurant> fldRestaurant;
    @FXML private TextField           fldPhone;
    @FXML private TextField           fldEmail;
    @FXML private TextField           fldHireDate;
    @FXML private TextField           fldSalary;
    @FXML private CheckBox            chkActive;
    @FXML private Label               errorLabel;

    private Employee editingEmployee = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<Restaurant> restaurants = restaurantService.findAll();
        cbRestaurant.getItems().add(null);
        cbRestaurant.getItems().addAll(restaurants);
        cbRestaurant.setCellFactory(lv -> restCell());
        cbRestaurant.setButtonCell(restCell());

        fldRestaurant.setItems(FXCollections.observableArrayList(restaurants));
        fldRestaurant.setCellFactory(lv -> restCell());
        fldRestaurant.setButtonCell(restCell());

        List<Position> positions = positionRepository.findAllByOrderByTitleAsc();
        fldPosition.setItems(FXCollections.observableArrayList(positions));
        fldPosition.setCellFactory(lv -> posCell());
        fldPosition.setButtonCell(posCell());

        colId        .setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getId()));
        colLastName  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLastName()));
        colFirstName .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFirstName()));
        colPosition  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPosition().getTitle()));
        colRestaurant.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRestaurant().getName()));
        colSalary    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSalary().toPlainString()));
        colActive    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "✓" : "×"));

        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> populateForm(sel));

        loadData(employeeService.findAll());
    }

    /** Оновлює список закладів у фільтрі та формі (викликається при переході на вкладку). */
    public void refreshRestaurantCombo() {
        Integer filterSelId = cbRestaurant.getValue()  != null ? cbRestaurant.getValue().getId()  : null;
        Integer formSelId   = fldRestaurant.getValue() != null ? fldRestaurant.getValue().getId() : null;
        List<Restaurant> restaurants = restaurantService.findAll();

        cbRestaurant.getItems().clear();
        cbRestaurant.getItems().add(null);
        cbRestaurant.getItems().addAll(restaurants);
        if (filterSelId != null) {
            restaurants.stream().filter(r -> r.getId().equals(filterSelId)).findFirst()
                    .ifPresent(cbRestaurant::setValue);
        }

        fldRestaurant.setItems(FXCollections.observableArrayList(restaurants));
        if (formSelId != null) {
            restaurants.stream().filter(r -> r.getId().equals(formSelId)).findFirst()
                    .ifPresent(fldRestaurant::setValue);
        }
    }

    private ListCell<Restaurant> restCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "— Всі заклади —" : r.getName());
            }
        };
    }

    private ListCell<Position> posCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Position p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getTitle());
            }
        };
    }

    private void loadData(List<Employee> list) {
        tableView.setItems(FXCollections.observableArrayList(list));
        clearForm();
    }

    private void populateForm(Employee e) {
        if (e == null) return;
        editingEmployee = e;
        fldFirstName .setText(e.getFirstName());
        fldLastName  .setText(e.getLastName());
        fldPosition  .setValue(e.getPosition());
        fldRestaurant.setValue(e.getRestaurant());
        fldPhone     .setText(e.getPhone()  != null ? e.getPhone()  : "");
        fldEmail     .setText(e.getEmail()  != null ? e.getEmail()  : "");
        fldHireDate  .setText(e.getHireDate().toString());
        fldSalary    .setText(e.getSalary().toPlainString());
        chkActive    .setSelected(e.isActive());
        errorLabel.setText("");
    }

    private void clearForm() {
        editingEmployee = null;
        fldFirstName.clear(); fldLastName.clear();
        fldPhone.clear(); fldEmail.clear();
        fldHireDate.clear(); fldSalary.clear();
        fldPosition.setValue(null); fldRestaurant.setValue(null);
        chkActive.setSelected(true);
        errorLabel.setText("");
        tableView.getSelectionModel().clearSelection();
    }

    @FXML private void handleFilter() {
        Restaurant r = cbRestaurant.getValue();
        List<Employee> result = r == null
                ? employeeService.findAll()
                : employeeService.findByRestaurant(r.getId());
        tableView.setItems(FXCollections.observableArrayList(result));
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().trim();
        List<Employee> result = q.isEmpty()
                ? employeeService.findAll()
                : employeeService.search(q);
        tableView.setItems(FXCollections.observableArrayList(result));
    }

    @FXML private void handleRefresh() {
        searchField.clear(); cbRestaurant.setValue(null);
        loadData(employeeService.findAll());
    }

    @FXML private void handleAdd() { clearForm(); fldFirstName.requestFocus(); }

    @FXML
    private void handleDeactivate() {
        Employee sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { UiUtils.warn("Нічого не вибрано", "Оберіть співробітника."); return; }
        Optional<ButtonType> c = UiUtils.confirm("Деактивація", "Деактивувати «" + sel.getFullName() + "»?");
        if (c.filter(b -> b == ButtonType.OK).isPresent()) {
            try { employeeService.deactivate(sel.getId()); loadData(employeeService.findAll()); }
            catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
        }
    }

    @FXML
    private void handleDelete() {
        Employee sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { UiUtils.warn("Нічого не вибрано", "Оберіть співробітника."); return; }
        Optional<ButtonType> c = UiUtils.confirm("Видалення", "Видалити «" + sel.getFullName() + "»?");
        if (c.filter(b -> b == ButtonType.OK).isPresent()) {
            try { employeeService.delete(sel.getId()); loadData(employeeService.findAll()); }
            catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");
        try {
            if (fldFirstName.getText().isBlank() || fldLastName.getText().isBlank()
                    || fldPosition.getValue() == null || fldRestaurant.getValue() == null
                    || fldHireDate.getText().isBlank() || fldSalary.getText().isBlank()) {
                errorLabel.setText("Заповніть усі обов'язкові поля (*)."); return;
            }
            LocalDate hireDate;
            try { hireDate = LocalDate.parse(fldHireDate.getText().trim()); }
            catch (DateTimeParseException e) { errorLabel.setText("Невірний формат дати. Приклад: 2024-01-15"); return; }

            BigDecimal salary;
            try {
                salary = new BigDecimal(fldSalary.getText().trim());
                if (salary.signum() <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) { errorLabel.setText("Зарплата повинна бути позитивним числом."); return; }

            Employee emp = editingEmployee != null ? editingEmployee : new Employee();
            emp.setFirstName(fldFirstName.getText().trim());
            emp.setLastName(fldLastName.getText().trim());
            emp.setPosition(fldPosition.getValue());
            emp.setRestaurant(fldRestaurant.getValue());
            emp.setPhone(fldPhone.getText().isBlank() ? null : fldPhone.getText().trim());
            emp.setEmail(fldEmail.getText().isBlank() ? null : fldEmail.getText().trim());
            emp.setHireDate(hireDate);
            emp.setSalary(salary);
            emp.setActive(chkActive.isSelected());

            if (editingEmployee == null) employeeService.save(emp);
            else                         employeeService.update(emp.getId(), emp);

            loadData(employeeService.findAll());
            UiUtils.info("Збережено", "Дані співробітника успішно збережено.");

        } catch (RestaurantAppException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка збереження співробітника", e);
            errorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML private void handleCancel() { clearForm(); }
}
