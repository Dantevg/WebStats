package nl.dantevg.webstats.webserver;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import nl.dantevg.webstats.WebStats;
import nl.dantevg.webstats.WebStatsConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.util.Date;
import java.util.Enumeration;
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
		boolean needsRenewal = checkCertificatesExpiration(keyStore);
		sslContext.init(HTTPSWebServer.getKeyManagers(keyStore, config.keystorePassword),
				HTTPSWebServer.getTrustManagers(keyStore), null);
		server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
			public void configure(HttpsParameters params) {
				SSLContext ctx = getSSLContext();
				params.setSSLParameters(ctx.getDefaultSSLParameters());
			}
		});
		
		if (needsRenewal && config.automatic) {
			File pluginFolder = WebStats.getPlugin(WebStats.class).getDataFolder();
			ACME acme = new ACME(pluginFolder,
					config.email,
					config.domain,
					config.token,
					new File(pluginFolder, config.keystoreFile),
					config.keystorePassword);
			Bukkit.getScheduler().runTaskAsynchronously(WebStats.getPlugin(WebStats.class), () -> {
				WebStats.logger.log(Level.INFO, "Renewing TLS certificate...");
				try {
					acme.renew();
				} catch (IOException | InterruptedException e) {
					WebStats.logger.log(Level.SEVERE, "Failed to renew TLS certificate!", e);
				}
			});
		}
	}
	
	/**
	 * Check if any of the certificates in the keystore are expired or will expire soon.
	 *
	 * @param keyStore the keystore containing the certificates to check
	 * @return whether any of the certificates expire within one month
	 */
	private boolean checkCertificatesExpiration(KeyStore keyStore) throws KeyStoreException {
		boolean needsRenewal = false;
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			if (checkCertificateExpiration((X509Certificate) keyStore.getCertificate(alias))) {
				needsRenewal = true;
			}
		}
		return needsRenewal;
	}
	
	/**
	 * Check if the certificate has expired or will expire soon.
	 *
	 * @param certificate the certificate to check
	 * @return whether the certificate expires within one month, and thus needs to be renewed soon
	 */
	private boolean checkCertificateExpiration(X509Certificate certificate) {
		Date expiration = certificate.getNotAfter();
		Date now = Date.from(Instant.now());
		Date oneWeekFromNow = Date.from(Instant.now().plus(Period.ofWeeks(1)));
		Date oneMonthFromNow = Date.from(Instant.now().plus(Period.ofMonths(1)));
		DateFormat formatter = DateFormat.getDateInstance();
		
		if (expiration.before(now)) {
			WebStats.logger.log(Level.SEVERE, String.format(
					"The TLS certificate has expired on %s! See https://github.com/Dantevg/WebStats/wiki/HTTPS#in-plugin-https for more info.",
					formatter.format(expiration)));
		} else if (expiration.before(oneWeekFromNow)) {
			WebStats.logger.log(Level.WARNING, String.format(
					"The TLS certificate expires on %s (in %d days)! Make sure to renew it before that time, see https://github.com/Dantevg/WebStats/wiki/HTTPS#in-plugin-https for more info.",
					formatter.format(expiration),
					Duration.between(Instant.now(), expiration.toInstant()).toDays()));
		} else {
			WebStats.logger.log(Level.INFO, String.format(
					"The TLS certificate should be valid until %s.",
					formatter.format(expiration)));
		}
		
		return expiration.before(oneMonthFromNow);
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
