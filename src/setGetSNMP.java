import java.net.InetAddress;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class snmpPanel {
	// Variables for the SNMP	
	public static final String READ_COMMUNITY = "public";
	public static final String WRITE_COMMUNITY = "private";
	public static final String temp = ".1.3.6.1.2.1.1.5.0";
	public static final String OID_UPS_OUTLET_GROUP1 = temp;
	public static final String OID_UPS_BATTERY_CAPACITY = temp;
   
	public static void main(String[] args) {
		controlPanel();
		/*try {
			String strIPAddress = "127.0.0.1";
			snmpPanel objSNMP = new snmpPanel();

			// Get Basic state of UPS
			System.out.println("======================================");
			String batteryCap = objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, OID_UPS_BATTERY_CAPACITY);
			System.out.println(batteryCap);

			System.out.println("======================================");
			// Set Value = 2 to turn OFF UPS OUTLET Group1
			// Set Value = 1 to turn ON  UPS OUTLET Group1
			String value = "Banana";
			objSNMP.snmpSet(strIPAddress, WRITE_COMMUNITY, OID_UPS_OUTLET_GROUP1, value);
			System.out.println("======================================");
       
			batteryCap = objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, OID_UPS_BATTERY_CAPACITY);
			System.out.println(batteryCap);
			System.out.println("======================================");
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	/*
	* The following code valid only SNMP version2c
	* This method is very useful to set a parameter on remote device
	*/
	public String snmpSet(String strAddress, String community, String strOID, String value) {
		String response = "SNMP set request = FAILED";
		Address targetAddress = GenericAddress.parse(strAddress + "/161");
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			transport.listen();
         
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString(community));
			target.setVersion(SnmpConstants.version2c);
			target.setAddress(targetAddress);
			target.setRetries(2);
			target.setTimeout(5000);
         
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(new OID(strOID), new OctetString(value)));
			pdu.setType(PDU.SET);
			pdu.setRequestID(new Integer32(1));
         
			Snmp snmp = new Snmp(transport);
			ResponseEvent event = snmp.set(pdu, target);
			if(event != null) {
				response = "SNMP set request = " + event.getRequest().getVariableBindings() + '\n';
				PDU strResponse = event.getResponse();
				response += "Response = " + strResponse + '\n';
				
				if(strResponse != null) {
					String result = strResponse.getErrorStatusText();
					response += "Set status is: " + result + '\n';
					//System.out.println(response);
				}
			}
			snmp.close();
		} catch (Exception e) {
			e.printStackTrace();
		} return response;
	}

	/*
	* The code code is valid only SNMP version2c
	* SNMPGet method return response for given OID from the Device
	*/
	public String snmpGet(String strAddress, String comm, String strOID) {
		String str = "FAILED";
		String length="NULL";
		try {
			Address targetaddress = new UdpAddress(strAddress + "/161");
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			transport.listen();
         
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString(comm));
			target.setVersion(SnmpConstants.version2c);
			target.setAddress(targetaddress);
			target.setRetries(2);
			target.setTimeout(5000);
         
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(new OID(strOID)));
			pdu.setRequestID(new Integer32(1));
			pdu.setType(PDU.GET);
         
			Snmp snmp = new Snmp(transport);
			ResponseEvent response = snmp.get(pdu,target);
         
			if(response != null) {
				if(response.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
					str = "SUCCESS";
					PDU pduresponse=response.getResponse();
					length = pduresponse.getVariableBindings().firstElement().toString();
				
					if(length.contains("=")) {
						int len = length.indexOf("=");
						length=length.substring(len+1, length.length());
					} else {
						str = "FAILED: Response = NULL\n";
						//System.out.println(str);
						return str;
					}
				} else {
					System.out.println("Feeling like a TimeOut occured ");
					snmp.close();
				}
			} 
		} catch(Exception e) { e.printStackTrace(); }
		str = "Address: " + strAddress + '\n' + str + ": response = " + length + '\n';
		return str;
	}
	
	public static void controlPanel() {
		snmpPanel obj = new snmpPanel();	
		JFrame frame = new JFrame();
		frame.setSize(750, 500);
		frame.setTitle("SNMP Boi");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     
		frame.setLayout(new BorderLayout());
		JLabel panel = new JLabel(new ImageIcon("plz_no_crash.jpg"));

		// Host Label
		JLabel hostLabel = new JLabel();
		hostLabel.setText("Host Address: ");
		hostLabel.setForeground(Color.white);
		hostLabel.setLocation(10, -15);
		hostLabel.setSize(90, 50);

		// Host text field
		JTextField hostTF = new JTextField();
		hostTF.setBounds(10, 20, 110, 20);

		// Community label
		JLabel commLabel = new JLabel();
		commLabel.setText("Community ID:");
		commLabel.setForeground(Color.white);
		commLabel.setLocation(130, -15);
		commLabel.setSize(90, 50);
	
		// Community text field
		JTextField commTF = new JTextField();
		commTF.setBounds(130, 20, 110, 20);

		// Object ID label
		JLabel oidLabel = new JLabel();
		oidLabel.setText("Object ID:");
		oidLabel.setForeground(Color.white);
		oidLabel.setLocation(250, -15);
		oidLabel.setSize(90, 50);

		// Object ID text field
		JTextField oidTF = new JTextField();
		oidTF.setBounds(250, 20, 110, 20);

		// Value label
		JLabel valueLabel = new JLabel();
		valueLabel.setText("Value:");
		valueLabel.setForeground(Color.white);
		valueLabel.setLocation(370, -15);
		valueLabel.setSize(90, 50);

		// Value text field
		JTextField valueTF = new JTextField();
		valueTF.setBounds(370, 20, 110, 20);
 
		// IP Text Area
		JTextArea ipTA = new JTextArea();
		ipTA.setEditable(false);
		JScrollPane scroll = new JScrollPane(ipTA);
		scroll.setBounds(10, 290, 725, 140);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		// write Button a.k.a. snmpSet(strAdr, comm, strOID, value)
		JButton writeButt = new JButton();
		writeButt.setText("Write/Set info");
		writeButt.setBounds(10, 60, 125, 50);
		writeButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if((!hostTF.getText().equals("")) && (!commTF.getText().equals("")) && (!oidTF.getText().equals("")) && (!valueTF.getText().equals(""))) {
					ipTA.append("=============== Writing ===============\n" + obj.snmpSet(hostTF.getText(), commTF.getText(), oidTF.getText(), valueTF.getText()) + '\n');
				}
			}
		});

		// read Button a.k.a. snmpGet(strAdr, comm, strOID)
		JButton readButt = new JButton();
		readButt.setText("Read/Get info");
		readButt.setBounds(145, 60, 125, 50);
		readButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if((!hostTF.getText().equals("")) && (!commTF.getText().equals("")) && (!oidTF.getText().equals(""))) {
					ipTA.append("================== Read ==================\n" + obj.snmpGet(hostTF.getText(), commTF.getText(), oidTF.getText()) + '\n');
				}
			}
		});
 
		// Close button
		JButton close = new JButton();
		close.setText("Exit");
		close.setBounds(280, 60, 100, 50);
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

		//JPanel panel = new JPanel();
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
		panel.add(close);
		panel.add(scroll);
		frame.add(panel);
		frame.setResizable(false);
		frame.setVisible(true);
	}
}
