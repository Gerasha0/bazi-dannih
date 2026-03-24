-- ============================================================
-- Курсова робота: Мережа закладів харчування
-- Скрипт 02: Створення таблиць
-- ============================================================

USE restaurant_network;

-- ----------------------------
-- Заклади (ресторани)
-- ----------------------------
CREATE TABLE restaurants (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(100),
    opening_time TIME        NOT NULL DEFAULT '08:00:00',
    closing_time TIME        NOT NULL DEFAULT '22:00:00',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_working_hours CHECK (closing_time > opening_time)
) ENGINE=InnoDB;

-- ----------------------------
-- Категорії страв
-- ----------------------------
CREATE TABLE categories (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    description TEXT
) ENGINE=InnoDB;

-- ----------------------------
-- Страви (меню)
-- ----------------------------
CREATE TABLE menu_items (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    description   TEXT,
    price         DECIMAL(10,2)  NOT NULL,
    category_id   INT            NOT NULL,
    is_available  TINYINT(1)     NOT NULL DEFAULT 1,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mi_category FOREIGN KEY (category_id) REFERENCES categories(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_price_positive CHECK (price > 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Інгредієнти
-- ----------------------------
CREATE TABLE ingredients (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL UNIQUE,
    unit            VARCHAR(20)   NOT NULL COMMENT 'од. вимірювання: г, кг, мл, л, шт',
    cost_per_unit   DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
    CONSTRAINT chk_cost_non_negative CHECK (cost_per_unit >= 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Склад страви (M:N між menu_items та ingredients)
-- ----------------------------
CREATE TABLE menu_item_ingredients (
    menu_item_id  INT           NOT NULL,
    ingredient_id INT           NOT NULL,
    quantity      DECIMAL(10,3) NOT NULL,
    PRIMARY KEY (menu_item_id, ingredient_id),
    CONSTRAINT fk_mii_item       FOREIGN KEY (menu_item_id)  REFERENCES menu_items(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_mii_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_qty_positive CHECK (quantity > 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Столики
-- ----------------------------
CREATE TABLE tables (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id INT NOT NULL,
    table_number  INT NOT NULL,
    capacity      INT NOT NULL,
    CONSTRAINT fk_tables_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uq_table_in_restaurant UNIQUE (restaurant_id, table_number),
    CONSTRAINT chk_capacity_positive  CHECK (capacity > 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Посади
-- ----------------------------
CREATE TABLE positions (
    id    INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(80) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- ----------------------------
-- Співробітники
-- ----------------------------
CREATE TABLE employees (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(50)    NOT NULL,
    last_name     VARCHAR(50)    NOT NULL,
    position_id   INT            NOT NULL,
    restaurant_id INT            NOT NULL,
    phone         VARCHAR(20),
    email         VARCHAR(100),
    hire_date     DATE           NOT NULL,
    salary        DECIMAL(10,2)  NOT NULL,
    is_active     TINYINT(1)     NOT NULL DEFAULT 1,
    CONSTRAINT fk_emp_position   FOREIGN KEY (position_id)   REFERENCES positions(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_emp_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_salary_positive CHECK (salary > 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Клієнти
-- ----------------------------
CREATE TABLE customers (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(50) NOT NULL,
    last_name      VARCHAR(50) NOT NULL,
    phone          VARCHAR(20)  UNIQUE,
    email          VARCHAR(100) UNIQUE,
    loyalty_points INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loyalty_non_negative CHECK (loyalty_points >= 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Замовлення
-- ----------------------------
CREATE TABLE orders (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id INT            NOT NULL,
    table_id      INT,
    customer_id   INT,
    employee_id   INT,
    status        ENUM('PENDING','PREPARING','READY','SERVED','PAID','CANCELLED')
                  NOT NULL DEFAULT 'pending',
    total_amount  DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    notes         TEXT,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at  TIMESTAMP      NULL,
    CONSTRAINT fk_orders_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_orders_table      FOREIGN KEY (table_id)      REFERENCES tables(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_orders_customer   FOREIGN KEY (customer_id)   REFERENCES customers(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_orders_employee   FOREIGN KEY (employee_id)   REFERENCES employees(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_total_non_negative CHECK (total_amount >= 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Позиції замовлення
-- ----------------------------
CREATE TABLE order_items (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    order_id     INT            NOT NULL,
    menu_item_id INT            NOT NULL,
    quantity     INT            NOT NULL DEFAULT 1,
    unit_price   DECIMAL(10,2)  NOT NULL,
    CONSTRAINT fk_oi_order     FOREIGN KEY (order_id)     REFERENCES orders(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_oi_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_oi_qty_positive   CHECK (quantity > 0),
    CONSTRAINT chk_oi_price_positive CHECK (unit_price > 0)
) ENGINE=InnoDB;

-- ----------------------------
-- Бронювання столиків
-- ----------------------------
CREATE TABLE reservations (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    restaurant_id       INT         NOT NULL,
    table_id            INT         NOT NULL,
    customer_id         INT,
    reservation_time    DATETIME    NOT NULL,
    end_time            DATETIME    NOT NULL,
    party_size          INT         NOT NULL DEFAULT 1,
    status              ENUM('CONFIRMED','CANCELLED','COMPLETED','NO_SHOW')
                        NOT NULL DEFAULT 'confirmed',
    notes               TEXT,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_res_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_res_table      FOREIGN KEY (table_id)      REFERENCES tables(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_res_customer   FOREIGN KEY (customer_id)   REFERENCES customers(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_party_size    CHECK (party_size > 0),
    CONSTRAINT chk_res_end_after_start CHECK (end_time > reservation_time)
) ENGINE=InnoDB;
