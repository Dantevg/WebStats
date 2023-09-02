package nl.dantevg.webstats.webserver;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import nl.dantevg.webstats.WebStats;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.logging.Level;

public class HTTPSWebServer extends WebServer<HttpsServer> {
	private static final String KEYSTORE_TYPE = "PKCS12";
	private static final String MANAGER_TYPE = "PKIX";
	
	public HTTPSWebServer()
			throws IOException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, UnrecoverableKeyException, KeyManagementException, InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling web server");
		
		ConfigurationSection section = WebStats.config.getConfigurationSection("https");
		if (section == null) {
			throw new InvalidConfigurationException("Invalid configuration: https should be a yaml object");
		}
		
		port = WebStats.config.getInt("port");
		String keystoreFile = section.getString("keystore");
		String keystorePassword = section.getString("keystore-password");
		if (keystoreFile == null || keystorePassword == null) {
			throw new InvalidConfigurationException("Invalid configuration: keystore and keystore-password are required for HTTPS. If you do not want HTTPS, comment it out.");
		}
		
		server = HttpsServer.create(new InetSocketAddress(port), 0);
		
		// https://stackoverflow.com/a/2323188
		
		SSLContext sslContext = SSLContext.getInstance("TLS");
		KeyStore keyStore = HTTPSWebServer.getKeyStore(keystoreFile, keystorePassword);
		sslContext.init(HTTPSWebServer.getKeyManagers(keyStore, keystorePassword),
				HTTPSWebServer.getTrustManagers(keyStore), null);
		server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
			public void configure(HttpsParameters params) {
				SSLContext ctx = getSSLContext();
				params.setSSLParameters(ctx.getDefaultSSLParameters());
			}
		});
	}
	
	private static KeyStore getKeyStore(String keystoreFile, String keystorePassword)
			throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
		KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
		keyStore.load(WebStats.getResourceInputStream(keystoreFile), keystorePassword.toCharArray());
		return keyStore;
	}
	
	private static KeyManager[] getKeyManagers(KeyStore keyStore, String keystorePassword)
			throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(MANAGER_TYPE);
		keyManagerFactory.init(keyStore, keystorePassword.toCharArray());
		return keyManagerFactory.getKeyManagers();
	}
	
	private static TrustManager[] getTrustManagers(KeyStore keyStore)
			throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(MANAGER_TYPE);
		trustManagerFactory.init(keyStore);
		return trustManagerFactory.getTrustManagers();
	}
	
}
