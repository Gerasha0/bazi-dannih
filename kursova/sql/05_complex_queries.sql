-- ============================================================
-- Курсова робота: Мережа закладів харчування
-- Скрипт 05: Складні аналітичні запити
-- ============================================================

USE restaurant_network;

-- -----------------------------------------------------------
-- Q1. Вільні столики у конкретному закладі на заданий час
--     Параметри: :restaurant_id = 1, :check_time = поточний час
-- -----------------------------------------------------------
SELECT
    t.id            AS table_id,
    t.table_number,
    t.capacity,
    r.name          AS restaurant
FROM tables t
JOIN restaurants r ON r.id = t.restaurant_id
WHERE t.restaurant_id = 1
  AND t.id NOT IN (
      SELECT res.table_id
      FROM reservations res
      WHERE res.restaurant_id = 1
        AND res.status IN ('confirmed')
        AND res.reservation_time <= NOW()
        AND res.end_time        >= NOW()
  )
ORDER BY t.table_number;

-- -----------------------------------------------------------
-- Q2. Звіт про продажі за квітень…поточний місяць
-- -----------------------------------------------------------
SELECT
    r.name                          AS restaurant,
    DATE(o.created_at)              AS sale_date,
    COUNT(DISTINCT o.id)            AS orders_count,
    SUM(oi.quantity)                AS items_sold,
    SUM(oi.quantity * oi.unit_price) AS total_revenue
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
JOIN restaurants r   ON r.id = o.restaurant_id
WHERE o.status = 'paid'
  AND o.created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')
  AND o.created_at <  DATE_FORMAT(NOW() + INTERVAL 1 MONTH, '%Y-%m-01')
GROUP BY r.id, DATE(o.created_at)
ORDER BY r.id, sale_date;

-- -----------------------------------------------------------
-- Q3. Звіт — загальний виторг за рестораном і місяцем
-- -----------------------------------------------------------
SELECT
    r.name                                              AS restaurant,
    DATE_FORMAT(o.created_at, '%Y-%m')                  AS month,
    COUNT(DISTINCT o.id)                                AS total_orders,
    SUM(oi.quantity * oi.unit_price)                    AS total_revenue,
    AVG(oi.quantity * oi.unit_price)                    AS avg_order_value
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
JOIN restaurants r   ON r.id = o.restaurant_id
WHERE o.status = 'paid'
GROUP BY r.id, DATE_FORMAT(o.created_at, '%Y-%m')
ORDER BY month DESC, total_revenue DESC;

-- -----------------------------------------------------------
-- Q4. Топ-10 найпопулярніших страв (за кількістю замовлень)
-- -----------------------------------------------------------
SELECT
    mi.id,
    mi.name                          AS dish,
    c.name                           AS category,
    mi.price,
    SUM(oi.quantity)                 AS times_ordered,
    SUM(oi.quantity * oi.unit_price) AS total_revenue
FROM order_items oi
JOIN menu_items mi ON mi.id = oi.menu_item_id
JOIN categories  c ON c.id  = mi.category_id
JOIN orders      o ON o.id  = oi.order_id
WHERE o.status = 'paid'
GROUP BY mi.id
ORDER BY times_ordered DESC
LIMIT 10;

-- -----------------------------------------------------------
-- Q5. Завантаженість персоналу (скільки замовлень на кожного офіціанта)
-- -----------------------------------------------------------
SELECT
    e.id,
    CONCAT(e.first_name, ' ', e.last_name) AS employee,
    p.title                                AS position,
    r.name                                 AS restaurant,
    COUNT(o.id)                            AS orders_served,
    SUM(o.total_amount)                    AS total_served_amount
FROM employees e
JOIN positions  p ON p.id = e.position_id
JOIN restaurants r ON r.id = e.restaurant_id
LEFT JOIN orders o ON o.employee_id = e.id AND o.status = 'paid'
GROUP BY e.id
ORDER BY orders_served DESC;

-- -----------------------------------------------------------
-- Q6. Клієнти з найбільшою кількістю замовлень та сумою
-- -----------------------------------------------------------
SELECT
    c.id,
    CONCAT(c.first_name, ' ', c.last_name) AS customer,
    c.phone,
    c.loyalty_points,
    COUNT(DISTINCT o.id)                   AS total_orders,
    COALESCE(SUM(o.total_amount), 0)       AS total_spent
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id AND o.status = 'paid'
GROUP BY c.id
ORDER BY total_spent DESC;

-- -----------------------------------------------------------
-- Q7. Середній чек по ресторанах
-- -----------------------------------------------------------
SELECT
    r.name                              AS restaurant,
    COUNT(DISTINCT o.id)                AS paid_orders,
    ROUND(AVG(o.total_amount), 2)       AS avg_check,
    MIN(o.total_amount)                 AS min_check,
    MAX(o.total_amount)                 AS max_check
FROM orders o
JOIN restaurants r ON r.id = o.restaurant_id
WHERE o.status = 'paid'
GROUP BY r.id
ORDER BY avg_check DESC;
