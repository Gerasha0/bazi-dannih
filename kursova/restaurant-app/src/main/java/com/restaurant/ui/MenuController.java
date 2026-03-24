package com.restaurant.ui;

import com.restaurant.dto.MenuItemDto;
import com.restaurant.entity.Category;
import com.restaurant.entity.MenuItem;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.repository.CategoryRepository;
import com.restaurant.service.MenuItemService;
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
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class MenuController implements Initializable {

    private final MenuItemService menuItemService;
    private final CategoryRepository categoryRepository;

    @FXML private ComboBox<Category>  cbCategory;
    @FXML private ComboBox<String>    cbAvailability;
    @FXML private TextField           searchField;
    @FXML private TableView<MenuItem> tableView;
    @FXML private TableColumn<MenuItem, Number> colId;
    @FXML private TableColumn<MenuItem, String> colName;
    @FXML private TableColumn<MenuItem, String> colCategory;
    @FXML private TableColumn<MenuItem, String> colPrice;
    @FXML private TableColumn<MenuItem, String> colAvail;

    @FXML private TextField         fldName;
    @FXML private ComboBox<Category> fldCategory;
    @FXML private TextField         fldPrice;
    @FXML private TextArea          fldDescription;
    @FXML private CheckBox          chkAvailable;
    @FXML private Label             errorLabel;

    private MenuItem editingItem = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

        cbCategory.getItems().add(null);
        cbCategory.getItems().addAll(categories);
        cbCategory.setCellFactory(lv -> categoryCell());
        cbCategory.setButtonCell(categoryCell());

        cbAvailability.setItems(FXCollections.observableArrayList("Всі", "Доступні", "Недоступні"));
        cbAvailability.setValue("Всі");

        fldCategory.setItems(FXCollections.observableArrayList(categories));
        fldCategory.setCellFactory(lv -> categoryCell());
        fldCategory.setButtonCell(categoryCell());

        colId.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(
                c.getValue().getId()));
        colName    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory().getName()));
        colPrice   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrice().toPlainString()));
        colAvail   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAvailable() ? "✔" : "—"));

        tableView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> populateForm(sel));

        loadData(menuItemService.findAllIncludingUnavailable());
    }

    private ListCell<Category> categoryCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Category c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "— Всі категорії —" : c.getName());
            }
        };
    }

    private void loadData(List<MenuItem> list) {
        tableView.setItems(FXCollections.observableArrayList(list));
        clearForm();
    }

    private void populateForm(MenuItem m) {
        if (m == null) return;
        editingItem = m;
        fldName.setText(m.getName());
        fldPrice.setText(m.getPrice().toPlainString());
        fldDescription.setText(m.getDescription() != null ? m.getDescription() : "");
        fldCategory.setValue(m.getCategory());
        chkAvailable.setSelected(m.isAvailable());
        errorLabel.setText("");
    }

    private void clearForm() {
        editingItem = null;
        fldName.clear(); fldPrice.clear(); fldDescription.clear();
        fldCategory.setValue(null); chkAvailable.setSelected(true);
        errorLabel.setText("");
        tableView.getSelectionModel().clearSelection();
    }

    private List<MenuItem> applyFilters() {
        Category cat = cbCategory.getValue();
        String avail = cbAvailability.getValue();

        List<MenuItem> all = cat == null
                ? menuItemService.findAllIncludingUnavailable()
                : menuItemService.findByCategory(cat.getId());

        if ("Доступні".equals(avail))    return all.stream().filter(MenuItem::isAvailable).toList();
        if ("Недоступні".equals(avail)) return all.stream().filter(m -> !m.isAvailable()).toList();
        return all;
    }

    @FXML private void handleFilter() {
        tableView.setItems(FXCollections.observableArrayList(applyFilters()));
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) { handleFilter(); return; }
        List<MenuItem> result = menuItemService.search(q);
        String avail = cbAvailability.getValue();
        if ("Доступні".equals(avail))    result = result.stream().filter(MenuItem::isAvailable).toList();
        if ("Недоступні".equals(avail)) result = result.stream().filter(m -> !m.isAvailable()).toList();
        tableView.setItems(FXCollections.observableArrayList(result));
    }

    @FXML private void handleRefresh() {
        searchField.clear(); cbCategory.setValue(null); cbAvailability.setValue("Всі");
        loadData(menuItemService.findAllIncludingUnavailable());
    }

    @FXML private void handleAdd()    { clearForm(); fldName.requestFocus(); }

    @FXML
    private void handleToggleAvailability() {
        MenuItem sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { UiUtils.warn("Нічого не вибрано", "Оберіть страву у таблиці."); return; }
        try {
            MenuItemDto dto = MenuItemDto.builder()
                    .name(sel.getName())
                    .description(sel.getDescription())
                    .price(sel.getPrice())
                    .categoryId(sel.getCategory().getId())
                    .available(!sel.isAvailable())
                    .build();
            menuItemService.update(sel.getId(), dto);
            handleFilter();
        } catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
    }

    @FXML
    private void handleDelete() {
        MenuItem sel = tableView.getSelectionModel().getSelectedItem();
        if (sel == null) { UiUtils.warn("Нічого не вибрано", "Оберіть страву в таблиці."); return; }
        Optional<ButtonType> c = UiUtils.confirm("Видалення", "Видалити «" + sel.getName() + "»?");
        if (c.filter(bt -> bt == ButtonType.OK).isPresent()) {
            try {
                menuItemService.delete(sel.getId());
                handleRefresh();
            } catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
        }
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");
        try {
            String name  = fldName.getText().trim();
            String priceStr = fldPrice.getText().trim();
            Category cat = fldCategory.getValue();

            if (name.isEmpty() || priceStr.isEmpty() || cat == null) {
                errorLabel.setText("Заповніть обов'язкові поля: назва, ціна, категорія.");
                return;
            }
            BigDecimal price;
            try {
                price = new BigDecimal(priceStr);
                if (price.signum() <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                errorLabel.setText("Ціна повинна бути позитивним числом.");
                return;
            }

            MenuItemDto dto = MenuItemDto.builder()
                    .id(editingItem != null ? editingItem.getId() : null)
                    .name(name)
                    .description(fldDescription.getText().trim())
                    .price(price)
                    .categoryId(cat.getId())
                    .available(chkAvailable.isSelected())
                    .build();

            if (editingItem == null) menuItemService.save(dto);
            else                     menuItemService.update(editingItem.getId(), dto);

            handleRefresh();
            UiUtils.info("Збережено", "Страву «" + name + "» успішно збережено.");

        } catch (RestaurantAppException e) {
            errorLabel.setText(e.getMessage());
        } catch (Exception e) {
            log.error("Помилка збереження страви", e);
            errorLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML private void handleCancel() { clearForm(); }
}
