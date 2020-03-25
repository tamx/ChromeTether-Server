package jp.cane.java.session;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

public class DH {
	private final static BigInteger P = new BigInteger(
			"179598475992789387538485788906703359462983988971716496975621221988212618278309021198885480551845548805991133255245593827023799315453962583455295401815758428825225271783480618112038184911887757080805399104954977661918302058569295713941361644318738683467384620592914363568397460684145936831890089213238436589571");
	private final static BigInteger G = new BigInteger(
			"57732654083335121699282604784479149038566240973648311100893335360147320721043739551528266025824352034877065492456626152121996989698744363606766938771450281444585470358931881028241276041756726627227281116637889826395079856106918302381087102131502900278929612631306789092654131027222464365237756594245034893555");

	private DHParameterSpec ownParamSpec;
	private KeyPair ownKeyPair;

	public DH() {
		init(P, G);
	}

	public DH(BigInteger p, BigInteger g) {
		init(p, g);
	}

	private void init(BigInteger p, BigInteger g) {
		ownParamSpec = new DHParameterSpec(p, g);

		KeyPairGenerator keyPairGen = null;
		try {
			keyPairGen = KeyPairGenerator.getInstance("DH");
			keyPairGen.initialize(ownParamSpec);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}

		ownKeyPair = keyPairGen.generateKeyPair();
	}

	public DHPublicKey getPublic() {
		return (DHPublicKey) ownKeyPair.getPublic();
	}

	public BigInteger computeSecret(BigInteger y) {
		DHPublicKeySpec opposidePublicKeySpec = new DHPublicKeySpec(y, P, G);

		DHPublicKey opposidePublicKey = null;
		try {
			opposidePublicKey = (DHPublicKey) KeyFactory.getInstance("DH")
					.generatePublic(opposidePublicKeySpec);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}

		return computeSecret(opposidePublicKey);
	}

	public BigInteger computeSecret(DHPublicKey opposidePublicKey) {
		KeyAgreement keyAgreement = null;
		try {
			keyAgreement = KeyAgreement.getInstance("DH");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		try {
			keyAgreement.init(ownKeyPair.getPrivate(), ownParamSpec);
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}

		try {
			keyAgreement.doPhase(opposidePublicKey, true);
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		}

		return new BigInteger(1, keyAgreement.generateSecret());
	}

	public static void main(String[] args) throws Exception {
		DH client = new DH();
		DH server = new DH();
		BigInteger clientSecret = client.computeSecret(server.getPublic()
				.getY());
		BigInteger serverSecret = server.computeSecret(client.getPublic()
				.getY());
		System.out.println("clientSecret: " + clientSecret.toString());
		System.out.println("serverSecret: " + serverSecret.toString());
		MessageDigest digest = MessageDigest.getInstance("MD5");
		digest.update(serverSecret.toString().getBytes());
		byte[] key = digest.digest();
		for (int i = 0; i < key.length; i++) {
			System.out.print(String.format(":%02x", key[i]));
		}
		System.out.println();
	}
}
