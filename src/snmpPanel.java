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
	public static final String OID_UPS_OUTLET_GROUP1 = ".1.3.6.1.2.1.25.5.1.1.1.7872";
	public static final String OID_UPS_BATTERY_CAPACITY = ".1.3.6.1.2.1.25.5.1.1.1.7872";
   
	public static void main(String[] args) {
		controlPanel();
		/*try {
			String strIPAddress = "127.0.0.1";
			snmpPanel objSNMP = new snmpPanel();

			// Set Value = 2 to turn OFF UPS OUTLET Group1
			// Set Value = 1 to turn ON  UPS OUTLET Group1
			int value = 3;
			objSNMP.snmpSet(strIPAddress, WRITE_COMMUNITY, OID_UPS_OUTLET_GROUP1, value);

			// Get Basic state of UPS
			String batteryCap = objSNMP.snmpGet(strIPAddress, READ_COMMUNITY, OID_UPS_BATTERY_CAPACITY);
         System.out.println(batteryCap);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	/*
	* The following code valid only SNMP version1
	* This method is very useful to set a parameter on remote device
	*/
	public void snmpSet(String strAddress, String community, String strOID, int value) {
		strAddress= strAddress+"/161"; //changed SNMP_PORT to 161
		Address targetAddress = GenericAddress.parse(strAddress);
		Snmp snmp;
		try {
			TransportMapping transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);
			transport.listen();
			CommunityTarget target = new CommunityTarget();
			target.setCommunity(new OctetString(community));
			target.setAddress(targetAddress);
			target.setRetries(2);
			target.setTimeout(5000);
			target.setVersion(SnmpConstants.version2c);//changed to version2c was version1
			PDU pdu = new PDU();
			pdu.add(new VariableBinding(new OID(strOID), new Integer32(value)));
			pdu.setType(PDU.SET);
			ResponseListener listener = new ResponseListener() {
				public void onResponse(ResponseEvent event) {
					// Always cancel async request when response has been received otherwise a memory leak is cre  ated!
					// Not canceling a request immediately can be useful when sending a request to a broadcast address.
					((Snmp)event.getSource()).cancel(event.getRequest(), this);
					System.out.println("Set Status is: " + event.getResponse().getErrorStatusText());
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
	public String snmpGet(String strAddress, String comm, String strOID) {
      String str="";
      String length="";
		try {
			OctetString Community = new OctetString(comm);
			strAddress = strAddress + "/161";
			Address targetaddress = new UdpAddress(strAddress);
			TransportMapping transport = new DefaultUdpTransportMapping();
			transport.listen();
			CommunityTarget comtarget = new CommunityTarget();
			comtarget.setCommunity(Community);
			comtarget.setVersion(SnmpConstants.version2c);
			comtarget.setAddress(targetaddress);
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
					//System.out.println("sucsess");
               str = "Success";
					PDU pduresponse=response.getResponse();
					length = pduresponse.getVariableBindings().firstElement().toString();
					if(length.contains("=")) {
						int len = length.indexOf("=");
						length=length.substring(len+1, length.length());
					} else{
						str = "no data";
					}
				} else{
					System.out.println("response failed");
				}
			} else {
				System.out.println("Feeling like a TimeOut occured ");
			}
			snmp.close();
		} catch(Exception e) { e.printStackTrace(); }
		str = "Address: " + strAddress + '\n' + str + ": response = " + length + '\n';
      //System.out.println(str);
		return str;
   }
	
	public static void controlPanel() {
		snmpPanel obj = new snmpPanel();	
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
		valueTF.setBounds(370, 20, 50, 20);
 
		// IP Text Area
		JTextArea ipTA = new JTextArea();
		ipTA.setBounds(435, 10, 300, 455);
		ipTA.setEditable(false);

		// write Button a.k.a. snmpSet(strAdr, comm, strOID, value)
		JButton writeButt = new JButton();
		writeButt.setText("Set info");
		writeButt.setBounds(10, 60, 125, 50);
        writeButt.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent event) {
            //System.out.println("Writing: " + hostTF.getText() + ' ' + commTF.getText() + ' ' + oidTF.getText() + ' ' + 2 + '\n');
				//ipTA.append("\tWriting:\n Host: " + hostTF.getText() + "\n Community: " + commTF.getText() + "\n Object ID: " + oidTF.getText() + "\n Value: " + valueTF.getText() + "\n==============================\n");
            int value = Integer.parseInt(valueTF.getText());
				obj.snmpSet(hostTF.getText(), commTF.getText(), oidTF.getText(), value);
        	}
        });

		// read Button a.k.a. snmpGet(strAdr, comm, strOID)
		JButton readButt = new JButton();
		readButt.setText("Write info");
		readButt.setBounds(145, 60, 125, 50);
		readButt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				//System.out.println("Reading: " + hostTF.getText() + ' ' + commTF.getText() + ' ' + oidTF.getText() + '\n');
				//ipTA.append("\tReading:\n Host: " + hostTF.getText() + "\n Community: " + commTF.getText() + "\n Object ID: " + oidTF.getText() + "\n==============================\n");
				ipTA.append(obj.snmpGet(hostTF.getText(), commTF.getText(), oidTF.getText()));
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
		panel.add(valueLabel);
		panel.add(valueTF);
		panel.add(writeButt);
		panel.add(readButt);
		panel.add(close);
		panel.add(ipTA);

		frame.add(panel);
		frame.setResizable(false);
		frame.setVisible(true);
   }
}
