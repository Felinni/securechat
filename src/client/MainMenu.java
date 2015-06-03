package client;

import java.util.Scanner;
import java.util.Collection;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.ChatManagerListener;

public class MainMenu {
	
	private Scanner _scanner;
	private Account _account;
	private Chat _chat;
	private ChatManager _chatmgr;
	
	private String _menu = "1 - Show all users\n" +
			"2 - Chat with friend\n" +
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
		_chatmgr = _account.get_con().getChatManager();
		_chatmgr.addChatListener(new ChatManagerListener(){
			@Override
			public void chatCreated(Chat chat, boolean localchat) {
				if(!localchat && _chat==null){
					_chat = chat;
					_chat.addMessageListener(new MessageListener(){

						@Override
						public void processMessage(Chat chat, Message message) {
							//TODO: Add decryption here
							if(chat==_chat)
								System.out.println(message.getFrom() + ":" + message.getBody());
							else System.out.println("Someone else is trying to send you a message");
						}
						
					});
				}
				
			}
			
		});
	}
	
	public void showMenu(){
		int opt;
		System.out.println(_menu);
		opt = _scanner.nextInt();
		while(true){
			if(opt==1){
				showFriends();
			} else if(opt==2){
				System.out.println("Enter username of whom you which to chat with:");
				String user = _scanner.nextLine();
				chat(user, true);
			} else if(opt==0){
				_account.logout();
				return;
			}
			opt = _scanner.nextInt();
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
	
	public void chat(String user, boolean local){
		if(local && _chat==null){
			_chat = _chatmgr.createChat(user, new MessageListener() {
				@Override
				public void processMessage(Chat chat, Message message) {
					//TODO: Add decryption here
					if(chat==_chat)
						System.out.println(message.getFrom() + ":" + message.getBody());
					else System.out.println("Someone else is trying to send you a message");
				}
			});
		}
		while(true){
			String send = _scanner.nextLine();
			if(!send.equals("exit")){
				try {
					_chat.sendMessage(send);
				} catch (XMPPException e) {
					System.out.println("Failed to send the message");
					System.out.println(e.getMessage());
				}
			} else {
				_chat = null;
			}
		}
	}

}
