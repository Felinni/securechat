package client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.ocsp.Signature;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;

public class Client implements MessageListener{
	
	private BufferedReader _scanner;
	private Account _account;
	private ChatManager _chatmgr;
	private boolean _closed;
	private String _incchat;
	private boolean _requested;
	private String opt;
	private String name = "MainMenu";
	private boolean debug = false;
	
	private String _menu = "1 - Show all users\n" +
			"2 - Chat with friend\n" +
			"0 - Exit";
	
	private String _myNonce;
	private boolean _local;
	private String _symKey;
	
	private String _reqNonce;
	private X509Certificate _reqCert;
	private String _reqEncNonce;
	private PublicKey _reqKey;
	
	public Client(){
		_scanner = new BufferedReader(new InputStreamReader(System.in));
		_closed = true;
		_requested = false;
		_local = false;
		//Requested stuff
		_reqNonce = "";
		_reqCert = null;
		_reqEncNonce = "";
		_symKey = "";
	}
	
	public void setUp(Account account){
		try {
			String user, pass;
			System.out.print("Username: ");
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
					if(!localchat && _closed){
						_incchat = chat.getParticipant();
						System.out.println("User " + chat.getParticipant() + " wishes to talk to you, enter 'ok' to continue.");
						_chatmgr.removeChatListener(this);
						_requested = true;
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
			} else if(opt.equals("2")){
				System.out.println("Enter username of whom you which to chat with:");
				String user = _scanner.readLine();
				user += "@feline.com";
				startChat(user);
				_account.logout();
				return;
			} else if(opt.equals("0")){
				_account.logout();
				return;
			} else {
				if(_requested){
					chatRoom(_incchat);
					_account.logout();
					return;
				}			
			}
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
		_local = true;
		chatRoom(user);
	}
	
	public void chatRoom(String user){
		_closed = false;
		String send;
		Chat chat;
		exchangeKeys(user);
		try {
			while(true){
				System.out.println("Send message: ");
				send = _scanner.readLine();
				//Add encryption here
				if(_closed){
					break;
				}
			    chat = _chatmgr.createChat(user, this);
			    if(!send.equals("exit")){
			    	send = encryptAES(_symKey, send);
					//if(debug) 
						System.out.println("The message after encryption is: " + send);
			    	chat.sendMessage(send);
			    } else {
			    	chat.sendMessage(send);
			    	System.out.println("Chat closed.");
			    	_closed = true;
			    	chat.removeMessageListener(this);
					System.out.println("Removing from listeners " + this.name);
			    	return;
			    }
			}
			_requested = false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void processMessage(Chat chat, Message message) {
		// TODO Auto-generated method stub
		if(message.getBody().split(":")[0].equals(chat.getParticipant().split("@")[0] + "/Nonce")){
			_reqNonce = message.getBody().split(":")[1];
			if(debug) System.out.println("Received nonce:" + _reqNonce);
		} else if(message.getBody().split(":")[0].equals(chat.getParticipant().split("@")[0] + "/PubCert")){			
			CertificateFactory cf;
			try {
				cf = CertificateFactory.getInstance("X.509");
				if(debug) System.out.println(message.getBody().split(":")[1]);
				_reqCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(Base64.decode(message.getBody().split(":")[1])));
				if(debug) System.out.println("Received pubkey certificate:");
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(message.getBody().split(":")[0].equals(chat.getParticipant().split("@")[0] + "/EncNonce")){
			_reqEncNonce = message.getBody().split(":")[1];
		} else if(message.getBody().split(":")[0].equals(chat.getParticipant().split("@")[0] + "/EncSym")){
			String decryptTarget = allEncrypting(false, message.getBody().split(":")[1], _account.get_privInfo());
			_symKey = decryptTarget;
		} else if(message.getBody().equals("exit")){
			chat.removeMessageListener(this);
			System.out.println("Removing from listeners " + this.name);
			_closed = true;
 			System.out.println("Chat closed by the other user, press 'ok' to continue");
		} else {
			try {
				System.out.println("Received message from " + chat.getParticipant());
				String recmessage = decryptAES(_symKey, message.getBody());
				System.out.println(message.getFrom() + ":" + recmessage);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public String generateNonce(){
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(25);
	}
	
	public void exchangeKeys(String user){
		Chat chat = _chatmgr.createChat(user, this);
		if(_local){
			try {
				System.out.println("Sending hello message");
				chat.sendMessage(_account.get_user());
				while(_reqNonce.equals("")){
					Thread.sleep(100);
				}
				if(debug) System.out.println("Preparing to send certificate after receiving nonce: " + _reqNonce);
				String mycert = new String(Base64.encode(_account.get_pubKeyCert().getEncoded()), "UTF-8");
				if(debug) System.out.println("Sending certificate: " + mycert);
				chat = _chatmgr.createChat(user, this);
				chat.sendMessage(_account.get_user() + "/PubCert:" + mycert);
				if(debug) System.out.println("Sending encrypted nonce");
				chat = _chatmgr.createChat(user, this);
				String encnonce = allEncrypting(true, _reqNonce, _account.get_privInfo());
				if(debug) System.out.println("The encrypted nonce is now" + encnonce);
				chat.sendMessage(_account.get_user() + "/EncNonce:" + encnonce);
				_myNonce = generateNonce();
				if(debug) System.out.println("Sending my nonce " + _myNonce);
				chat.sendMessage(_account.get_user() + "/Nonce:" + _myNonce);
				while(_reqCert==null){
					//Wait here
					Thread.sleep(100);
				}
				if(debug) printX509Certificate(_reqCert);
				_reqKey = _reqCert.getPublicKey();
				while(_reqEncNonce.equals("")){
					Thread.sleep(100);
				}
				String tmp_nonce = allEncrypting(false, _reqEncNonce, _reqKey);
				System.out.println("The received nonce is: " + tmp_nonce + " and the nonce sent is: " + _myNonce);
				while(_symKey.equals("")){
					Thread.sleep(100);
				}
				System.out.println("Received sym key: " + _symKey);
			} catch (XMPPException | CertificateEncodingException | UnsupportedEncodingException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				_myNonce = generateNonce();
				if(debug) System.out.println("Sending my nonce " + _myNonce);
				chat.sendMessage(_account.get_user() + "/Nonce:" + _myNonce);
				while(_reqCert==null){
					//Wait here
					Thread.sleep(100);
				}
				if(!verifyCertSign(_reqCert)){
					System.out.println("The signature does not match the certificate, exiting");
					return;
				}
				if(debug) printX509Certificate(_reqCert);
				_reqKey = _reqCert.getPublicKey();
				while(_reqEncNonce.equals("")){
					Thread.sleep(100);
				}
				String tmp_nonce = allEncrypting(false, _reqEncNonce, _reqKey);
				System.out.println("The received nonce is: " + tmp_nonce + " and the nonce sent is: " + _myNonce);
				while(_reqNonce.equals("")){
					Thread.sleep(100);
				}
				if(debug) System.out.println("Preparing to send certificate after receiving nonce: " + _reqNonce);
				String mycert = new String(Base64.encode(_account.get_pubKeyCert().getEncoded()), "UTF-8");
				if(debug) System.out.println("Sending certificate: " + mycert);
				chat = _chatmgr.createChat(user, this);
				chat.sendMessage(_account.get_user() + "/PubCert:" + mycert);
				if(debug) System.out.println("Sending encrypted nonce");
				chat = _chatmgr.createChat(user, this);
				String encnonce = allEncrypting(true, _reqNonce, _account.get_privInfo());
				if(debug) System.out.println("The encrypted nonce is now" + encnonce);
				chat.sendMessage(_account.get_user() + "/EncNonce:" + encnonce);
				//Generate SYMMETRIC KEY
				SecureRandom random = new SecureRandom();
				int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
				System.out.println("The max key length is " + maxKeyLen);
				_symKey = new BigInteger(130, random).toString(25);
				//Encrypt with others public key
				String encSymKey = allEncrypting(true, _symKey, _reqCert.getPublicKey());
				chat = _chatmgr.createChat(user, this);
				chat.sendMessage(_account.get_user() + "/EncSym:" + encSymKey);
				System.out.println("Sent sym key: " + _symKey);
			} catch (XMPPException | InterruptedException | CertificateEncodingException | UnsupportedEncodingException | NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void printX509Certificate(X509Certificate cert){
		try {
			cert.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			// TODO Auto-generated catch block
			System.out.println("The certificates date is invalid.");
			e.printStackTrace();
		}
		System.out.println(cert.getIssuerDN().getName());
		BASE64Encoder b64 = new BASE64Encoder();
		System.out.println("Certificate public key: " + b64.encode(cert.getPublicKey().getEncoded()));
	}
	
	public static byte[] encryptPub(byte[] text, PublicKey key) throws Exception{
		byte[] cipherText = null;
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		cipherText = cipher.doFinal(text);
		return cipherText;
	}
	public static byte[] decryptPub(byte[] text, PublicKey key) throws Exception{
		byte[] cipherText = null;
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		cipherText = cipher.doFinal(text);
		return cipherText;
	}

	public static byte[] encryptPriv(byte[] text, PrivateKey key) throws Exception{
		byte[] cipherText = null;
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		cipherText = cipher.doFinal(text);
		return cipherText;
	}
	public static byte[] decryptPriv(byte[] text, PrivateKey key) throws Exception{
		byte[] cipherText = null;
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, key);
		cipherText = cipher.doFinal(text);
		return cipherText;
	}
	
	private static String encodeBASE64(byte[] bytes){
		BASE64Encoder b64 = new BASE64Encoder();
		return b64.encode(bytes);
	}

	private static byte[] decodeBASE64(String text) throws IOException {
		BASE64Decoder b64 = new BASE64Decoder();
		return b64.decodeBuffer(text);
	}
	
	private String allEncrypting(boolean encrypt, String text, Key key){
		String result = "";
		try {
			byte[] cipherText = decodeBASE64(text);
			if(key instanceof PrivateKey){
				if(encrypt){
					cipherText = encryptPriv(cipherText, (PrivateKey)key);
				} else {
					cipherText = decryptPriv(cipherText, (PrivateKey)key);
				}
			} else if(key instanceof PublicKey){
				if(encrypt){
					cipherText = encryptPub(cipherText, (PublicKey)key);
				} else {
					cipherText = decryptPub(cipherText, (PublicKey)key);
				}
			}
			result = encodeBASE64(cipherText);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public static String encryptAES(String key, String toEncrypt) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		byte[] bytes16 = key.getBytes();
		byte[] newArray = Arrays.copyOfRange(bytes16, 0, 16);
		SecretKeySpec skeySpec = new SecretKeySpec(newArray, "AES");
	    //SecretKeySpec skeySpec = generateKeySpec(key);
	    Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
	    byte[] iv = { 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8 };
	    IvParameterSpec zeroIv = new IvParameterSpec(iv);
	    cipher.init(Cipher.ENCRYPT_MODE, skeySpec, zeroIv);
	    byte[] encrypted = cipher.doFinal(toEncrypt.getBytes());
	    //byte[] encryptedValue = Base64.encodeBase64(encrypted);
	    //return new String(encryptedValue);
	    return encodeBASE64(encrypted);
	}

	public static String decryptAES(String key, String encrypted) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		byte[] bytes16 = key.getBytes();
		byte[] newArray = Arrays.copyOfRange(bytes16, 0, 16);
		SecretKeySpec skeySpec = new SecretKeySpec(newArray, "AES");
		//Key skeySpec = generateKeySpec(key);
	    Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding", "BC");
	    byte[] iv = { 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8 };
	    IvParameterSpec zeroIv = new IvParameterSpec(iv);
	    cipher.init(Cipher.DECRYPT_MODE, skeySpec, zeroIv);
	    //byte[] decodedBytes = Base64.decodeBase64(encrypted.getBytes());
	    byte[] original = cipher.doFinal(decodeBASE64(encrypted));
	    return new String(original);
	}
	
	public boolean verifyCertSign(X509Certificate cert){
		PublicKey key = cert.getPublicKey();
		try {
			cert.verify(key);
		} catch (InvalidKeyException | CertificateException
				| NoSuchAlgorithmException | NoSuchProviderException
				| SignatureException e) {
			// TODO Auto-generated catch block
			
			//e.printStackTrace();
			return false;
		}
		return true;
	}
}
