package com.jiangnanyouzi.nettyproxy.utils;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * 产生证书
 * Created by jiangnan on 2018/4/12.
 */
public abstract class CertificateUtils {

    private static Logger logger = LoggerFactory.getLogger(CertificateUtils.class);


    public static void saveX509Certificate() throws Exception {

        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.genKeyPair();

        X500Name x500Name = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.C, ProxyConstant.CA_C)
                .addRDN(BCStyle.ST, ProxyConstant.CA_ST)
                .addRDN(BCStyle.L, ProxyConstant.CA_L)
                .addRDN(BCStyle.O, ProxyConstant.CA_O)
                .addRDN(BCStyle.OU, ProxyConstant.CA_OU)
                .addRDN(BCStyle.CN, ProxyConstant.CA_CN)
                .build();

        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + 730 * 48 * 3600000L);//two year

        saveX509Certificate(x500Name, keyPair, BigInteger.valueOf(1), notBefore, notAfter);


    }


    public static void saveX509Certificate(X500Name x500Name, KeyPair keyPair, BigInteger serial, Date notBefore, Date notAfter) throws Exception {

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                x500Name, serial, notBefore, notAfter, x500Name, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, false, new BasicConstraints(true));

        //builder.addExtension(Extension.subjectKeyIdentifier, false, new JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));
        //builder.addExtension(Extension.authorityKeyIdentifier, false, new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(keyPair.getPublic()));

        ContentSigner signer = new JcaContentSignerBuilder(ProxyConstant.CA_SHA).setProvider("BC").build(keyPair.getPrivate());
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(builder.build(signer));
        savePem(certificate.getEncoded(), ProxyConstant.CERT_FILE);
        PrivateKeyUtils.save(new File(ProxyConstant.PRIVATEKEY_FILE.toURI()), keyPair.getPrivate(), null);
        logger.info("save cert success, path {}", ProxyConstant.CERT_FILE);
        logger.info("save privatekey success, path {}", ProxyConstant.PRIVATEKEY_FILE);

    }

    public static void savePem(byte[] content, URL file) throws IOException, URISyntaxException {

        PemWriter pemWriter = null;
        try {
            pemWriter = new PemWriter(new OutputStreamWriter(new FileOutputStream(new File(file.toURI()))));
            pemWriter.writeObject(new PemObject("CERTIFICATE", content));
        } finally {
            try {
                if (pemWriter != null) {
                    pemWriter.close();
                }
            } catch (IOException ignore) {

            }
        }

    }
}
