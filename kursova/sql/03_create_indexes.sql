-- ============================================================
-- Курсова робота: Мережа закладів харчування
-- Скрипт 03: Індекси
-- ============================================================

USE restaurant_network;

-- Пошук страв за категорією та доступністю
CREATE INDEX idx_menu_items_category    ON menu_items (category_id);
CREATE INDEX idx_menu_items_available   ON menu_items (is_available);

-- Пошук замовлень за рестораном, статусом та датою
CREATE INDEX idx_orders_restaurant      ON orders (restaurant_id);
CREATE INDEX idx_orders_status          ON orders (status);
CREATE INDEX idx_orders_created_at      ON orders (created_at);
CREATE INDEX idx_orders_completed_at    ON orders (completed_at);
CREATE INDEX idx_orders_customer        ON orders (customer_id);
CREATE INDEX idx_orders_employee        ON orders (employee_id);

-- Пошук столиків за рестораном
CREATE INDEX idx_tables_restaurant      ON tables (restaurant_id);

-- Пошук бронювань за часом та статусом
CREATE INDEX idx_reservations_time      ON reservations (reservation_time, end_time);
CREATE INDEX idx_reservations_table     ON reservations (table_id);
CREATE INDEX idx_reservations_status    ON reservations (status);
CREATE INDEX idx_reservations_customer  ON reservations (customer_id);

-- Пошук співробітників за рестораном та посадою
CREATE INDEX idx_employees_restaurant   ON employees (restaurant_id);
CREATE INDEX idx_employees_position     ON employees (position_id);
CREATE INDEX idx_employees_active       ON employees (is_active);

-- Пошук клієнтів
CREATE INDEX idx_customers_phone        ON customers (phone);
CREATE INDEX idx_customers_email        ON customers (email);

-- Позиції замовлення
CREATE INDEX idx_order_items_order      ON order_items (order_id);
CREATE INDEX idx_order_items_menu_item  ON order_items (menu_item_id);
