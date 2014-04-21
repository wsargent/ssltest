
> **NOTE**: The following configuration requires JDK 1.8 and is VERY strict.

Start with the server with the following system properties for a more secure server handshake:

```
-Djdk.tls.ephemeralDHKeySize=2048
```

```
-Dsun.security.ssl.allowUnsafeRenegotiation=false
-Dsun.security.ssl.allowLegacyHelloMessages=false
```

Restrict client protocols globally as only TLS v1.2:

```
-Djdk.tls.client.protocols=TLSv1.2
```

Set up client authentication with the remote server by [[generating certificates|CertificateGeneration]] using an [ECDSA signature algorithm](https://tools.ietf.org/html/rfc4492) with `-keyalg EC`.  Note that you want

For additional security, check the key sizes match up on ECDHE_ECDSA cipher and the ECDSA signature algorithm:

```
# disabledAlgorithms.properties file
jdk.tls.disabledAlgorithms=<default settings>, ECDHE_ECDSA keySize < 2048
jdk.certpath.disabledAlgorithms=<default settings>, EC keySize < 256
```

And append the [security properties file](http://bugs.java.com/bugdatabase/view_bug.do?bug_id=7133344):

```
-Djava.security.properties=disabledAlgorithms.properties
```

Then, configure the client to only use TLS 1.2 and [Suite B 192-bit mode](https://tools.ietf.org/html/rfc5430):

```
ws.ssl {

  protocol = "TLSv1.2"

  enabledProtocols = [ "TLSv1.2" ]

  enabledCiphers = [
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
  ]

  ws.ssl.disabledSignatureAlgorithms = "MD2, MD4, MD5, SHA1, RSA"

  ws.ssl.disabledKeyAlgorithms = "EC keySize < 384"

  keyManager = {
    stores = [
      { type: "PKCS12", path: "keys/client.p12", password: $KEY_PASSWORD },
    ]
  }

  trustManager = {
    stores = [
      { type = "JKS", path = "truststore.jks" }
    ]
  }
}
```

Note the use of $KEY_PASSWORD to ensure that the password is pulled from the environment variable and not stored on file.

WS SSL does not implement [certificate pinning](http://www.thoughtcrime.org/blog/authenticity-is-broken-in-ssl-but-your-app-ha/), but this would be a useful additional step for more security.


http://www.juniper.net/techpubs/en_US/sa8.0/topics/reference/general/secure-access-ecc-about.html