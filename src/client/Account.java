package client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

public class Account {

	private String _user;
	private String _password;
	private ConnectionConfiguration _conConf;
	private XMPPConnection _con;
	
	public Account(String user, String password){
		_user = user;
		_password = password;
		_conConf = new ConnectionConfiguration("192.168.1.108", 5222, "feline.com");
		_con = new XMPPConnection(_conConf);
	}
	
	public boolean login(){
		try {
			_con.connect();
			System.out.println("Logging in...");
			_con.login(_user, _password);
		} catch (XMPPException e) {
			System.out.println("Login failed.");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public void logout(){
		_con.disconnect();
	}

	public String get_user() {
		return _user;
	}

	public String get_password() {
		return _password;
	}

	public XMPPConnection get_con() {
		return _con;
	}
	
}
