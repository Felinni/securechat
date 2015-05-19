package client;

public class Account {

	private String _user;
	private String _password;
	
	public Account(String user, String password){
		_user = user;
		_password = password;
	}

	public String get_user() {
		return _user;
	}

	public String get_password() {
		return _password;
	}
	
	
	
}
