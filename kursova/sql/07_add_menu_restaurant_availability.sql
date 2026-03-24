-- ============================================================
-- Migration 07: Per-restaurant menu item availability
-- A menu item can now be assigned to specific restaurants.
-- ============================================================

-- Join table: which menu items are available at which restaurants
CREATE TABLE IF NOT EXISTS menu_item_restaurants (
    menu_item_id INT NOT NULL,
    restaurant_id INT NOT NULL,
    PRIMARY KEY (menu_item_id, restaurant_id),
    CONSTRAINT fk_mir_menu_item  FOREIGN KEY (menu_item_id)  REFERENCES menu_items(id)   ON DELETE CASCADE,
    CONSTRAINT fk_mir_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate existing data: every currently-available item gets assigned to ALL restaurants
INSERT IGNORE INTO menu_item_restaurants (menu_item_id, restaurant_id)
SELECT mi.id, r.id
FROM menu_items mi
CROSS JOIN restaurants r
WHERE mi.is_available = 1;
