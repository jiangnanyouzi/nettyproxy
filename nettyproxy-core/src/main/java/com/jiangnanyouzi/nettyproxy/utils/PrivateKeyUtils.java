package com.jiangnanyouzi.nettyproxy.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Created by jiangnan on 2018/4/12.
 */
public abstract class PrivateKeyUtils {


    public static void save(File file, PrivateKey privateKey, OutputEncryptor encryptor) throws IOException {
        JcaPKCS8Generator jcaPKCS8Generator = new JcaPKCS8Generator(privateKey, encryptor);

        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(stringWriter)) {
            pw.writeObject(jcaPKCS8Generator.generate());
        }

        FileUtils.writeByteArrayToFile(file, stringWriter.toString().getBytes());

    }


    public static PrivateKey read(URL url) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

        String unencrypted = IOUtils.toString(url, "UTF-8");
        unencrypted = unencrypted.replace("-----BEGIN PRIVATE KEY-----", "");
        unencrypted = unencrypted.replace("-----END PRIVATE KEY-----", "");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(unencrypted)));

    }


    public static PrivateKey readEncryptedPrivateKey(URL url, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {


        String encrypted = IOUtils.toString(url, "UTF-8");
        encrypted = encrypted.replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "");
        encrypted = encrypted.replace("-----END ENCRYPTED PRIVATE KEY-----", "");
        EncryptedPrivateKeyInfo pkInfo = new EncryptedPrivateKeyInfo(Base64.decode(encrypted));
        SecretKeyFactory pbeKeyFactory = SecretKeyFactory.getInstance(pkInfo.getAlgName());
        PKCS8EncodedKeySpec encodedKeySpec = pkInfo.getKeySpec(pbeKeyFactory.generateSecret(new PBEKeySpec(password.toCharArray())));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(encodedKeySpec);

    }


}
