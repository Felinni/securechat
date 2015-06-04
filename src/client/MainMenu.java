package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.ChatManagerListener;

public class MainMenu implements Runnable{
	
	private BufferedReader _scanner;
	private Account _account;
	private Chat _chat;
	private ChatManager _chatmgr;
	private ChatRoom _chatroom;
	private String opt;
	
	private String _menu = "1 - Show all users\n" +
			"2 - Chat with friend\n" +
			"0 - Exit";
	
	public MainMenu(){
		_chat = null;
		_chatroom = new ChatRoom();
		_scanner = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public void setUp(Account account){
		String user, pass;
		System.out.print("Username: ");
		try {
			user = _scanner.readLine();
			System.out.print("Password: ");
			pass = _scanner.readLine();
			_account = new Account(user, pass);
			if(_account.login()){
				System.out.println("Successfully logged in on " + user +".");
			} else {
				return;
			}
			_chatmgr = _account.get_con().getChatManager();
			_chatmgr.addChatListener(new ChatManagerListener(){
				@Override
				public void chatCreated(Chat chat, boolean localchat) {
					if(!localchat && _chat==null){
						System.out.println("User " + chat.getParticipant() + " wants to chat, enter anything to continue");
						System.out.println("Chat started");
						//_chat = _chatmgr.createChat(chat.getParticipant(), null);
						_chat = chat;
						_chatroom.set_chat(_chat);
						_chatroom.run();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						while(_chatroom.is_run()){
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						System.out.println("Boop");

					} else {
						return;
					}
				}
				
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void run(){
		try {
			showMenu();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void showMenu() throws IOException{
		System.out.println(_menu);
		opt = _scanner.readLine();
		while(true){
			if(opt.equals("1")){
				showFriends();
				opt = null;
			} else if(opt.equals("2")){
				System.out.println("Enter username of whom you which to chat with:");
				String user = _scanner.readLine();
				user += "@feline.com";
				opt = null;
				startChat(user);
			} else if(opt.equals("0")){
				_account.logout();
				return;
			} else {
				System.out.println("Waiting for chat room to close");
				try{
					Thread.sleep(2000);
					while(_chatroom.is_run()){
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Chat closed");
				opt = null;
			}
			System.out.println(opt);
			System.out.println(_menu);
			opt = _scanner.readLine();
		}
	}

	
	public void showFriends(){
		Roster roster = _account.get_roster();
		Collection<RosterEntry> entries = roster.getEntries();
		System.out.println("This is your list of friends.");
		for(RosterEntry entry : entries){
			System.out.println(entry);
		}
	}
	
	public void startChat(String user){
		if(_chat==null){
			System.out.println("Going to create chat with user " + user);
			_chat = _chatmgr.createChat(user, null);
			System.out.println("Chat created: " + _chat.getParticipant());
			_chatroom.set_chat(_chat);
			_chatroom.run();
			while(_chatroom.is_run()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			_chat=null;
		}
	}

}
