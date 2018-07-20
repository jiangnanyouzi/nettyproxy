package com.jiangnanyouzi.nettyproxy.utils;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/**
 * 产生证书
 * Created by jiangnan on 2018/4/12.
 */
public abstract class CertificateUtils {

    private static Logger logger = LoggerFactory.getLogger(CertificateUtils.class);
    private static URL crtFilePath = ProxyConstant.CERT_FILE;
    private static URL derFilePath = ProxyConstant.PRIVATEKEY_FILE;
    private static KeyFactory keyFactory;
    private static Map<String, X509Certificate> certCache = new HashMap<>();


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

    public static X509Certificate load() {

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(crtFilePath.openStream());
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Load File Fail , path %s", crtFilePath), e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

    }

    public static PrivateKey loadPrivateKey(byte[] bts) {
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bts);
        if (keyFactory == null) {
            try {
                keyFactory = KeyFactory.getInstance("RSA");
                return KeyFactory.getInstance("RSA").generatePrivate(privateKeySpec);
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

    }


    public static X509Certificate generateChildX509Certificate(X509Certificate sources, PublicKey publicKey, String hosts) {

        if (certCache.containsKey(hosts.toLowerCase())) {
            return certCache.get(hosts.toLowerCase());
        }

        List<String> issuerList = Arrays.asList(StringUtils.split(sources.getIssuerDN().getName(), ", "));
        Collections.reverse(issuerList);

        String issuerName = StringUtils.join(issuerList, ", ");
        String subjectName = issuerName.replaceAll("CN=.*", "CN=" + hosts);

        logger.info("subject {}", subjectName);
        logger.info("issuer1 {}", issuerName);

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis() + (long) (Math.random() * 10000) + 1000);
        X500Name issuer = new X500Name(issuerName);
        X500Name subject = new X500Name(subjectName);
        Date notBefore = sources.getNotBefore();
        Date notAfter = sources.getNotAfter();
        try {
            //PrivateKey privateKey = loadPrivateKey(FileUtils.readFileToByteArray(new File(derFilePath)));
            PrivateKey privateKey = PrivateKeyUtils.read(derFilePath);
            JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(
                    issuer, serial, notBefore, notAfter, subject, publicKey);

            GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, hosts));
            jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
            ContentSigner signer = new JcaContentSignerBuilder(ProxyConstant.CA_SHA).build(privateKey);
            X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
            certCache.put(hosts.toLowerCase(), certificate);
            return certificate;
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("load File Fail , path %s", derFilePath), e);
        } catch (OperatorCreationException | CertificateException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }


    public static String convertToString(X509Certificate x509Certificate) {
        String str = null;
        try {
            str = DatatypeConverter.printBase64Binary(x509Certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            logger.error("x509 Certificate Can not Encoding");
        }

        if (StringUtils.isBlank(str)) {
            return null;
        }

        return "-----BEGIN CERTIFICATE-----\n" +
                str.replaceAll("(.{64})", "$1\n") +
                "\n" +
                "-----END CERTIFICATE-----\n";
    }
}
