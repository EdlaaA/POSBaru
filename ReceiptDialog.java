import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ReceiptDialog extends JDialog {
    public ReceiptDialog(Frame owner, List<CartItem> cart, double total, double paid, double change) {
        super(owner, "Receipt", true);
        setSize(400,500);
        setLocationRelativeTo(owner);
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        StringBuilder sb = new StringBuilder();
        sb.append("RECEIPT\n");
        sb.append("------------------------------\n");
        for (CartItem c: cart) {
            sb.append(String.format("%s x%d  RM %.2f\n", c.getProduct().getName(), c.getQty(), c.getTotal()));
        }
        sb.append("------------------------------\n");
        sb.append(String.format("Total: RM %.2f\n", total));
        sb.append(String.format("Paid:  RM %.2f\n", paid));
        sb.append(String.format("Change: RM %.2f\n", change));
        sb.append("\nThank you!");
        ta.setText(sb.toString());
        add(new JScrollPane(ta), BorderLayout.CENTER);
        JButton ok = new JButton("Close");
        ok.addActionListener(e -> dispose());
        JPanel p = new JPanel();
        p.add(ok);
        add(p, BorderLayout.SOUTH);
    }
}
