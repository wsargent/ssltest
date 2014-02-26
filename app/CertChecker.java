/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */

/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.*;
import java.util.*;

/**
 * http://stackoverflow.com/questions/1694466/how-can-i-verify-that-a-certificate-is-an-ev-certificate-with-java
 */
public class CertChecker implements X509TrustManager {

    public static void main(String[] args) throws Exception {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[]{new CertChecker()}, new SecureRandom());
        SSLSocketFactory ssf = sc.getSocketFactory();
        ((SSLSocket) ssf.createSocket("banking.dkb.de", 443)).startHandshake();
    }


    private final X509TrustManager defaultTM;

    public CertChecker() throws GeneralSecurityException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        defaultTM = (X509TrustManager) tmf.getTrustManagers()[0];
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType) {
        if (defaultTM != null) {
            try {
                defaultTM.checkServerTrusted(certs, authType);
                Set<TrustAnchor> trustAnchors = getTrustAnchors();

                if (isEVCertificate(trustAnchors, certs))
                    System.out.println("EV Certificate: " + certs[0].getSubjectX500Principal().getName() + " issued by " + certs[0].getIssuerX500Principal().getName());
                System.out.println("Certificate valid");
            } catch (CertificateException ex) {
                System.out.println("Certificate invalid: " + ex.getMessage());
            }
        }
    }

    private Set<TrustAnchor> getTrustAnchors() {
        X509Certificate[] acceptedIssuers = defaultTM.getAcceptedIssuers();
        Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
        for (X509Certificate acceptedIssuer : acceptedIssuers) {
            TrustAnchor trustAnchor = new TrustAnchor(acceptedIssuer, null);
            trustAnchors.add(trustAnchor);
        }
        return trustAnchors;
    }

    private boolean isEVCertificate(Set<TrustAnchor> trustAnchors, X509Certificate[] certs) {
        try {

            // load keystore with trusted CA certificates

            // build a cert selector that selects the first certificate of the certificate chain
            // TODO we should verify this against the hostname...
            X509CertSelector targetConstraints = new X509CertSelector();
            targetConstraints.setSubject(certs[0].getSubjectX500Principal());

            // build a cert path from our selected cert to a CA cert
            PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetConstraints);
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(Arrays.asList(certs))));
            params.setRevocationEnabled(false);
            CertPath cp = CertPathBuilder.getInstance("PKIX").build(params).getCertPath();

            // validate the cert path
            PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) CertPathValidator.getInstance("PKIX").validate(cp, params);
            return isEV(result);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    // based on http://stackoverflow.com/questions/1694466/1694720#1694720
    static boolean isEV(PKIXCertPathValidatorResult result) {


        final Map<X500Principal, String> policies = new HashMap<X500Principal, String>();

        // It would make sense to populate this map from Properties loaded through
        // Class.getResourceAsStream().
        policies.put(
                new X500Principal("OU=Class 3 Public Primary Certification Authority,O=VeriSign\\, Inc.,C=US"),
                "2.16.840.1.113733.1.7.23.6"
        );

        // Determine the policy to look for.
        X500Principal root = result.getTrustAnchor().getTrustedCert().getSubjectX500Principal();
        System.out.println("[Debug] Found root DN: " + root.getName());
        String policy = policies.get(root);
        if (policy != null)
            System.out.println("[Debug] EV Policy should be: " + policy);

        // Traverse the tree, looking at its "leaves" to see if the end-entity
        // certificate was issued under the corresponding EV policy.
        PolicyNode tree = result.getPolicyTree();
        if (tree == null)
            return false;
        Deque<PolicyNode> stack = new ArrayDeque<PolicyNode>();
        stack.push(tree);
        while (!stack.isEmpty()) {
            PolicyNode current = stack.pop();
            Iterator<? extends PolicyNode> children = current.getChildren();
            int leaf = stack.size();
            while (children.hasNext())
                stack.push(children.next());
            if (stack.size() == leaf) {
                System.out.println("[Debug] Found policy: " + current.getValidPolicy());
                // If the stack didn't grow, there were no "children". I.e., the
                // current node is a "leaf" node of the policy tree.
                if (current.getValidPolicy().equals(policy))
                    return true;
            }
        }
        // The certificate wasn't issued under the authority's EV policy.
        return false;
    }
}