import java.net.InetAddress;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

import org.snmp4j.CommunityTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.PDU;
import org.snmp4j.smi.*;
import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class snmpPanel extends JFrame {
	// Objects for the GUI
	private ActionListener listener;
	private JLabel oidLabel;
	private JTextField oidText;
	private JButton setButt;
	private JButton getButt;
	private JTextArea ackArea;
	private JPanel controlPanel;

	// Variables for the SNMP	
	public static final String READ_COMMUNITY = "public";
	public static final String WRITE_COMMUNITY = "private";
	public static final int mSNMPVersion = 0;
	public static final String OID_UPS_OUTLET_GROUP1 = "1.3.6.1.4.1.318.1.1.1.12.3.2.1.3.1";
	public static final String OID_UPS_BATTERY_CAPACITY = "1.3.6.1.4.1.318.1.1.1.2.2.1.0";

	public snmpPanel() {
		// Implementing a listener to be used for the ActionListener
		listener = new ChoiceListener();
		createControlPanel();
	}

	public static void main(String[] args) {
		try {
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
		}
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
	
	// Making the buttons do some stuff
	class ChoiceListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if(event.getSource() == setButt) {
				// Writing
				ackArea.setText("setButt pressed");
			} else if(event.getSource() == getButt) {
				// Reading
				ackArea.setText("getButt pressed");
			}
		}
	}

	// Setting up the frame for snmpPanel
	public void createControlPanel() {
		// Creating the buttons
		setButt = new JButton("setButt");
		getButt = new JButton("getButt");
		setButt.addActionListener(listener);		
		getButt.addActionListener(listener);

		// Creating the text fields
		ackArea = new JTextArea(100, 20);
		ackArea.setEditable(false);

		// Creating the text area
   		oidLabel = new JLabel("oidLable");
		oidText = new JTextField("oidText");

		// Adding all the objects to the panel
		JPanel panel = new JPanel();
		panel.add(setButt);
		panel.add(getButt);
		panel.add(ackArea);
		panel.add(oidLabel);
		panel.add(oidText);
	}
}
