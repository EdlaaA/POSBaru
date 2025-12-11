import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;

public class POSFrame extends JFrame {
    private DefaultListModel<Product> productListModel = new DefaultListModel<>();
    private JList<Product> productList;
    private DefaultTableModel cartTableModel;
    private java.util.List<CartItem> cart = new ArrayList<>();
    private JLabel totalLabel;

    private final Path historyFile = Paths.get(System.getProperty("user.home"), "pos_sales_history.csv");

    public POSFrame() {
        setTitle("Simple POS - NetBeans (Swing)");
        setSize(900,600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initProducts();
        initUI();
    }

    private void initProducts() {
        // Sample products - you can modify or load from file.
        productListModel.addElement(new Product("P001","Espresso", 6.00));
        productListModel.addElement(new Product("P002","Latte", 8.50));
        productListModel.addElement(new Product("P003","Cappuccino", 8.00));
        productListModel.addElement(new Product("P004","Bottled Water", 2.00));
        productListModel.addElement(new Product("P005","Chocolate Bar", 3.50));
    }

    private void initUI() {
        setLayout(new BorderLayout(8,8));
        JPanel left = new JPanel(new BorderLayout(6,6));
        left.setBorder(BorderFactory.createTitledBorder("Products"));
        productList = new JList<>(productListModel);
        productList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane pscroll = new JScrollPane(productList);
        left.add(pscroll, BorderLayout.CENTER);

        JButton addBtn = new JButton("Add to Cart");
        addBtn.addActionListener(e -> addSelectedProductToCart());
        left.add(addBtn, BorderLayout.SOUTH);

        add(left, BorderLayout.WEST);
        left.setPreferredSize(new Dimension(300, getHeight()));

        // Cart table
        String[] cols = {"Product ID","Name","Price","Qty","Total"};
        cartTableModel = new DefaultTableModel(cols,0) {
            public boolean isCellEditable(int row,int col){ return col==3; } // allow qty edit
        };
        JTable cartTable = new JTable(cartTableModel);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(50);
        cartTable.getModel().addTableModelListener(e -> {
            if (e.getType()==TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (col==3) {
                    try {
                        int qty = Integer.parseInt(cartTableModel.getValueAt(row,3).toString());
                        if (qty<=0) { cartTableModel.removeRow(row); cart.remove(row); }
                        else { cart.get(row).setQty(qty); cartTableModel.setValueAt(String.format("%.2f", cart.get(row).getTotal()), row, 4); }
                        updateTotal();
                    } catch(Exception ex) { /* ignore parse errors */ }
                }
            }
        });

        JPanel center = new JPanel(new BorderLayout(6,6));
        center.setBorder(BorderFactory.createTitledBorder("Cart"));
        center.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(6,6));
        totalLabel = new JLabel("Total: RM 0.00");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 16f));
        bottom.add(totalLabel, BorderLayout.WEST);

        JPanel controls = new JPanel();
        JButton removeBtn = new JButton("Remove Selected");
        removeBtn.addActionListener(e -> {
            int r = cartTable.getSelectedRow();
            if (r>=0) { cartTableModel.removeRow(r); cart.remove(r); updateTotal(); }
        });
        JButton clearBtn = new JButton("Clear Cart");
        clearBtn.addActionListener(e -> { cart.clear(); cartTableModel.setRowCount(0); updateTotal(); });
        JButton checkoutBtn = new JButton("Checkout");
        checkoutBtn.addActionListener(e -> doCheckout());
        JButton historyBtn = new JButton("Sales History");
        historyBtn.addActionListener(e -> showSalesHistory());
        controls.add(removeBtn);
        controls.add(clearBtn);
        controls.add(checkoutBtn);
        controls.add(historyBtn);

        bottom.add(controls, BorderLayout.EAST);
        center.add(bottom, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    private void addSelectedProductToCart() {
        Product p = productList.getSelectedValue();
        if (p==null) { JOptionPane.showMessageDialog(this, "Select a product first."); return; }
        // If exists, increment qty
        for (int i=0;i<cart.size();i++) {
            if (cart.get(i).getProduct().getId().equals(p.getId())) {
                cart.get(i).setQty(cart.get(i).getQty()+1);
                cartTableModel.setValueAt(cart.get(i).getQty(), i, 3);
                cartTableModel.setValueAt(String.format("%.2f", cart.get(i).getTotal()), i, 4);
                updateTotal();
                return;
            }
        }
        CartItem ci = new CartItem(p,1);
        cart.add(ci);
        cartTableModel.addRow(new Object[]{p.getId(), p.getName(), String.format("%.2f", p.getPrice()), ci.getQty(), String.format("%.2f", ci.getTotal())});
        updateTotal();
    }

    private void updateTotal() {
        double total=0;
        for (CartItem c:cart) total += c.getTotal();
        totalLabel.setText("Total: RM " + String.format("%.2f", total));
    }

    private void doCheckout() {
        if (cart.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty."); return; }
        double total=0;
        for (CartItem c:cart) total += c.getTotal();
        String paidStr = JOptionPane.showInputDialog(this, "Total: RM " + String.format("%.2f", total) + "\nEnter amount paid (RM):", "");
        if (paidStr==null) return;
        try {
            double paid = Double.parseDouble(paidStr);
            if (paid < total) { JOptionPane.showMessageDialog(this, "Insufficient payment."); return; }
            double change = paid - total;
            // Save sale
            saveSale(total, paid, change);
            // Show receipt
            ReceiptDialog rd = new ReceiptDialog(this, cart, total, paid, change);
            rd.setVisible(true);
            // Clear cart
            cart.clear();
            cartTableModel.setRowCount(0);
            updateTotal();
        } catch(NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number.");
        } catch(IOException ioe) {
            JOptionPane.showMessageDialog(this, "Failed to save sale: " + ioe.getMessage());
        }
    }

    private void saveSale(double total, double paid, double change) throws IOException {
        boolean exists = Files.exists(historyFile);
        try (BufferedWriter bw = Files.newBufferedWriter(historyFile, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
            if (!exists) {
                bw.write("timestamp,total,paid,change,items");
                bw.newLine();
            }
            StringBuilder items = new StringBuilder();
            for (CartItem c:cart) {
                if (items.length()>0) items.append(" | ");
                items.append(c.getProduct().getName()).append("x").append(c.getQty());
            }
            String line = String.format("%s,%.2f,%.2f,%.2f,\"%s\"", new java.util.Date().toString(), total, paid, change, items.toString());
            bw.write(line);
            bw.newLine();
        }
    }

    private void showSalesHistory() {
        try {
            if (!Files.exists(historyFile)) { JOptionPane.showMessageDialog(this, "No sales history found."); return; }
            java.util.List<String> lines = Files.readAllLines(historyFile);
            JTextArea ta = new JTextArea();
            for (String l:lines) ta.append(l + "\n");
            ta.setCaretPosition(0);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(600,400));
            JOptionPane.showMessageDialog(this, sp, "Sales History (CSV)", JOptionPane.INFORMATION_MESSAGE);
        } catch(IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to read history: " + ex.getMessage());
        }
    }
}
