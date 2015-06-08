package client;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import Decoder.BASE64Encoder;

@SuppressWarnings("deprecation")
public class Account {

	private String _user;
	private String _password;
	private String _name;
	private ConnectionConfiguration _conConf;
	private XMPPConnection _con;
	private Roster _roster;
	private PrivateKey _privInfo;
	private PublicKey _pubInfo;
	private String _privKey;
	private String _pubKey;
	private X509Certificate _pubKeyCert;
	
	private boolean debug = false;
	
	public Account(String user, String password){
		//Account settings
		_user = user;
		_password = password;
		_name = "CN=" + user;
		
		//Connection settings
		_conConf = new ConnectionConfiguration("192.168.1.82", 5222, "feline.com");
		_con = new XMPPConnection(_conConf);
		
		//Key pair and certificate settings
		KeyPairGenerator keyGen;
		try {
		    Security.addProvider(new BouncyCastleProvider());
			keyGen = KeyPairGenerator.getInstance("RSA", "BC");
			keyGen.initialize(1024);
			KeyPair key = keyGen.generateKeyPair();
			BASE64Encoder b64 = new BASE64Encoder();
			_privInfo = key.getPrivate();
			_pubInfo = key.getPublic();
	        _privKey = b64.encode(_privInfo.getEncoded());
	        _pubKey = b64.encode(_pubInfo.getEncoded());
	        System.out.println("Public key: " + _pubKey);
	        //System.out.println("Private key: " + _privKey);
	        createX509Certificate();
	        if(debug) printX509Certificate();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public boolean login(){
		try {
			System.out.println("Logging in...");
			_con.connect();
			_con.login(_user, _password);
			_roster = _con.getRoster();
		} catch (XMPPException e) {
			System.out.println("Login failed.");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void createX509Certificate(){
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		
		//Start and Expire Date
		Date startDate = new Date();
		Calendar c = new GregorianCalendar();
		c.add(Calendar.DATE, 90);
		Date expireDate = c.getTime();
		
		//Serial number
		BigInteger serialNumber = new BigInteger(256, new Random());
		
		//Generate Self-Signed Certificate
		X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
		X500Principal dnName = new X500Principal(_name);
		certGen.setSerialNumber(serialNumber);
		certGen.setSubjectDN(dnName);
		certGen.setIssuerDN(dnName);
		certGen.setNotBefore(startDate);
		certGen.setNotAfter(expireDate);
		certGen.setPublicKey(_pubInfo);
		certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
		///Extensions for v3 certificate
		certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
	    certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature
	        | KeyUsage.keyEncipherment));
	    certGen.addExtension(X509Extensions.ExtendedKeyUsage, true, new ExtendedKeyUsage( KeyPurposeId.id_kp_serverAuth));
	    certGen.addExtension(X509Extensions.SubjectAlternativeName, false, new GeneralNames(
	        new GeneralName(GeneralName.rfc822Name, "test@test.test")));
	    
	    
		try {
			_pubKeyCert = certGen.generateX509Certificate(_privInfo, "BC");
		} catch (InvalidKeyException | NoSuchProviderException
				| SecurityException | SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void printX509Certificate(){
		try {
			_pubKeyCert.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			// TODO Auto-generated catch block
			System.out.println("The certificates date is invalid.");
			e.printStackTrace();
		}
		if(debug) System.out.println(_pubKeyCert.getIssuerDN().getName());
		BASE64Encoder b64 = new BASE64Encoder();
		if(debug) System.out.println("Certificate public key: " + b64.encode(_pubKeyCert.getPublicKey().getEncoded()));
	}
	
	public void logout(){
		_con.disconnect();
	}
	
	public void addFriend(){
		
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

	public Roster get_roster() {
		return _roster;
	}

	public String get_pubKey() {
		return _pubKey;
	}

	public X509Certificate get_pubKeyCert() {
		return _pubKeyCert;
	}

	public String get_privKey() {
		return _privKey;
	}

	public PrivateKey get_privInfo() {
		return _privInfo;
	}

	public PublicKey get_pubInfo() {
		return _pubInfo;
	}
	
	
	
}
