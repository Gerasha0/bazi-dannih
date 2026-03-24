package com.restaurant.ui;

import com.restaurant.entity.Customer;
import com.restaurant.entity.Employee;
import com.restaurant.entity.MenuItem;
import com.restaurant.entity.Order;
import com.restaurant.entity.OrderItem;
import com.restaurant.entity.OrderStatus;
import com.restaurant.entity.Restaurant;
import com.restaurant.entity.RestaurantTable;
import com.restaurant.exception.RestaurantAppException;
import com.restaurant.repository.RestaurantTableRepository;
import com.restaurant.service.CustomerService;
import com.restaurant.service.EmployeeService;
import com.restaurant.service.MenuItemService;
import com.restaurant.service.OrderService;
import com.restaurant.service.RestaurantService;
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
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrdersController implements Initializable {

    private final OrderService              orderService;
    private final RestaurantService         restaurantService;
    private final MenuItemService           menuItemService;
    private final EmployeeService           employeeService;
    private final CustomerService           customerService;
    private final RestaurantTableRepository tableRepository;
    private final ReportsController         reportsController;

    @FXML private ComboBox<Restaurant> cbRestaurant;
    @FXML private ComboBox<String>     cbStatus;

    @FXML private TableView<Order>     ordersTable;
    @FXML private TableColumn<Order, Number> colId;
    @FXML private TableColumn<Order, String> colRestaurant;
    @FXML private TableColumn<Order, String> colTable;
    @FXML private TableColumn<Order, String> colEmployee;
    @FXML private TableColumn<Order, String> colCustomer;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colCreatedAt;

    @FXML private TableView<OrderItem> itemsTable;
    @FXML private TableColumn<OrderItem, String> iColName;
    @FXML private TableColumn<OrderItem, Number> iColQty;
    @FXML private TableColumn<OrderItem, String> iColPrice;
    @FXML private TableColumn<OrderItem, String> iColTotal;

    @FXML private ComboBox<OrderStatus> cbNewStatus;
    @FXML private ComboBox<MenuItem>    cbDish;
    @FXML private TextField             fldQty;
    @FXML private Label                 orderErrorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Фільтри
        cbRestaurant.getItems().add(null);
        cbRestaurant.getItems().addAll(restaurantService.findAll());
        cbRestaurant.setCellFactory(lv -> restCell());
        cbRestaurant.setButtonCell(restCell());

        cbStatus.getItems().add("Всі");
        for (OrderStatus s : OrderStatus.values()) cbStatus.getItems().add(s.getDisplayName());
        cbStatus.setValue("Всі");

        // Головна таблиця
        colId        .setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getId()));
        colRestaurant.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRestaurant().getName()));
        colTable     .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTable() != null ? "Стіл " + c.getValue().getTable().getTableNumber() : "—"));
        colEmployee  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEmployee() != null ? c.getValue().getEmployee().getFullName() : "—"));
        colCustomer  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCustomer() != null ? c.getValue().getCustomer().getFullName() : "—"));
        colStatus    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getDisplayName()));
        colTotal     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTotalAmount().toPlainString()));
        colCreatedAt .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt().toString().replace("T", " ")));

        ordersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> showOrderItems(sel));

        // Деталі замовлення
        iColName .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMenuItem().getName()));
        iColQty  .setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().getQuantity()));
        iColPrice.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnitPrice().toPlainString()));
        iColTotal.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSubtotal().toPlainString()));

        // Зміна статусу
        cbNewStatus.setItems(FXCollections.observableArrayList(OrderStatus.values()));
        cbNewStatus.setCellFactory(lv -> statusCell());
        cbNewStatus.setButtonCell(statusCell());

        // Вибір страви для додавання
        List<MenuItem> dishes = menuItemService.findAvailable();
        cbDish.setItems(FXCollections.observableArrayList(dishes));
        cbDish.setCellFactory(lv -> dishCell());
        cbDish.setButtonCell(dishCell());

        refreshOrders();
    }

    private void refreshOrders() {
        Restaurant r = cbRestaurant.getValue();
        List<Order> all = r == null ? orderService.findAll()
                                    : orderService.findByRestaurant(r.getId());

        String statusFilter = cbStatus.getValue();
        if (statusFilter != null && !"Всі".equals(statusFilter)) {
            all = all.stream()
                    .filter(o -> o.getStatus().getDisplayName().equals(statusFilter))
                    .toList();
        }
        ordersTable.setItems(FXCollections.observableArrayList(all));
    }

    private void showOrderItems(Order order) {
        if (order == null) {
            itemsTable.getItems().clear();
            return;
        }
        itemsTable.setItems(FXCollections.observableArrayList(order.getItems()));
    }

    private Order selectedOrder() {
        return ordersTable.getSelectionModel().getSelectedItem();
    }

    @FXML private void handleFilter()  { refreshOrders(); }
    @FXML private void handleRefresh() { cbRestaurant.setValue(null); cbStatus.setValue("Всі"); refreshOrders(); }

    /** Оновлює список закладів у фільтрі (викликається при переході на вкладку). */
    public void refreshRestaurantCombo() {
        Integer selId = cbRestaurant.getValue() != null ? cbRestaurant.getValue().getId() : null;
        cbRestaurant.getItems().clear();
        cbRestaurant.getItems().add(null);
        List<Restaurant> freshList = restaurantService.findAll();
        cbRestaurant.getItems().addAll(freshList);
        if (selId != null) {
            freshList.stream().filter(r -> r.getId().equals(selId)).findFirst()
                    .ifPresent(cbRestaurant::setValue);
        }
    }

    @FXML
    private void handleNewOrder() {
        List<Restaurant> restaurants = restaurantService.findAll();
        if (restaurants.isEmpty()) {
            UiUtils.warn("Немає закладів", "Спочатку додайте заклад."); return;
        }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Нове замовлення");
        dlg.setHeaderText("Заповніть дані нового замовлення:");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Restaurant>      cbR = new ComboBox<>();
        ComboBox<RestaurantTable> cbT = new ComboBox<>();
        ComboBox<Employee>        cbE = new ComboBox<>();
        ComboBox<Customer>        cbC = new ComboBox<>();
        Label errLbl = new Label();
        errLbl.setStyle("-fx-text-fill:red;");

        cbR.setItems(FXCollections.observableArrayList(restaurants));
        cbR.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty); setText(empty || r == null ? "" : r.getName());
            }
        });
        cbR.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "— Оберіть заклад —" : r.getName());
            }
        });
        cbR.setPrefWidth(220);

        cbT.setPromptText("— Без столу —"); cbT.setPrefWidth(220);
        cbT.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(RestaurantTable t, boolean empty) {
                super.updateItem(t, empty); setText(empty || t == null ? "" : t.toString());
            }
        });
        cbT.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(RestaurantTable t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "— Без столу —" : t.toString());
            }
        });

        cbE.setPromptText("— Без офіціанта —"); cbE.setPrefWidth(220);
        cbE.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty); setText(empty || e == null ? "" : e.getFullName());
            }
        });
        cbE.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "— Без офіціанта —" : e.getFullName());
            }
        });

        cbC.setPromptText("— Без клієнта —"); cbC.setPrefWidth(220);
        cbC.setItems(FXCollections.observableArrayList(customerService.findAll()));
        cbC.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty); setText(empty || c == null ? "" : c.getFullName());
            }
        });
        cbC.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "— Без клієнта —" : c.getFullName());
            }
        });

        // При виборі закладу — автоматично завантажуємо його столи та активних офіціантів
        cbR.valueProperty().addListener((obs, oldR, newR) -> {
            cbT.getItems().clear(); cbT.setValue(null);
            cbE.getItems().clear(); cbE.setValue(null);
            if (newR != null) {
                cbT.setItems(FXCollections.observableArrayList(
                        tableRepository.findByRestaurantIdOrderByTableNumber(newR.getId())));
                cbE.setItems(FXCollections.observableArrayList(
                        employeeService.findActiveByRestaurant(newR.getId())));
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Заклад *:"),  0, 0); grid.add(cbR, 1, 0);
        grid.add(new Label("Стіл:"),      0, 1); grid.add(cbT, 1, 1);
        grid.add(new Label("Офіціант:"),  0, 2); grid.add(cbE, 1, 2);
        grid.add(new Label("Клієнт:"),    0, 3); grid.add(cbC, 1, 3);
        grid.add(errLbl,                  0, 4, 2, 1);
        dlg.getDialogPane().setContent(grid);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            if (cbR.getValue() == null) { errLbl.setText("Оберіть заклад!"); evt.consume(); }
        });

        dlg.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                Integer tId = cbT.getValue() != null ? cbT.getValue().getId() : null;
                Integer eId = cbE.getValue() != null ? cbE.getValue().getId() : null;
                Integer cId = cbC.getValue() != null ? cbC.getValue().getId() : null;
                orderService.createOrder(cbR.getValue().getId(), tId, cId, eId);
                refreshOrders();
            } catch (RestaurantAppException e) {
                UiUtils.error("Помилка", e.getMessage());
            }
        });
    }

    @FXML
    private void handleDeleteOrder() {
        Order o = selectedOrder();
        if (o == null) { UiUtils.warn("Нічого не вибрано", "Оберіть замовлення у таблиці."); return; }
        UiUtils.confirm("Видалення",
                "Видалити замовлення #" + o.getId() + "?").filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try { orderService.delete(o.getId()); refreshOrders(); }
            catch (Exception e) { UiUtils.error("Помилка", e.getMessage()); }
        });
    }

    @FXML
    private void handleFreeTable() {
        Order o = selectedOrder();
        if (o == null) { UiUtils.warn("Нічого не вибрано", "Оберіть замовлення у таблиці."); return; }
        if (o.getTable() == null) {
            UiUtils.warn("Столик не призначено", "У цього замовлення немає прив'язаного столика."); return;
        }
        String tableLabel = "Стіл №" + o.getTable().getTableNumber();
        UiUtils.confirm("Звільнити стіл",
                tableLabel + " — підтвердіть, що гості покинули місця.")
                .filter(bt -> bt == ButtonType.OK)
                .ifPresent(bt -> {
                    try {
                        orderService.freeTable(o.getTable().getId());
                        refreshOrders();
                        reportsController.refreshOccupancyNow();
                        UiUtils.info("Стіл звільнено", tableLabel + " тепер вільний.");
                    } catch (Exception e) {
                        UiUtils.error("Помилка", e.getMessage());
                    }
                });
    }

    @FXML
    private void handleEditAssignment() {
        Order o = selectedOrder();
        if (o == null) { UiUtils.warn("Нічого не вибрано", "Оберіть замовлення."); return; }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Призначення до замовлення #" + o.getId());
        dlg.setHeaderText("Стіл / Офіціант / Клієнт (заклад: " + o.getRestaurant().getName() + ")");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<RestaurantTable> cbT = new ComboBox<>();
        ComboBox<Employee>        cbE = new ComboBox<>();
        ComboBox<Customer>        cbC = new ComboBox<>();

        cbT.setItems(FXCollections.observableArrayList(
                tableRepository.findByRestaurantIdOrderByTableNumber(o.getRestaurant().getId())));
        cbT.setValue(o.getTable());
        cbT.setPromptText("— Без столу —"); cbT.setPrefWidth(220);
        cbT.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(RestaurantTable t, boolean empty) {
                super.updateItem(t, empty); setText(empty || t == null ? "— Без столу —" : t.toString());
            }
        });
        cbT.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(RestaurantTable t, boolean empty) {
                super.updateItem(t, empty); setText(empty || t == null ? "— Без столу —" : t.toString());
            }
        });

        cbE.setItems(FXCollections.observableArrayList(
                employeeService.findActiveByRestaurant(o.getRestaurant().getId())));
        cbE.setValue(o.getEmployee());
        cbE.setPromptText("— Без офіціанта —"); cbE.setPrefWidth(220);
        cbE.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty); setText(empty || e == null ? "— Без офіціанта —" : e.getFullName());
            }
        });
        cbE.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Employee e, boolean empty) {
                super.updateItem(e, empty); setText(empty || e == null ? "— Без офіціанта —" : e.getFullName());
            }
        });

        cbC.setItems(FXCollections.observableArrayList(customerService.findAll()));
        cbC.setValue(o.getCustomer());
        cbC.setPromptText("— Без клієнта —"); cbC.setPrefWidth(220);
        cbC.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty); setText(empty || c == null ? "— Без клієнта —" : c.getFullName());
            }
        });
        cbC.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Customer c, boolean empty) {
                super.updateItem(c, empty); setText(empty || c == null ? "— Без клієнта —" : c.getFullName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.setPadding(new Insets(16));
        grid.add(new Label("Стіл:"),     0, 0); grid.add(cbT, 1, 0);
        grid.add(new Label("Офіціант:"), 0, 1); grid.add(cbE, 1, 1);
        grid.add(new Label("Клієнт:"),   0, 2); grid.add(cbC, 1, 2);
        dlg.getDialogPane().setContent(grid);

        dlg.showAndWait().filter(bt -> bt == ButtonType.OK).ifPresent(bt -> {
            try {
                Integer tId = cbT.getValue() != null ? cbT.getValue().getId() : null;
                Integer eId = cbE.getValue() != null ? cbE.getValue().getId() : null;
                Integer cId = cbC.getValue() != null ? cbC.getValue().getId() : null;
                orderService.updateAssignment(o.getId(), tId, eId, cId);
                refreshOrders();
            } catch (RestaurantAppException e) { UiUtils.error("Помилка", e.getMessage()); }
        });
    }

    @FXML
    private void handleChangeStatus() {
        Order o = selectedOrder();
        OrderStatus ns = cbNewStatus.getValue();
        if (o == null || ns == null) {
            UiUtils.warn("Нічого не вибрано", "Оберіть замовлення і новий статус."); return;
        }
        try {
            orderService.changeStatus(o.getId(), ns);
            refreshOrders();
        } catch (RestaurantAppException e) {
            orderErrorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handlePay() {
        Order o = selectedOrder();
        if (o == null) { UiUtils.warn("Нічого не вибрано", "Оберіть замовлення."); return; }
        try {
            orderService.payOrder(o.getId());
            refreshOrders();
            UiUtils.info("Оплачено", "Замовлення #" + o.getId() + " оплачено успішно.");
        } catch (RestaurantAppException e) {
            orderErrorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleAddItem() {
        orderErrorLabel.setText("");
        Order o = selectedOrder();
        MenuItem dish = cbDish.getValue();
        if (o == null || dish == null) {
            orderErrorLabel.setText("Оберіть замовлення і страву."); return;
        }
        int qty;
        try {
            qty = Integer.parseInt(fldQty.getText().trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            orderErrorLabel.setText("Вкажіть кількість (ціле позитивне число)."); return;
        }
        try {
            orderService.addItem(o.getId(), dish.getId(), qty);
            refreshOrders();
            // Re-select
            ordersTable.getItems().stream()
                    .filter(ord -> ord.getId().equals(o.getId())).findFirst()
                    .ifPresent(ord -> { ordersTable.getSelectionModel().select(ord); showOrderItems(ord); });
        } catch (RestaurantAppException e) {
            orderErrorLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleRemoveItem() {
        orderErrorLabel.setText("");
        Order o = selectedOrder();
        OrderItem item = itemsTable.getSelectionModel().getSelectedItem();
        if (o == null || item == null) {
            orderErrorLabel.setText("Оберіть замовлення та позицію для видалення."); return;
        }
        try {
            orderService.removeItem(o.getId(), item.getId());
            refreshOrders();
            ordersTable.getItems().stream()
                    .filter(ord -> ord.getId().equals(o.getId())).findFirst()
                    .ifPresent(ord -> { ordersTable.getSelectionModel().select(ord); showOrderItems(ord); });
        } catch (RestaurantAppException e) {
            orderErrorLabel.setText(e.getMessage());
        }
    }

    // --- Cells ---
    private ListCell<Restaurant> restCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Restaurant r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? "— Всі заклади —" : r.getName());
            }
        };
    }

    private ListCell<OrderStatus> statusCell() {
        return new ListCell<>() {
            @Override protected void updateItem(OrderStatus s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s.getDisplayName());
            }
        };
    }

    private ListCell<MenuItem> dishCell() {
        return new ListCell<>() {
            @Override protected void updateItem(MenuItem m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? "" : m.getName() + " (" + m.getPrice() + " грн)");
            }
        };
    }
}
