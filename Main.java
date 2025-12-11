import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            POSFrame frame = new POSFrame();
            frame.setVisible(true);
        });
    }
}
