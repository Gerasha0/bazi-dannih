package com.restaurant.ui;

import com.restaurant.entity.Customer;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.service.CustomerService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomersController implements Initializable {

    private final CustomerService customerService;

    @FXML private TextField          searchField;
    @FXML private TableView<Customer> tableView;
    @FXML private TableColumn<Customer, Number> colId;
    @FXML private TableColumn<Customer, String> colFull;
    @FXML private TableColumn<Customer, String> colPhone;
    @FXML private TableColumn<Customer, String> colEmail;
    @FXML private TableColumn<Customer, Number> colPoints;

    @FXML private TextField fldFirstName;
    @FXML private TextField fldLastName;
    @FXML private TextField fldPhone;
    @FXML private TextField fldEmail;
    @FXML private TextField fldPoints;
    @FXML private Label     errorLabel;

    private Customer editingCustomer = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId    .setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getId()));
        colFull  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colPhone .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPhone() != null ? c.getValue().getPhone() : "—"));
        colEmail .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmail() != null ? c.getValue().getEmail() : "—"));
        colPoints.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(
                c.getValue().getLoyaltyPoints()));

        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> populateForm(sel));

        loadData(customerService.findAll());
    }

    private void loadData(List<Customer> list) {
        tableView.setItems(FXCollections.observableArrayList(list));
        clearForm();
    }

    private void populateForm(Customer c) {
        if (c == null) return;
        editingCustomer = c;
        fldFirstName.setText(c.getFirstName());
        fldLastName .setText(c.getLastName());
        fldPhone    .setText(c.getPhone()  != null ? c.getPhone()  : "");
        fldEmail    .setText(c.getEmail()  != null ? c.getEmail()  : "");
        fldPoints   .setText(String.valueOf(c.getLoyaltyPoints()));
        errorLabel.setText("");
    }

    private void clearForm() {
        editingCustomer = null;
        fldFirstName.clear(); fldLastName.clear();
        fldPhone.clear(); fldEmail.clear(); fldPoints.setText("0");
        errorLabel.setText("");
        tableView.getSelectionModel().clearSelection();
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().trim();
        List<Customer> result = q.isEmpty()
                ? customerService.findAll()
                : customerService.search(q);
        tableView.setItems(FXCollections.observableArrayList(result));
    }

    @FXML private void handleRefresh() {
        searchField.clear(); loadData(customerService.findAll());
    }

    @FXML private void handleAdd() { clearForm(); fldFirstName.requestFocus(); }

    @FXML
    private void handleDelete() {
        Customer sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { UiUtils.warn("Нічого не вибрано", "Оберіть клієнта."); return; }
        Optional<ButtonType> c = UiUtils.confirm("Видалення",
                "Видалити клієнта «" + sel.getFullName() + "»?");
        if (c.filter(b -> b == ButtonType.OK).isPresent()) {
            try { customerService.delete(sel.getId()); loadData(customerService.findAll()); }
            catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");
        try {
            if (fldFirstName.getText().isBlank() || fldLastName.getText().isBlank()) {
                errorLabel.setText("Ім'я та прізвище є обов'язковими."); return;
            }
            Customer c = editingCustomer != null ? editingCustomer : new Customer();
            c.setFirstName(fldFirstName.getText().trim());
            c.setLastName(fldLastName.getText().trim());
            c.setPhone(fldPhone.getText().isBlank() ? null : fldPhone.getText().trim());
            c.setEmail(fldEmail.getText().isBlank() ? null : fldEmail.getText().trim());

            if (editingCustomer == null) customerService.save(c);
            else                         customerService.update(c.getId(), c);

            loadData(customerService.findAll());
            UiUtils.info("Збережено", "Дані клієнта успішно збережено.");

        } catch (RestaurantAppException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка збереження клієнта", e);
            errorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML private void handleCancel() { clearForm(); }
}
