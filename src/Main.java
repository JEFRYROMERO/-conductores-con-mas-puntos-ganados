import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class Main {
    private Connection conn;
    private JFrame frame;
    private JComboBox<String> comboBox;
    private JTable table;
    private DefaultTableModel tableModel;
    private JPanel progressPanel;
    private JProgressBar progressBar;

    public Main() {
        // Establecer conexión a la base de datos PostgreSQL
        connectDB();

        // Crear la interfaz gráfica
        createGUI();
    }

    private void connectDB() {
        try {
            String url = "jdbc:postgresql://localhost:5432/prueba2";
            String user = "postgres";
            String password = "elmejorcr7";
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Conexión establecida con PostgreSQL.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        frame = new JFrame("CONDUCTORES");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        // Panel principal con borde y layout
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(255, 255, 255)); // Fondo blanco

        // Encabezado personalizado
        JLabel headerLabel = new JLabel("CONDUCTORES", JLabel.CENTER);
        headerLabel.setFont(new Font("Tahoma", Font.BOLD, 28));
        headerLabel.setForeground(new Color(0, 102, 204)); // Azul oscuro
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Panel para el combo box y la barra de progreso
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(255, 255, 255)); // Fondo blanco

        comboBox = new JComboBox<>();
        populateComboBox();
        comboBox.addActionListener(e -> updateTableInBackground());
        comboBox.setBackground(new Color(204, 229, 255)); // Azul muy claro
        comboBox.setForeground(Color.BLACK);
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(0, 102, 204), 2));

        topPanel.add(comboBox, BorderLayout.WEST);

        // Barra de progreso sin indicador de porcentaje
        progressPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // Barra de progreso indeterminada
        progressBar.setForeground(new Color(0, 102, 204)); // Azul oscuro
        progressPanel.add(progressBar, BorderLayout.CENTER);
        topPanel.add(progressPanel, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Panel para la tabla
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(255, 255, 255)); // Fondo blanco

        // Tabla para mostrar los datos de los puntos de los conductores
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        // Estilo de tabla
        table.setBackground(new Color(204, 229, 255)); // Azul claro
        table.setForeground(Color.BLACK);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(30);

        // Encabezados de la tabla
        table.getTableHeader().setBackground(new Color(0, 102, 204)); // Azul oscuro
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Tahoma", Font.BOLD, 16));

        // Centrar el contenido de las celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        centerPanel.add(scrollPane);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);
    }

    private void populateComboBox() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT year FROM races ORDER BY year DESC");
            while (rs.next()) {
                comboBox.addItem(rs.getString("year"));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTableInBackground() {
        String selectedYear = (String) comboBox.getSelectedItem();
        if (selectedYear != null) {
            // Crear un SwingWorker para ejecutar la consulta en segundo plano
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        // Consulta para obtener los conductores y sus puntos totales para el año seleccionado
                        String query = "SELECT d.forename || ' ' || d.surname AS driver_name, SUM(ds.points) AS total_points " +
                                "FROM drivers d " +
                                "JOIN driver_standings ds ON d.driver_id = ds.driver_id " +
                                "JOIN races r ON ds.race_id = r.race_id " +
                                "WHERE r.year = ? " +
                                "GROUP BY driver_name " +
                                "ORDER BY total_points DESC";

                        PreparedStatement pstmt = conn.prepareStatement(query);
                        pstmt.setInt(1, Integer.parseInt(selectedYear));
                        ResultSet rs = pstmt.executeQuery();

                        // Obtener columnas
                        Vector<String> columnNames = new Vector<>();
                        columnNames.add("CONDUCTORES");
                        columnNames.add("TOTAL PUNTOS");

                        // Obtener filas
                        Vector<Vector<Object>> data = new Vector<>();
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            row.add(rs.getString("driver_name"));
                            row.add(rs.getDouble("total_points"));
                            data.add(row);
                        }

                        // Actualizar modelo de la tabla en el hilo de eventos de Swing
                        SwingUtilities.invokeLater(() -> {
                            tableModel.setDataVector(data, columnNames);
                            progressBar.setIndeterminate(false); // Detener el estado indeterminado
                        });

                        rs.close();
                        pstmt.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };

            // Iniciar el SwingWorker y mostrar la barra de progreso
            progressBar.setIndeterminate(true); // Mostrar una barra de progreso indeterminada
            worker.execute();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
