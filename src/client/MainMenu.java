package client;

import java.util.Scanner;
import java.util.Collection;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;

public class MainMenu {
	
	private Scanner _scanner;
	private Account _account;
	
	private String _menu = "1 - Show available users\n" +
			"0 - Exit";
	
	public MainMenu(){
		_scanner = new Scanner(System.in);
	}
	
	public void setUp(Account account){
		String user, pass;
		System.out.print("Username: ");
		user = _scanner.next();
		System.out.print("Password: ");
		pass = _scanner.next();
		_account = new Account(user, pass);
		if(_account.login()){
			System.out.println("Successfully logged in on " + user +".");
		} else {
			return;
		}	
	}
	
	public void showMenu(){
		/*int opt;
		System.out.println(_menu);
		opt = _scanner.nextInt();*/
		XMPPConnection accountcon = _account.get_con();
		Roster roster = accountcon.getRoster();
		Collection<RosterEntry> entries = roster.getEntries();
		for(RosterEntry entry : entries){
			System.out.println(entry);
		}
		System.out.println("This is your list of friends.");
		int opt = _scanner.nextInt();
		_account.logout();
	}

}
