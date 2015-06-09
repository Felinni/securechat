# securechat
XMPP based chat with both bouncycastle and java encryption systems

### Libraries used
  - bcprov-ext-jdk15on-152.jar (bouncycastle)
  - smack-3.2.2.jar (XMPP)
  - smackx-3.2.1.jar (XMPP)
  - sun.misc.BASE64Decoder.jar (string-byte conversion)


### Notes
The chat is made in the console and it has a very [basic] implementation of an XMPP chat.
There is still a lot of things that can be improved in the security mechanisms.
It uses self-signed public key certificates (X509) from bouncycastle that are deprecated.


### Improvements
  - Deploy in a good platform
  - Use better mechanism for nonces
  - Use better mechanism for initialization vector of CBT
  - Change the certificate mechanism
  - Change symmetric key size (currently 128bit due to java policies)
