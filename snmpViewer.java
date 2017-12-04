import javax.swing.JFrame;

public class snmpViewer {
	public static void main(String[] args) {
		// Using JFrame to print all the java objects on the panel
		JFrame frame = new snmpPanel();
		frame.setSize(500,500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("snmpViewer");
		frame.setVisible(true);
	}
}
