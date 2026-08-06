import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

/// Minimal curl-like tool: GET a URL, optionally using a custom trust store.
///
/// Usage:
///   java TrustStoreCheck.java <url> [trustStorePath] [trustStorePassword]
///
/// If trustStorePath is omitted, the JVM's default trust store is used.
void main(String[] args) throws Exception {
    if (args.length < 1) {
        System.err.println("Usage: java TrustStoreCheck.java <url> [trustStorePath] [trustStorePassword]");
        System.exit(1);
    }

    var url = URI.create(args[0]);
    var trustStorePath = args.length > 1 ? args[1] : null;
    var trustStorePassword = args.length > 2 ? args[2] : "changeit";

    var clientBuilder = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL);

    if (trustStorePath != null) {
        clientBuilder.sslContext(sslContextFromTrustStore(trustStorePath, trustStorePassword));
    }

    var client = clientBuilder.build();
    var request = HttpRequest.newBuilder(url).GET().build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    response.sslSession().ifPresent(this::printPeerCertificate);

    System.out.println("HTTP " + response.statusCode());
    response.headers().map().forEach((name, values) ->
            values.forEach(value -> System.out.println(name + ": " + value)));
    System.out.println();
    System.out.println(response.body());
}

SSLContext sslContextFromTrustStore(String path, String password) throws Exception {
    var type = path.toLowerCase().endsWith(".p12") || path.toLowerCase().endsWith(".pfx")
            ? "PKCS12"
            : "JKS";
    var trustStore = KeyStore.getInstance(type);
    try (var in = new FileInputStream(path)) {
        trustStore.load(in, password.toCharArray());
    }

    var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);

    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, tmf.getTrustManagers(), null);
    return sslContext;
}

void printPeerCertificate(SSLSession session) {
    try {
        var cert = (X509Certificate) session.getPeerCertificates()[0];
        System.out.println("Peer certificate:");
        System.out.println("  Subject: " + cert.getSubjectX500Principal());
        System.out.println("  Issuer:  " + cert.getIssuerX500Principal());
        System.out.println("  Valid:   " + cert.getNotBefore() + " -> " + cert.getNotAfter());
        System.out.println();
    } catch (Exception e) {
        System.err.println("Could not read peer certificate: " + e.getMessage());
    }
}