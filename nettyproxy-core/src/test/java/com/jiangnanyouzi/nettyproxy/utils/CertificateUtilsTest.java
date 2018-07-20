package com.jiangnanyouzi.nettyproxy.utils;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.pem.PemObject;
import org.junit.Test;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

public class CertificateUtilsTest {

    @Test
    public void saveX509Certificate() throws Exception {

       /*
            //C表示国家(Country)，只能是国家字母缩写，如CN、US等；
            ProxyConstant.CA_C = "CN";
            //ST表示州或者省(State/Provice)
            ProxyConstant.CA_ST = "GD";
            //L表示城市或者地区(Locality)
            ProxyConstant.CA_L = "SZ";
            //O表示组织名(Organization Name)
            ProxyConstant.CA_O = "io";
            //xx认证中心
            ProxyConstant.CA_OU = "netty";
            //xx认证中心
            ProxyConstant.CA_CN = "proxy";
       */

        CertificateUtils.saveX509Certificate();

    }

    @Test
    public void load() throws Exception {

        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").
                generateCertificate(new FileInputStream(new File(ProxyConstant.CERT_FILE.toURI())));
    }


    @Test
    public void loadPrivateKey() throws Exception {

        String unencrypted = IOUtils.toString(ProxyConstant.PRIVATEKEY_FILE, "UTF-8");
        unencrypted = unencrypted.replace("-----BEGIN PRIVATE KEY-----", "");
        unencrypted = unencrypted.replace("-----END PRIVATE KEY-----", "");
        byte[] encoded = Base64.decode(unencrypted);
        CertificateUtils.loadPrivateKey(encoded);

    }

    @Test
    public void generateX509Certificate() throws Exception {

        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        caKeyPairGen.initialize(2048, new SecureRandom());
        PublicKey publicKey = caKeyPairGen.generateKeyPair().getPublic();

        String host = "www.baidu.com";
        X509Certificate sources = CertificateUtils.load();

        X509Certificate target = CertificateUtils.generateChildX509Certificate(sources, publicKey, host);

        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        sb.append(
                DatatypeConverter.printBase64Binary(target.getEncoded())
                        .replaceAll("(.{64})", "$1\n"));
        sb.append("\n");
        sb.append("-----END CERTIFICATE-----\n");

        System.out.println(sb.toString());


    }


    @Test
    public void savePrivateKey() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = kpGen.generateKeyPair();


        //unencrypted form of PKCS#8 file
        JcaPKCS8Generator gen1 = new JcaPKCS8Generator(keyPair.getPrivate(), null);
        PemObject obj1 = gen1.generate();
        StringWriter sw1 = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw1)) {
            pw.writeObject(obj1);
        }
        String pkcs8Key1 = sw1.toString();
        FileOutputStream fos1 = new FileOutputStream(new File(ProxyConstant.PRIVATEKEY_FILE.toURI()));
        fos1.write(pkcs8Key1.getBytes());
        fos1.flush();
        fos1.close();

        //encrypted form of PKCS#8 file
        //JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_RC2_128);
        //encryptorBuilder.setRandom(new SecureRandom());
        //encryptorBuilder.setPasssword("abcde" .toCharArray()); // password
        //OutputEncryptor encryptor = encryptorBuilder.build();
        //
        //JcaPKCS8Generator gen2 = new JcaPKCS8Generator(keyPair.getPrivate(), encryptor);
        //PemObject obj2 = gen2.generate();
        //StringWriter sw2 = new StringWriter();
        //try (JcaPEMWriter pw = new JcaPEMWriter(sw2)) {
        //    pw.writeObject(obj2);
        //}
        //String pkcs8Key2 = sw2.toString();
        //FileOutputStream fos2 = new FileOutputStream("D:\\privatekey-encrypted.pkcs8");
        //fos2.write(pkcs8Key2.getBytes());
        //fos2.flush();
        //fos2.close();
    }

    @Test
    public void readPrivateKey() throws Exception {


        String unencrypted = IOUtils.toString(ProxyConstant.PRIVATEKEY_FILE, "UTF-8");

        //Create object from unencrypted private key
        unencrypted = unencrypted.replace("-----BEGIN PRIVATE KEY-----", "");
        unencrypted = unencrypted.replace("-----END PRIVATE KEY-----", "");
        byte[] encoded = Base64.decode(unencrypted);
        PKCS8EncodedKeySpec kspec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey unencryptedPrivateKey = kf.generatePrivate(kspec);
        System.out.println(unencryptedPrivateKey);

    }


    @Test
    public void readEncryptedPrivateKey() throws Exception {


        String encrypted = IOUtils.toString(ProxyConstant.PRIVATEKEY_FILE, "UTF-8");
        encrypted = encrypted.replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "");
        encrypted = encrypted.replace("-----END ENCRYPTED PRIVATE KEY-----", "");
        EncryptedPrivateKeyInfo pkInfo = new EncryptedPrivateKeyInfo(Base64.decode(encrypted));

        PBEKeySpec keySpec = new PBEKeySpec("abcde".toCharArray()); // password
        SecretKeyFactory pbeKeyFactory = SecretKeyFactory.getInstance(pkInfo.getAlgName());
        PKCS8EncodedKeySpec encodedKeySpec = pkInfo.getKeySpec(pbeKeyFactory.generateSecret(keySpec));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey encryptedPrivateKey = keyFactory.generatePrivate(encodedKeySpec);

        //comparing both private key for equality
        //System.out.println(unencryptedPrivateKey.equals(encryptedPrivateKey));

    }


    @Test
    public void saveToPrivateKey() throws Exception {

        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = kpGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PrivateKeyUtils.save(new File(ProxyConstant.PRIVATEKEY_FILE.toURI()), privateKey, null);
        PrivateKey fileKey = PrivateKeyUtils.read(ProxyConstant.PRIVATEKEY_FILE);

        System.out.println(privateKey.equals(fileKey));

    }

    @Test
    public void savePasswordToPrivateKey() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
        kpGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = kpGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        String pasword = "abcde";

        JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_RC2_128);
        encryptorBuilder.setRandom(new SecureRandom());
        encryptorBuilder.setPasssword(pasword.toCharArray());
        OutputEncryptor encryptor = encryptorBuilder.build();

        PrivateKeyUtils.save(new File(ProxyConstant.PRIVATEKEY_FILE.toURI()), keyPair.getPrivate(), encryptor);

        PrivateKey fileKey = PrivateKeyUtils.readEncryptedPrivateKey(ProxyConstant.PRIVATEKEY_FILE, pasword);


        System.out.println(privateKey.equals(fileKey));
    }


}