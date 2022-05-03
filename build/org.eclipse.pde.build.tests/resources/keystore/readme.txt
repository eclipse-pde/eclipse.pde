How to generate a new keystore

Taken from:
http://wiki.eclipse.org/Generating_a_Private_Key_and_a_Keystore

Command to generate keystore. Validity is the length of time in days.
keytool -genkey -alias pde.build -keyalg DSA -keystore keystore -validity 1000

Command to list the keystore.
keytool -list -v -keystore keystore

key password = keypass
store password = storepass

*******************************************

Keystore type: JKS
Keystore provider: SUN

Your keystore contains 1 entry

Alias name: pde.build
Creation date: Nov 16, 2011
Entry type: PrivateKeyEntry
Certificate chain length: 1
Certificate[1]:
Owner: CN=Test Certificate, OU=PDE Build, O=Eclipse, L=Ottawa, ST=ON, C=CA
Issuer: CN=Test Certificate, OU=PDE Build, O=Eclipse, L=Ottawa, ST=ON, C=CA
Serial number: 4ec3fef7
Valid from: Wed Nov 16 13:20:39 EST 2011 until: Tue Aug 12 14:20:39 EDT 2014
Certificate fingerprints:
	 MD5:  E6:BA:31:FA:E5:84:C9:FC:F2:8B:55:DC:37:9A:68:94
	 SHA1: 80:97:F5:64:5D:19:6A:AD:0F:B3:C7:FC:53:03:F2:70:31:D1:08:17
	 Signature algorithm name: SHA1withDSA
	 Version: 3


*******************************************
