package client;

public class SecureChat {
	
	private static Account _account;

	public static void main(String[] args) {
		MainMenu menu = new MainMenu();
		menu.setUp(_account);
		menu.showMenu();
	}

}
