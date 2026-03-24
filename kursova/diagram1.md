@startuml RestaurantNetwork_ER
!define TABLE(name,desc) class name as "desc" << (T,#FFAAAA) >>
!define PK(x) <u>x</u>
hide methods
hide stereotypes
skinparam classFontSize 11
skinparam classBackgroundColor #FFFFF0
skinparam classBorderColor #8B4513

TABLE(restaurants, "restaurants\n(Заклади)") {
  PK(id) INT AI
  --
  name VARCHAR(100) NOT NULL
  address VARCHAR(255) NOT NULL
  phone VARCHAR(20)
  email VARCHAR(100)
  opening_time TIME DEFAULT '08:00:00'
  closing_time TIME DEFAULT '22:00:00'
  created_at TIMESTAMP
}

TABLE(categories, "categories\n(Категорії меню)") {
  PK(id) INT AI
  --
  name VARCHAR(80) UNIQUE
  description TEXT
}

TABLE(menu_items, "menu_items\n(Позиції меню)") {
  PK(id) INT AI
  --
  name VARCHAR(100) NOT NULL
  description TEXT
  price DECIMAL(10,2) NOT NULL
  category_id INT FK
  is_available TINYINT(1) DEFAULT 1
  created_at TIMESTAMP
}

TABLE(ingredients, "ingredients\n(Інгредієнти)") {
  PK(id) INT AI
  --
  name VARCHAR(100) UNIQUE
  unit VARCHAR(20)
  cost_per_unit DECIMAL(10,4)
}

TABLE(menu_item_ingredients, "menu_item_ingredients\n(Склад страви)") {
  PK(menu_item_id) INT FK
  PK(ingredient_id) INT FK
  --
  quantity DECIMAL(10,3) NOT NULL
}

TABLE(tables, "tables\n(Столики)") {
  PK(id) INT AI
  --
  restaurant_id INT FK
  table_number INT NOT NULL
  capacity INT NOT NULL
}

TABLE(positions, "positions\n(Посади)") {
  PK(id) INT AI
  --
  title VARCHAR(80) UNIQUE
}

TABLE(employees, "employees\n(Співробітники)") {
  PK(id) INT AI
  --
  first_name VARCHAR(50) NOT NULL
  last_name VARCHAR(50) NOT NULL
  position_id INT FK
  restaurant_id INT FK
  phone VARCHAR(20)
  email VARCHAR(100)
  hire_date DATE
  salary DECIMAL(10,2)
  is_active TINYINT(1) DEFAULT 1
}

TABLE(customers, "customers\n(Клієнти)") {
  PK(id) INT AI
  --
  first_name VARCHAR(50) NOT NULL
  last_name VARCHAR(50) NOT NULL
  phone VARCHAR(20) UNIQUE
  email VARCHAR(100) UNIQUE
  loyalty_points INT DEFAULT 0
  created_at TIMESTAMP
}

TABLE(orders, "orders\n(Замовлення)") {
  PK(id) INT AI
  --
  restaurant_id INT FK
  table_id INT FK
  customer_id INT FK
  employee_id INT FK
  status ENUM('PENDING','PREPARING','READY','SERVED','PAID','CANCELLED')
  total_amount DECIMAL(10,2) DEFAULT 0.00
  notes TEXT
  created_at TIMESTAMP
  completed_at TIMESTAMP
}

TABLE(order_items, "order_items\n(Позиції замовлення)") {
  PK(id) INT AI
  --
  order_id INT FK
  menu_item_id INT FK
  quantity INT NOT NULL
  unit_price DECIMAL(10,2) NOT NULL
}

TABLE(reservations, "reservations\n(Бронювання)") {
  PK(id) INT AI
  --
  restaurant_id INT FK
  table_id INT FK
  customer_id INT FK
  reservation_time DATETIME NOT NULL
  end_time DATETIME NOT NULL
  party_size INT NOT NULL
  status ENUM('CONFIRMED','CANCELLED','COMPLETED','NO_SHOW')
  notes TEXT
  created_at TIMESTAMP
}

' Зв'язки
restaurants    "1" --o{ "N" tables         : "має"
restaurants    "1" --o{ "N" employees      : "працює"
restaurants    "1" --o{ "N" orders         : "реєструє"
restaurants    "1" --o{ "N" reservations   : "бронює"

categories     "1" --o{ "N" menu_items     : "містить"

menu_items     "1" --o{ "N" menu_item_ingredients : "входить"
ingredients    "1" --o{ "N" menu_item_ingredients : "входить"
menu_items     "1" --o{ "N" order_items    : "замовляють"

tables         "1" --o{ "N" orders         : "на столику"
tables         "1" --o{ "N" reservations   : "бронюється"

positions      "1" --o{ "N" employees      : "займає"

customers      "1" --o{ "N" orders         : "робить"
customers      "1" --o{ "N" reservations   : "бронює"
employees      "1" --o{ "N" orders         : "обслуговує"

orders         "1" --o{ "N" order_items    : "містить"

@enduml