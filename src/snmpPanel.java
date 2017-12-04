import java.net.InetAddress;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

import org.snmp4j.CommunityTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.PDU;
import org.snmp4j.smi.*;
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class snmpPanel {
	// Variables for the SNMP	
	public static final String READ_COMMUNITY = "public";
	public static final String WRITE_COMMUNITY = "private";
	public static final int mSNMPVersion = 0;
	public static final String OID_UPS_OUTLET_GROUP1 = "1.3.6.1.4.1.318.1.1.1.12.3.2.1.3.1";
	public static final String OID_UPS_BATTERY_CAPACITY = "1.3.6.1.4.1.318.1.1.1.2.2.1.0";
   
	public static void main(String[] args) {
		controlPanel();
		/*try {
			String strIPAddress = "172.20.1.150";
			snmpPanel objSNMP = new snmpPanel();

			// Set Value = 2 to turn OFF UPS OUTLET Group1
			// Set Value = 1 to turn ON  UPS OUTLET Group1
			int value = 2;
			objSNMP.snmpSet(strIPAddress, WRITE_COMMUNITY, OID_UPS_OUTLET_GROUP1, value);

			// Get Basic state of UPS
			String batteryCap = objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, OID_UPS_BATTERY_CAPACITY);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	/*
	* The following code valid only SNMP version1
	* This method is very useful to set a parameter on remote device
	*/
	public void snmpSet(String strAdr, String comm, String strOID, int value) {
		strAdr = strAdr + "/" + "161";
		Address targetAddress = GenericAddress.parse(strAdr);
		Snmp snmp;
		try {
			DefaultUdpTransportMapping transport =  new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			transport.listen();
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString(comm));
			target.setAddress(targetAddress);
			target.setRetries(2);
			target.setTimeout(5000);
			target.setVersion(SnmpConstants.version1);
			PDU pdu = new PDU();
			pdu.setType(PDU.SET);
			ResponseListener listener = new ResponseListener() {
				public void onResponse(ResponseEvent event) {
					// Always cancel asynce request when response has been received otherwise a memory leak is created
					// Not cancelling a request immediately can be useful when sendign a request to a broadcast address
					((Snmp)event.getSource()).cancel(event.getRequest(), this);
					System.out.println("Set status is: " + event.getResponse().getErrorStatusText());
				}
			};
			snmp.send(pdu, target, null, listener);
			snmp.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	* The code code is valid only SNMP version1
	* SNMPGet method return response for given OID from the Device
	*/
	public String snmpGet(String strAdr, String comm, String strOID) {
		String str="";
		try {
		    OctetString Comm = new OctetString(comm);
		    strAdr = strAdr + "/" + "162";
		    Address targetAddress = new UdpAddress(strAdr);
		    DefaultUdpTransportMapping transport = new DefaultUdpTransportMapping();
		    transport.listen();
		    CommunityTarget comtarget = new CommunityTarget();
		    comtarget.setCommunity(Comm);
		    comtarget.setVersion(SnmpConstants.version1);
		    comtarget.setAddress(targetAddress);
		    comtarget.setRetries(2);
		    comtarget.setTimeout(5000);
		    PDU pdu = new PDU();
		    ResponseEvent response;
		    Snmp snmp;
		    pdu.add(new VariableBinding(new OID(strOID)));
		    pdu.setType(PDU.GET);
		    snmp = new Snmp(transport);
		    response = snmp.get(pdu,comtarget);
		    if(response != null) {
			    if(response.getResponse().getErrorStatusText().equalsIgnoreCase("Success")) {
				    PDU pduresponse = response.getResponse();
				    str = pduresponse.getVariableBindings().firstElement().toString();
				    if(str.contains("=")) {
				        int len = str.indexOf("=");
					    str = str.substring(len+1, str.length());
				    }
			    }
		    } else {
			    System.out.println("Feeling like a timeout occured");
		    }
		    snmp.close();
	    } catch(Exception e) {
		    e.printStackTrace();
	    }
	    return "Response: " + str;
	}
	
	public static void controlPanel() {
		JFrame frame = new JFrame();
		frame.setSize(750, 500);
		frame.setTitle("SNMP Boi");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     
		frame.setLayout(new BorderLayout());
		JLabel panel = new JLabel(new ImageIcon("child.jpg"));

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
		commLabel.setLocation(140, -15);
		commLabel.setSize(90, 50);
	
		// Community text field
		JTextField commTF = new JTextField();
		commTF.setBounds(140, 20, 110, 20);

		// Object ID label
		JLabel oidLabel = new JLabel();
		oidLabel.setText("Object ID:");
		oidLabel.setLocation(270, -15);
		oidLabel.setSize(90, 50);

		// Object ID text field
		JTextField oidTF = new JTextField();
		oidTF.setBounds(270, 20, 110, 20);
 
		// IP Text Area
		JTextArea ipTA = new JTextArea();
		ipTA.setBounds(430, 20, 300, 450);
		ipTA.setEditable(false);

		// write Button a.k.a. snmpSet(strAdr, comm, strOID, value)
		JButton writeButt = new JButton();
		writeButt.setText("Read-Write info");
		writeButt.setBounds(10, 60, 125, 50);
        writeButt.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent event) {
           		System.out.println("Writing: " + hostTF.getText() + ' ' + commTF.getText() + ' ' + oidTF.getText() + ' ' + 2 + '\n');
				ipTA.append("\tWriting:\n Host: " + hostTF.getText() + "\n Community: " + commTF.getText() + "\n Object ID: " + oidTF.getText() + "\n Value: " + 2 + "\n==============================\n");
        	}
        });

		// read Button a.k.a. snmpGet(strAdr, comm, strOID)
		JButton readButt = new JButton();
		readButt.setText("Read-only info");
		readButt.setBounds(145, 60, 125, 50);
		readButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.out.println("Reading: " + hostTF.getText() + ' ' + commTF.getText() + ' ' + oidTF.getText() + '\n');
				ipTA.append("\tReading:\n Host: " + hostTF.getText() + "\n Community: " + commTF.getText() + "\n Object ID: " + oidTF.getText() + "\n==============================\n");
			}
		});
 
		// Close button
		JButton close = new JButton();
		close.setText("Exit");
		close.setBounds(10, 410, 100, 50);
		close.addActionListener(new ActionListener() {
        		public void actionPerformed(ActionEvent event) {
	            	System.exit(0);
         		}
      		});

		// Set SNMP Radio
		JRadioButton writeRadio = new JRadioButton("Write", true);
		writeRadio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.out.print(event.getActionCommand());
			}
		});

		ButtonGroup group = new ButtonGroup();
		group.add(writeRadio);

		//JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.add(hostLabel);
		panel.add(hostTF);
		panel.add(commLabel);
		panel.add(commTF);
		panel.add(oidLabel);
		panel.add(oidTF);
		panel.add(writeButt);
		panel.add(readButt);
		panel.add(close);
		panel.add(ipTA);

		frame.add(panel);
		frame.setResizable(false);
		frame.setVisible(true);
   }
}
