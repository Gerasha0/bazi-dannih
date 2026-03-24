package com.university.decanat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Головне вікно додатку для роботи з БД Деканату
 * Лабораторна робота №4
 */
public class DecanatApp extends JFrame {
    
    // Компоненти GUI
    private JTabbedPane tabbedPane;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JTextArea resultArea;
    
    // Панелі вкладок
    private JPanel sortPanel;
    private JPanel filterPanel;
    private JPanel joinPanel;
    private JPanel aggregatePanel;
    private JPanel groupByPanel;
    
    public DecanatApp() {
        setTitle("Деканат - Управління БД (Lab 4)");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Перевірка підключення до БД
        if (!DatabaseConnection.testConnection()) {
            JOptionPane.showMessageDialog(this,
                "Не вдалося підключитися до БД!\nПеревірте налаштування підключення.",
                "Помилка підключення",
                JOptionPane.ERROR_MESSAGE);
        }
        
        initComponents();
        
        // Завантажити початкові дані
        loadAllStudents();
    }
    
    /**
     * Ініціалізація компонентів GUI
     */
    private void initComponents() {
        // Головна панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Таблиця для відображення даних
        tableModel = new DefaultTableModel();
        dataTable = new JTable(tableModel);
        dataTable.setFillsViewportHeight(true);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane tableScrollPane = new JScrollPane(dataTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Результати запитів"));
        
        // Область для текстових результатів
        resultArea = new JTextArea(5, 20);
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder("Агрегатні результати"));
        
        // Вкладки з функціоналом
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📊 Сортування", createSortPanel());
        tabbedPane.addTab("🔍 Фільтр по діапазону", createFilterPanel());
        tabbedPane.addTab("🔗 JOIN таблиць", createJoinPanel());
        tabbedPane.addTab("📈 Агрегатні функції", createAggregatePanel());
        tabbedPane.addTab("📋 GROUP BY", createGroupByPanel());
        
        // Розміщення компонентів
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, 
                                                 tableScrollPane, resultScrollPane);
        bottomSplit.setResizeWeight(0.7);
        
        mainPanel.add(tabbedPane, BorderLayout.NORTH);
        mainPanel.add(bottomSplit, BorderLayout.CENTER);
        
        // Панель статусу
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel statusLabel = new JLabel("📁 БД: decanat_lab3 | 👤 Користувач: decanat_user");
        statusPanel.add(statusLabel);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * ВКЛАДКА 1: Сортування даних
     */
    private JPanel createSortPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Поле для сортування
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Сортувати по полю:"), gbc);
        
        gbc.gridx = 1;
        String[] sortFields = {"last_name", "first_name", "grade", "enrollment_year", "exam_date"};
        JComboBox<String> sortFieldCombo = new JComboBox<>(sortFields);
        panel.add(sortFieldCombo, gbc);
        
        // Порядок сортування
        gbc.gridx = 2;
        panel.add(new JLabel("Порядок:"), gbc);
        
        gbc.gridx = 3;
        String[] sortOrders = {"ASC (За зростанням)", "DESC (За спаданням)"};
        JComboBox<String> sortOrderCombo = new JComboBox<>(sortOrders);
        panel.add(sortOrderCombo, gbc);
        
        // Кнопка "Сортувати"
        gbc.gridx = 4;
        JButton sortButton = new JButton("🔄 Сортувати");
        sortButton.addActionListener(e -> {
            String field = (String) sortFieldCombo.getSelectedItem();
            String order = sortOrderCombo.getSelectedIndex() == 0 ? "ASC" : "DESC";
            sortData(field, order, false);
        });
        panel.add(sortButton, gbc);
        
        // Кнопка "Топ-10"
        gbc.gridx = 5;
        JButton top10Button = new JButton("🏆 Топ-10");
        top10Button.addActionListener(e -> {
            String field = (String) sortFieldCombo.getSelectedItem();
            String order = sortOrderCombo.getSelectedIndex() == 0 ? "ASC" : "DESC";
            sortData(field, order, true);
        });
        panel.add(top10Button, gbc);
        
        // Кнопка "Всі студенти"
        gbc.gridx = 6;
        JButton allStudentsButton = new JButton("👥 Всі студенти");
        allStudentsButton.addActionListener(e -> loadAllStudents());
        panel.add(allStudentsButton, gbc);
        
        return panel;
    }
    
    /**
     * ВКЛАДКА 2: Фільтрація по діапазону
     */
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Поле для фільтрації
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Фільтрувати по полю:"), gbc);
        
        gbc.gridx = 1;
        String[] filterFields = {"grade (оцінка)", "enrollment_year (рік)", "credit_hours (кредити)"};
        JComboBox<String> filterFieldCombo = new JComboBox<>(filterFields);
        panel.add(filterFieldCombo, gbc);
        
        // Від
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Від:"), gbc);
        
        gbc.gridx = 1;
        JTextField fromField = new JTextField("60", 10);
        panel.add(fromField, gbc);
        
        // До
        gbc.gridx = 2;
        panel.add(new JLabel("До:"), gbc);
        
        gbc.gridx = 3;
        JTextField toField = new JTextField("90", 10);
        panel.add(toField, gbc);
        
        // Кнопка "Фільтрувати"
        gbc.gridx = 4;
        JButton filterButton = new JButton("🔍 Фільтрувати");
        filterButton.addActionListener(e -> {
            String fieldWithDesc = (String) filterFieldCombo.getSelectedItem();
            String field = fieldWithDesc.split(" ")[0]; // Взяти тільки назву поля
            String from = fromField.getText();
            String to = toField.getText();
            filterByRange(field, from, to);
        });
        panel.add(filterButton, gbc);
        
        return panel;
    }
    
    /**
     * ВКЛАДКА 3: JOIN таблиць
     */
    private JPanel createJoinPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Оберіть тип JOIN:"), gbc);
        
        // Кнопки для різних JOIN
        gbc.gridy = 1;
        JButton studentGroupButton = new JButton("👥 Студенти + Групи");
        studentGroupButton.addActionListener(e -> joinStudentGroup());
        panel.add(studentGroupButton, gbc);
        
        gbc.gridy = 2;
        JButton fullJoinButton = new JButton("🔗 Повний JOIN (5 таблиць)");
        fullJoinButton.addActionListener(e -> joinAllTables());
        panel.add(fullJoinButton, gbc);
        
        gbc.gridy = 3;
        JButton studentExamsButton = new JButton("📝 Студенти + Іспити");
        studentExamsButton.addActionListener(e -> joinStudentExams());
        panel.add(studentExamsButton, gbc);
        
        return panel;
    }
    
    /**
     * ВКЛАДКА 4: Агрегатні функції
     */
    private JPanel createAggregatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Обрати таблицю:"), gbc);
        
        gbc.gridx = 1;
        String[] tables = {"Exam (оцінки)", "Subject (кредити)", "Student (роки)"};
        JComboBox<String> tableCombo = new JComboBox<>(tables);
        panel.add(tableCombo, gbc);
        
        // Кнопки агрегатних функцій
        gbc.gridx = 0; gbc.gridy = 1;
        JButton avgButton = new JButton("📊 Середнє (AVG)");
        avgButton.addActionListener(e -> calculateAggregate("AVG", tableCombo.getSelectedIndex()));
        panel.add(avgButton, gbc);
        
        gbc.gridx = 1;
        JButton minButton = new JButton("📉 Мінімум (MIN)");
        minButton.addActionListener(e -> calculateAggregate("MIN", tableCombo.getSelectedIndex()));
        panel.add(minButton, gbc);
        
        gbc.gridx = 2;
        JButton maxButton = new JButton("📈 Максимум (MAX)");
        maxButton.addActionListener(e -> calculateAggregate("MAX", tableCombo.getSelectedIndex()));
        panel.add(maxButton, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3;
        JButton allStatsButton = new JButton("📊 Всі статистики");
        allStatsButton.addActionListener(e -> calculateAllStatistics());
        panel.add(allStatsButton, gbc);
        
        return panel;
    }
    
    /**
     * ВКЛАДКА 5: GROUP BY з WHERE
     */
    private JPanel createGroupByPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Умова WHERE (оцінка >=):"), gbc);
        
        gbc.gridx = 1;
        JTextField whereField = new JTextField("75", 10);
        panel.add(whereField, gbc);
        
        // Кнопки GROUP BY запитів
        gbc.gridx = 0; gbc.gridy = 1;
        JButton groupByStudentButton = new JButton("👤 Середні оцінки студентів");
        groupByStudentButton.addActionListener(e -> {
            String condition = whereField.getText();
            groupByStudent(condition);
        });
        panel.add(groupByStudentButton, gbc);
        
        gbc.gridy = 2;
        JButton groupBySubjectButton = new JButton("📚 Середні оцінки по предметах");
        groupBySubjectButton.addActionListener(e -> {
            String condition = whereField.getText();
            groupBySubject(condition);
        });
        panel.add(groupBySubjectButton, gbc);
        
        gbc.gridy = 3;
        JButton groupByGroupButton = new JButton("🎓 Середні оцінки по групах");
        groupByGroupButton.addActionListener(e -> {
            String condition = whereField.getText();
            groupByGroup(condition);
        });
        panel.add(groupByGroupButton, gbc);
        
        return panel;
    }
    
    // ============================================================
    // МЕТОДИ РОБОТИ З ДАНИМИ
    // ============================================================
    
    /**
     * Завантажити всіх студентів
     */
    private void loadAllStudents() {
        String query = "SELECT student_id, last_name, first_name, middle_name, " +
                      "birth_date, enrollment_year, group_id, email, phone " +
                      "FROM Student ORDER BY last_name";
        executeQuery(query);
    }
    
    /**
     * Сортування даних
     */
    private void sortData(String field, String order, boolean top10) {
        String query = "";
        
        if (field.equals("grade") || field.equals("exam_date")) {
            // Сортування по полях з Exam
            query = "SELECT s.student_id, s.last_name, s.first_name, " +
                   "e.grade, e.exam_date, sub.subject_name " +
                   "FROM Student s " +
                   "JOIN Exam e ON s.student_id = e.student_id " +
                   "JOIN Subject sub ON e.subject_id = sub.subject_id " +
                   "ORDER BY " + field + " " + order;
        } else {
            // Сортування по полях Student
            query = "SELECT student_id, last_name, first_name, middle_name, " +
                   "birth_date, enrollment_year, group_id, email " +
                   "FROM Student " +
                   "ORDER BY " + field + " " + order;
        }
        
        if (top10) {
            query += " LIMIT 10";
        }
        
        executeQuery(query);
        resultArea.setText("Сортування по: " + field + " (" + order + ")" + 
                          (top10 ? " - Перші 10 записів" : ""));
    }
    
    /**
     * Фільтрація по діапазону
     */
    private void filterByRange(String field, String from, String to) {
        String query = "";
        
        if (field.equals("grade")) {
            query = "SELECT s.last_name, s.first_name, e.grade, sub.subject_name, e.exam_date " +
                   "FROM Student s " +
                   "JOIN Exam e ON s.student_id = e.student_id " +
                   "JOIN Subject sub ON e.subject_id = sub.subject_id " +
                   "WHERE e.grade BETWEEN " + from + " AND " + to +
                   " ORDER BY e.grade DESC";
        } else if (field.equals("enrollment_year")) {
            query = "SELECT student_id, last_name, first_name, enrollment_year, email " +
                   "FROM Student " +
                   "WHERE enrollment_year BETWEEN " + from + " AND " + to +
                   " ORDER BY enrollment_year";
        } else if (field.equals("credit_hours")) {
            query = "SELECT subject_id, subject_name, credit_hours, semester " +
                   "FROM Subject " +
                   "WHERE credit_hours BETWEEN " + from + " AND " + to +
                   " ORDER BY credit_hours DESC";
        }
        
        executeQuery(query);
        resultArea.setText("Фільтр: " + field + " від " + from + " до " + to);
    }
    
    /**
     * JOIN: Студенти + Групи
     */
    private void joinStudentGroup() {
        String query = "SELECT s.student_id, s.last_name, s.first_name, " +
                      "g.group_name, g.specialty, g.course " +
                      "FROM Student s " +
                      "INNER JOIN `Group` g ON s.group_id = g.group_id " +
                      "ORDER BY g.group_name, s.last_name";
        executeQuery(query);
        resultArea.setText("INNER JOIN: Student ⇔ Group");
    }
    
    /**
     * JOIN: Всі 5 таблиць
     */
    private void joinAllTables() {
        String query = "SELECT " +
                      "CONCAT(s.last_name, ' ', s.first_name) AS 'Студент', " +
                      "g.group_name AS 'Група', " +
                      "sub.subject_name AS 'Предмет', " +
                      "CONCAT(t.last_name, ' ', LEFT(t.first_name, 1), '.') AS 'Викладач', " +
                      "e.grade AS 'Оцінка', " +
                      "e.exam_date AS 'Дата' " +
                      "FROM Exam e " +
                      "JOIN Student s ON e.student_id = s.student_id " +
                      "JOIN `Group` g ON s.group_id = g.group_id " +
                      "JOIN Subject sub ON e.subject_id = sub.subject_id " +
                      "JOIN Teacher t ON e.teacher_id = t.teacher_id " +
                      "ORDER BY e.exam_date DESC " +
                      "LIMIT 20";
        executeQuery(query);
        resultArea.setText("INNER JOIN: Student ⇔ Group ⇔ Exam ⇔ Subject ⇔ Teacher (5 таблиць)");
    }
    
    /**
     * JOIN: Студенти + Іспити
     */
    private void joinStudentExams() {
        String query = "SELECT s.last_name, s.first_name, " +
                      "COUNT(e.exam_id) AS 'Іспитів', " +
                      "AVG(e.grade) AS 'Середня оцінка' " +
                      "FROM Student s " +
                      "LEFT JOIN Exam e ON s.student_id = e.student_id " +
                      "GROUP BY s.student_id, s.last_name, s.first_name " +
                      "ORDER BY AVG(e.grade) DESC";
        executeQuery(query);
        resultArea.setText("LEFT JOIN: Student ⇔ Exam (з підрахунком іспитів)");
    }
    
    /**
     * Обчислення агрегатних функцій
     */
    private void calculateAggregate(String function, int tableIndex) {
        String field = "";
        String tableName = "";
        
        switch (tableIndex) {
            case 0: // Exam
                field = "grade";
                tableName = "Exam";
                break;
            case 1: // Subject
                field = "credit_hours";
                tableName = "Subject";
                break;
            case 2: // Student
                field = "YEAR(birth_date)";
                tableName = "Student";
                break;
        }
        
        String query = "SELECT " + function + "(" + field + ") AS result FROM " + tableName;
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                double result = rs.getDouble("result");
                resultArea.setText(String.format("%s(%s) = %.2f", function, field, result));
            }
        } catch (SQLException e) {
            showError("Помилка обчислення", e);
        }
    }
    
    /**
     * Обчислити всі статистики
     */
    private void calculateAllStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== СТАТИСТИКА ПО ОЦІНКАХ ===\n");
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Статистика по оцінках
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) as cnt, AVG(grade) as avg, MIN(grade) as min, MAX(grade) as max FROM Exam"
            );
            if (rs.next()) {
                stats.append(String.format("Всього іспитів: %d\n", rs.getInt("cnt")));
                stats.append(String.format("Середня оцінка: %.2f\n", rs.getDouble("avg")));
                stats.append(String.format("Мінімальна оцінка: %.0f\n", rs.getDouble("min")));
                stats.append(String.format("Максимальна оцінка: %.0f\n", rs.getDouble("max")));
            }
            
            stats.append("\n=== СТАТИСТИКА ПО СТУДЕНТАХ ===\n");
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM Student");
            if (rs.next()) {
                stats.append(String.format("Всього студентів: %d\n", rs.getInt("cnt")));
            }
            
            stats.append("\n=== СТАТИСТИКА ПО ПРЕДМЕТАХ ===\n");
            rs = stmt.executeQuery(
                "SELECT COUNT(*) as cnt, AVG(credit_hours) as avg, SUM(credit_hours) as sum FROM Subject"
            );
            if (rs.next()) {
                stats.append(String.format("Всього предметів: %d\n", rs.getInt("cnt")));
                stats.append(String.format("Середня кількість кредитів: %.1f\n", rs.getDouble("avg")));
                stats.append(String.format("Загальна кількість кредитів: %d\n", rs.getInt("sum")));
            }
            
            resultArea.setText(stats.toString());
            
        } catch (SQLException e) {
            showError("Помилка обчислення статистики", e);
        }
    }
    
    /**
     * GROUP BY: по студентах
     */
    private void groupByStudent(String minGrade) {
        String query = "SELECT s.last_name AS 'Прізвище', s.first_name AS 'Ім''я', " +
                      "COUNT(e.exam_id) AS 'Іспитів', " +
                      "AVG(e.grade) AS 'Середня оцінка', " +
                      "MIN(e.grade) AS 'Мін', " +
                      "MAX(e.grade) AS 'Макс' " +
                      "FROM Student s " +
                      "JOIN Exam e ON s.student_id = e.student_id " +
                      "WHERE e.grade >= " + minGrade + " " +
                      "GROUP BY s.student_id, s.last_name, s.first_name " +
                      "HAVING COUNT(e.exam_id) > 0 " +
                      "ORDER BY AVG(e.grade) DESC";
        executeQuery(query);
        resultArea.setText("GROUP BY студент з WHERE grade >= " + minGrade);
    }
    
    /**
     * GROUP BY: по предметах
     */
    private void groupBySubject(String minGrade) {
        String query = "SELECT sub.subject_name AS 'Предмет', " +
                      "COUNT(e.exam_id) AS 'Іспитів', " +
                      "AVG(e.grade) AS 'Середня оцінка', " +
                      "MIN(e.grade) AS 'Мін', " +
                      "MAX(e.grade) AS 'Макс' " +
                      "FROM Subject sub " +
                      "JOIN Exam e ON sub.subject_id = e.subject_id " +
                      "WHERE e.grade >= " + minGrade + " " +
                      "GROUP BY sub.subject_id, sub.subject_name " +
                      "ORDER BY AVG(e.grade) DESC";
        executeQuery(query);
        resultArea.setText("GROUP BY предмет з WHERE grade >= " + minGrade);
    }
    
    /**
     * GROUP BY: по групах
     */
    private void groupByGroup(String minGrade) {
        String query = "SELECT g.group_name AS 'Група', " +
                      "COUNT(DISTINCT s.student_id) AS 'Студентів', " +
                      "COUNT(e.exam_id) AS 'Іспитів', " +
                      "AVG(e.grade) AS 'Середня оцінка' " +
                      "FROM `Group` g " +
                      "JOIN Student s ON g.group_id = s.group_id " +
                      "JOIN Exam e ON s.student_id = e.student_id " +
                      "WHERE e.grade >= " + minGrade + " " +
                      "GROUP BY g.group_id, g.group_name " +
                      "ORDER BY AVG(e.grade) DESC";
        executeQuery(query);
        resultArea.setText("GROUP BY група з WHERE grade >= " + minGrade);
    }
    
    /**
     * Виконати SQL запит та показати результат в таблиці
     */
    private void executeQuery(String query) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            // Очистити попередню модель
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            
            // Отримати метадані
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Додати назви колонок
            for (int i = 1; i <= columnCount; i++) {
                tableModel.addColumn(metaData.getColumnLabel(i));
            }
            
            // Додати рядки даних
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                tableModel.addRow(row);
            }
            
            // Показати кількість результатів
            int rowCount = tableModel.getRowCount();
            System.out.println("✅ Запит виконано. Знайдено записів: " + rowCount);
            
        } catch (SQLException e) {
            showError("Помилка виконання запиту", e);
        }
    }
    
    /**
     * Показати помилку
     */
    private void showError(String title, Exception e) {
        System.err.println("❌ " + title + ": " + e.getMessage());
        e.printStackTrace();
        JOptionPane.showMessageDialog(this,
            title + ":\n" + e.getMessage(),
            "Помилка",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Головний метод запуску додатку
     */
    public static void main(String[] args) {
        // Встановити Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Запустити GUI в EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            DecanatApp app = new DecanatApp();
            app.setVisible(true);
        });
    }
}
