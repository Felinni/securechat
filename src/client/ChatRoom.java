package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class ChatRoom implements Runnable {
	
	private BufferedReader _scanner;
	private Chat _chat;
	private boolean _run;

	public ChatRoom(){
		_scanner = new BufferedReader(new InputStreamReader(System.in));
		_run = false;
	}
	
	@Override
	public void run() {
		_run = true;
		_chat.addMessageListener(new MessageListener(){
			@Override
			public void processMessage(Chat chat, Message message) {
				//TODO: Add decryption here
				System.out.println("Received message from " + chat.getParticipant());
				if(message.getBody().equals("exit")){
					_run=false;
					System.out.println("Chat closed by the other user, press anything to continue");
				} else System.out.println(message.getFrom() + ":" + message.getBody());
			}
		});
		System.out.println("Chatroom created with listeners " + _chat.getListeners().size());
		try{
			String firstmessage = "start";
			_chat.sendMessage(firstmessage);
			String send;
			while(_run){
				System.out.println("Listeners still on" + _chat.getListeners().size() + "Please insert message:");
				send = _scanner.readLine();
					if(!send.equals("exit")){
						_chat.sendMessage(send);
						System.out.println("Message sent to " + _chat.getParticipant());
					} else {
						_chat.sendMessage(send);
						_chat = null;
						System.out.println("Chat closed in chatroom");
						_run=false;
						return;
					}
			}
			_chat = null;
			System.out.println("Returning to main menu");
			return;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (XMPPException e) {
			System.out.println("Failed to send the message");
			System.out.println(e.getMessage());
		}
	}

	public void set_chat(Chat _chat) {
		this._chat = _chat;
	}

	public boolean is_run() {
		return _run;
	}

	public void set_run(boolean _run) {
		this._run = _run;
	}
	
	
}
