package nl.dantevg.webstats.webserver;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.WebStatsConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Level;

public class HTTPSWebServer extends WebServer<HttpsServer> {
	private static final String KEYSTORE_TYPE = "PKCS12";
	private static final String MANAGER_TYPE = "PKIX";
	
	private final HTTPSConfig config;
	
	public HTTPSWebServer()
			throws IOException, NoSuchAlgorithmException, KeyStoreException,
			CertificateException, UnrecoverableKeyException, KeyManagementException, InvalidConfigurationException {
		WebStats.logger.log(Level.INFO, "Enabling web server");
		
		config = HTTPSConfig.getInstance(true);
		port = WebStatsConfig.getInstance().port;
		server = HttpsServer.create(new InetSocketAddress(port), 0);
		
		// https://stackoverflow.com/a/2323188
		
		SSLContext sslContext = SSLContext.getInstance("TLS");
		KeyStore keyStore = HTTPSWebServer.getKeyStore(config.keystoreFile, config.keystorePassword);
		sslContext.init(HTTPSWebServer.getKeyManagers(keyStore, config.keystorePassword),
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

class HTTPSConfig {
	private static HTTPSConfig instance;
	
	public final String keystoreFile;
	public final String keystorePassword;
	
	private HTTPSConfig() throws InvalidConfigurationException {
		ConfigurationSection section = WebStats.config.getConfigurationSection("https");
		if (section == null) {
			throw new InvalidConfigurationException("Invalid configuration: https should be a yaml object");
		}
		
		keystoreFile = section.getString("keystore-file");
		keystorePassword = section.getString("keystore-password");
		if (keystoreFile == null || keystorePassword == null) {
			throw new InvalidConfigurationException("Invalid configuration: keystore-file and keystore-password are required for HTTPS. If you do not want HTTPS, comment it out.");
		}
	}
	
	public static HTTPSConfig getInstance(boolean forceNew) throws InvalidConfigurationException {
		if (instance == null || forceNew) instance = new HTTPSConfig();
		return instance;
	}
	
	public static HTTPSConfig getInstance() throws InvalidConfigurationException {
		return getInstance(false);
	}
	
}
