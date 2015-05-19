package client;

import java.util.Scanner;

public class MainMenu {
	
	private Scanner _scanner;
	
	private String _menu = "1 - Show available users\n" +
			"0 - Exit";
	
	public MainMenu(){
		_scanner = new Scanner(System.in);
	}
	
	public void setUp(Account account){
		//Register
		System.out.println("Let's register a new user.");
		String user, pass;
		System.out.print("Username: ");
		user = _scanner.next();
		System.out.print("Password: ");
		pass = _scanner.next();
		account = new Account(user, pass);
		
		//Confirmation
		System.out.println("You have successfully registered as " + user + ".");
	}
	
	public void showMenu(){
		int opt;
		System.out.println(_menu);
		opt = _scanner.nextInt();
		
	}

}
