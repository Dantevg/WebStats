package nl.dantevg.webstats.webserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ACME {
	private final String homePath;
	private final String email;
	private final String domain;
	private final String token;
	private final File keystoreFile;
	private final String keystorePassword;
	
	private final File acmeDir;
	
	public ACME(File home, String email, String subdomain, String token, File keystoreFile, String keystorePassword) {
		this.homePath = home.getAbsolutePath();
		this.email = email;
		this.domain = subdomain + ".duckdns.org";
		this.token = token;
		this.keystoreFile = keystoreFile;
		this.keystorePassword = keystorePassword;
		
		acmeDir = new File(home, "acme");
	}
	
	/**
	 * Renew the certificate. Should be run on a separate thread, because it
	 * will wait for each acme.sh process to complete. Can take several minutes.
	 *
	 * @return whether the certificate was renewed or issued successfully.
	 */
	boolean renew() throws IOException, InterruptedException {
		boolean success;
		if (acmeDir.isDirectory()) {
			success = acme(getUpdateCommand());
		} else {
			success = acme(getInstallCommand());
		}
		if (!success) return false;
		
		if (keystoreFile.isFile()) {
			success = acme(getRenewCommand());
		} else {
			success = acme(getIssueCommand());
		}
		if (!success) return false;
		
		return acme(getConvertCommand());
	}
	
	/**
	 * Run an acme.sh step with the right environment variables and directory.
	 *
	 * @param command the command to run
	 * @return whether the command was successful
	 */
	private boolean acme(List<String> command) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(command);
		Map<String, String> env = builder.environment();
		env.put("DuckDNS_Token", token);
		env.put("CERT_PFX_PATH", keystoreFile.getAbsolutePath());
		builder.directory(acmeDir);
		return builder.start().waitFor() == 0;
	}
	
	private List<String> getInstallCommand() {
		return Arrays.asList("./acme.sh", "--home", homePath, "--install-online", "--no-cron", "--no-profile", "--email", email);
	}
	
	private List<String> getUpdateCommand() {
		return Arrays.asList("./acme.sh", "--home", homePath, "--upgrade", "--no-cron", "--no-profile");
	}
	
	private List<String> getIssueCommand() {
		return Arrays.asList("./acme.sh", "--home", homePath, "--server", "letsencrypt", "--issue", "--dns", "dns_duckdns", "-d", domain);
	}
	
	private List<String> getRenewCommand() {
		return Arrays.asList("./acme.sh", "--home", homePath, "--server", "letsencrypt", "--renew", "-d", domain, "--force");
	}
	
	private List<String> getConvertCommand() {
		return Arrays.asList("./acme.sh", "--home", homePath, "--to-pkcs12", "-d", domain, "--password", keystorePassword);
	}
}
