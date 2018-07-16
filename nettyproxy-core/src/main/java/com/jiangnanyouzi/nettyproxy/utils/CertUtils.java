package com.jiangnanyouzi.nettyproxy.utils;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/**
 * @author
 * @create 2018-03-25 16:03
 **/
public class CertUtils {

    private static Logger logger = LoggerFactory.getLogger(CertUtils.class);
    private static URL crtFilePath = ProxyConstant.CERT_FILE;
    private static URL derFilePath = ProxyConstant.PRIVATEKEY_FILE;
    private static KeyFactory keyFactory;
    private static Map<String, X509Certificate> certCache = new HashMap<>();

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


    public static X509Certificate genCert(X509Certificate sources, PublicKey publicKey, String hosts) {

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
