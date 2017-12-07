import java.net.InetAddress;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Vector;

import org.snmp4j.CommunityTarget;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class classSNMP implements CommandResponder {
	private static JTextArea ipTA;
	private int n = 0;
	private long start = -1;
	private MultiThreadedMessageDispatcher dispatcher;
	private Snmp snmp = null;
	private Address listenAddress;
	private ThreadPool threadPool;

	public classSNMP() {}

	public static void main(String[] args) {
		new classSNMP().run();
		controlPanel();
	}

	private void run() {
		try {
			init();
			snmp.addCommandResponder(this);
		} catch (Exception e) { e.printStackTrace(); }
	}

	public static CommunityTarget createCT(String addr, String port, String comm) {
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(comm));
		target.setVersion(SnmpConstants.version2c);
		target.setAddress(new UdpAddress(addr + "/" + port));
		target.setRetries(2);
		target.setTimeout(5000);
		return target;
	}
	
	public String setSNMP(String addr, String comm, String oid, String value) {
		String resp = "SNMP set request = FAILED";
		
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			transport.listen();

			CommunityTarget target = createCT(addr, "161", comm);
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(new OID(oid), new OctetString(value)));
			pdu.setType(PDU.SET);
			pdu.setRequestID(new Integer32(1));

			Snmp snmp = new Snmp(transport);
			ResponseEvent event = snmp.set(pdu, target);
			if(event != null) {
				resp = "SNMP set request = " + event.getRequest().getVariableBindings() + '\n';
				PDU pduResp = event.getResponse();
				resp += "Response = " + pduResp + '\n';

				if(pduResp != null)
					resp += "Set status is: " + pduResp.getErrorStatusText() + '\n';
			}
			snmp.close();
		} catch (Exception e) { e.printStackTrace(); }
		return resp;
	}
	
	public String getSNMP(String addr, String comm, String oid) {
		String str = "FAILED";
		String length = "NULL";
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			transport.listen();

			CommunityTarget target = createCT(addr, "161", comm);
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(new OID(oid)));
			pdu.setRequestID(new Integer32(1));
			pdu.setType(PDU.GET);

			Snmp snmp = new Snmp(transport);
			ResponseEvent resp = snmp.get(pdu, target);

			if(resp != null) {
				if(resp.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
					str = "SUCCESS";
					PDU pduResp = resp.getResponse();
					length = pduResp.getVariableBindings().firstElement().toString();
					
					if(length.contains("=")) {
						int len = length.indexOf("=");
						length = length.substring(len + 1, length.length());
					} else 
						return "FAILED: Response = NULL\n";
				} else {
					System.out.println("Feeling like a timeout occured");
					snmp.close();
				}
			}
		} catch (Exception e) { e.printStackTrace(); }
		return "Address: " + addr + '\n' + str + ": response = " + length + '\n';
	}

private void init() throws UnknownHostException, IOException {
		threadPool = ThreadPool.create("Trap", 10);
		dispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());
		listenAddress = GenericAddress.parse(System.getProperty("snmp4j.listenAddress", "udp:0.0.0.0/162"));
		TransportMapping<?> transport;
		if (listenAddress instanceof UdpAddress) {
			transport = new DefaultUdpTransportMapping((UdpAddress) listenAddress);
		} else {
			transport = new DefaultTcpTransportMapping((TcpAddress) listenAddress);
		}

		snmp = new Snmp(dispatcher, transport);
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
		snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
		snmp.listen();
	}

	public void processPdu(CommandResponderEvent event) {
		if (start < 0) {
			start = System.currentTimeMillis() - 1;
		}
		n++;
		if ((n % 100 == 1)) {
			//System.out.println("Processed " + (n / (double) (System.currentTimeMillis() - start)) * 1000 + "/s, total=" + n);
		}

		StringBuffer msg = new StringBuffer();
		msg.append(event.toString()).append("\n");
		Vector<? extends VariableBinding> varBinds = event.getPDU().getVariableBindings();
		if (varBinds != null && !varBinds.isEmpty()) {
			Iterator<? extends VariableBinding> varIter = varBinds.iterator();
			while (varIter.hasNext()) {
				VariableBinding var = varIter.next();
				msg.append(var.toString()).append("\n");
			}
		};
		String message = "";
		String temp = msg.toString();
		for(char c : temp.toCharArray()) {
			if(c == ',' || c == ';') {
				message += c + "\n";
			}
			message += c;
		}
		ipTA.append("================== Trap ==================\nMessage Received: " + message + '\n');
	}

	public static void controlPanel() {
		JFrame frame = new JFrame();
		frame.setSize(750,500);
		frame.setTitle("Loera SNMP");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		classSNMP objSNMP = new classSNMP();
		// Host Label
		JLabel hostLabel = new JLabel();
		hostLabel.setText("Host Address: ");
		hostLabel.setLocation(10, -15);
		hostLabel.setSize(90, 50);

		// Host text field
		JTextField hostTF = new JTextField();
		hostTF.setBounds(10, 20, 110, 20);

		// Community label
		JLabel commLabel = new JLabel();
		commLabel.setText("Community ID:");
		commLabel.setLocation(130, -15);
		commLabel.setSize(90, 50);
	
		// Community text field
		JTextField commTF = new JTextField();
		commTF.setBounds(130, 20, 110, 20);

		// Object ID label
		JLabel oidLabel = new JLabel();
		oidLabel.setText("Object ID:");
		oidLabel.setLocation(250, -15);
		oidLabel.setSize(90, 50);

		// Object ID text field
		JTextField oidTF = new JTextField();
		oidTF.setBounds(250, 20, 110, 20);

		// Value label
		JLabel valueLabel = new JLabel();
		valueLabel.setText("Value:");
		valueLabel.setLocation(370, -15);
		valueLabel.setSize(90, 50);

		// Value text field
		JTextField valueTF = new JTextField();
		valueTF.setBounds(370, 20, 110, 20);
 
		// IP Text Area
		ipTA = new JTextArea();
		ipTA.setEditable(false);
		JScrollPane scroll = new JScrollPane(ipTA);
		scroll.setBounds(10, 120, 720, 335);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		// write Button a.k.a. setSNMP(strAdr, comm, strOID, value)
		JButton writeButt = new JButton();
		writeButt.setText("Write/Set info");
		writeButt.setBounds(10, 60, 125, 50);
		writeButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if((!hostTF.getText().equals("")) && (!commTF.getText().equals("")) && (!oidTF.getText().equals("")) && (!valueTF.getText().equals(""))) {
					ipTA.append("=============== Writing ===============\n" + objSNMP.setSNMP(hostTF.getText(), commTF.getText(), oidTF.getText(), valueTF.getText()) + '\n');
				} else 
					ipTA.append("=======================================\nERROR: ENTER INFO IN <HOST> <COMMUNITY> <OID> <VALUE>\n\n");
			}
		});

		// read Button a.k.a. getSNMP(strAdr, comm, strOID)
		JButton readButt = new JButton();
		readButt.setText("Read/Get info");
		readButt.setBounds(145, 60, 125, 50);
		readButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if((!hostTF.getText().equals("")) && (!commTF.getText().equals("")) && (!oidTF.getText().equals(""))) {
					ipTA.append("================== Read ==================\n" + objSNMP.getSNMP(hostTF.getText(), commTF.getText(), oidTF.getText()) + '\n');
				} else 
					ipTA.append("=======================================\nERROR: ENTER INFO IN <HOST> <COMMUNITY> <OID>\n\n");
			}
		});

		// Default button
		JButton defButt = new JButton();
		defButt.setText("Default");
		defButt.setBounds(280, 60, 125, 50);
		defButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				hostTF.setText("127.0.0.1");
				commTF.setText("public");
				oidTF.setText(".1.3.6.1.2.1.1.5.0");
			}
		});
 
		// Close button
		JButton close = new JButton();
		close.setText("Exit");
		close.setBounds(415, 60, 125, 50);
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

		JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.add(hostLabel);
		panel.add(hostTF);
		panel.add(commLabel);
		panel.add(commTF);
		panel.add(oidLabel);
		panel.add(oidTF);
		panel.add(valueLabel);
		panel.add(valueTF);
		panel.add(writeButt);
		panel.add(readButt);
		panel.add(defButt);
		panel.add(close);
		panel.add(scroll);
		frame.add(panel);
		frame.setVisible(true);
	}
}
