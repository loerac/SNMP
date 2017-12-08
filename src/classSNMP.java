// Importing tools for error detection and vectors
import java.net.InetAddress;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Vector;

// Importing tools for SNMP
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

// Importing tools for JFrame
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

public class classSNMP implements CommandResponder {
	// Creating a textArea to variable
	// Used for set/get/trap data
	private static JTextArea ipTA;

	// Creating a constructor
	public static void main(String[] args) {
		new classSNMP().run();
		controlPanel();
	}

	// Initiallize the threads
	private void run() {
		try {
			ThreadPool threadPool = ThreadPool.create("Trap", 10);
			MultiThreadedMessageDispatcher dispatcher = new MultiThreadedMessageDispatcher(threadPool, new MessageDispatcherImpl());
			
			Address listenAddress = GenericAddress.parse(System.getProperty("snmp4j.listenAddress", "udp:0.0.0.0/162"));
			TransportMapping<?> transport;
			if (listenAddress instanceof UdpAddress) {
				transport = new DefaultUdpTransportMapping((UdpAddress) listenAddress);
			} else {
				transport = new DefaultTcpTransportMapping((TcpAddress) listenAddress);
			}
			
			Snmp snmp = new Snmp(dispatcher, transport);
			snmp.getMessageDispatcher().addMessageProcessingModel(new MPv1());
			snmp.getMessageDispatcher().addMessageProcessingModel(new MPv2c());
			snmp.listen();
			snmp.addCommandResponder(this);
		} catch (Exception e) { e.printStackTrace(); }
	}

	// Creating a CommunityTarget function
	// After writing one, I decided to 
	// create a function to do it for me
	// since I have to make one for set and get
	public static CommunityTarget createCT(String addr, String port, String comm) {
		CommunityTarget target = new CommunityTarget();		// A object that will be returned
		target.setCommunity(new OctetString(comm));		// setting the community with comm
		target.setVersion(SnmpConstants.version2c);		// Setting the type of version (1, 2c, 3)
		target.setAddress(new UdpAddress(addr + "/" + port));	// Setting the address with the port number to target
		target.setRetries(2);
		target.setTimeout(5000);
		return target;
	}

	// Creating a PDU function
	// After writing one, I decided to
	// create a function to do it for me
	// since I have to make one for set and get
	public static PDU createPDU(String oid, String value, int sg) {
		PDU pdu = new PDU();					// A object that will be returned
		if(sg == 0) {						// sg stands for set-get, make sg = 0 for get
			pdu.add(new VariableBinding(new OID(oid)));	// Binding the oid and adding it to the object
			pdu.setType(PDU.GET);				// Setting the object to PDU.GET
		}
		else {							// Make sg != 0 for set
			pdu.add(new VariableBinding(new OID(oid), new OctetString(value)));	// Binding the oid and value then adding it to the PDU object
			pdu.setType(PDU.SET);				// Setting the object to PDU.SET
		}
		pdu.setRequestID(new Integer32(1));
		return pdu;
	}
	
	// Function to set a value to a writable OID
	public String setSNMP(String addr, String comm, String oid, String value) {
		// Creating a variable to return back to the user
		// If there is a fault in setting the OID to the new value
		// Send back this message
		String resp = "SNMP set request = FAILED";
		
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			transport.listen();

			// Creating an CommTar and PDU object
			CommunityTarget target = createCT(addr, "161", comm);
			PDU pdu = createPDU(oid, value, 1);

			Snmp snmp = new Snmp(transport);
			ResponseEvent event = snmp.set(pdu, target);	// Setting the snmp with the pdu and target object
			if(event != null) {
				resp = "SNMP set request = " + event.getRequest().getVariableBindings() + '\n';
				PDU pduResp = event.getResponse();	// Setting the new value to the OID
				resp += "Response = " + pduResp + '\n';

				if(pduResp != null)
					resp += "Set status is: " + pduResp.getErrorStatusText() + '\n';
			}
			snmp.close();
		} catch (Exception e) { e.printStackTrace(); }
		return resp;
	}
	
	// Function to get a value to a readable OID
	public String getSNMP(String addr, String comm, String oid) {
		// Creating variable's to return back
		// If there is a failure in getting back the information
		// Send back these values
		String str = "FAILED";
		String length = "NULL";
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			transport.listen();

			// Creating a value for CommTar and PDU
			CommunityTarget target = createCT(addr, "161", comm);
			PDU pdu = createPDU(oid, "", 0);

			Snmp snmp = new Snmp(transport);
			ResponseEvent resp = snmp.get(pdu, target);	// Getting the information with the PDU and CommTar info

			if(resp != null) {
				if(resp.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
					str = "SUCCESS";
					PDU pduResp = resp.getResponse();	// Setting a value to a PDU then
					length = pduResp.getVariableBindings().firstElement().toString();	// Retrieve it's value
					
					if(length.contains("=")) {
						int len = length.indexOf("=");
						length = length.substring(len + 1, length.length());	// Grabbing the value of the OID
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

	// A function to trap OID's
	public void processPdu(CommandResponderEvent event) {
		//StringBuffer msg = new StringBuffer();
		//msg.append(event.toString());
		String message = "";// = event.toString();
		String temp = event.toString();
		
		// Creating a VariableBinding vector that will have the trap
		Vector<? extends VariableBinding> varBinds = event.getPDU().getVariableBindings();

		// If there was a trap caught and added to the vector,
		// it will then change 
		if (varBinds != null && !varBinds.isEmpty()) {
			Iterator<? extends VariableBinding> varIter = varBinds.iterator();
			while (varIter.hasNext()) {
				VariableBinding var = varIter.next();
				//msg.append(var.toString()).append("\n");
				temp += var.toString() + "\n";
			}

			// Making the output the the text area look pretty
			for(char c : temp.toCharArray()) {
				if(c == ',' || c == ';')
					message += "\n";	
				else
					message += c;
			}
		};
		ipTA.append("================== Trap ==================\nMessage Received: " + message + '\n');
	}

	// Creating a panel for all the componets in my frame
	public static void controlPanel() {
		JFrame frame = new JFrame();
		frame.setSize(750,500);
		frame.setTitle("Loera SNMP");
		frame.setResizable(false);
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
					ipTA.append("================== Writing =================\n" + objSNMP.setSNMP(hostTF.getText(), commTF.getText(), oidTF.getText(), valueTF.getText()) + '\n');
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
				valueTF.setText("Banana");
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
