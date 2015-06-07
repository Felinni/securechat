package client;

public class SecureChat {
	
	private static Account _account;

	public static void main(String[] args) {
		Client menu = new Client();
		menu.setUp(_account);
		menu.run();
	}

}
