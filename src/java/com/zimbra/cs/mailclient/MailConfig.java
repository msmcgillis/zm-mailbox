package com.zimbra.cs.mailclient;

import com.zimbra.cs.mailclient.auth.SaslAuthenticator;
import com.zimbra.cs.mailclient.auth.AuthenticatorFactory;

import javax.net.ssl.SSLSocketFactory;
import java.util.Map;
import java.util.HashMap;
import java.io.PrintStream;

/**
 * Mail client configuration settings.
 */
public abstract class MailConfig {
    protected String host;
    protected int port = -1;
    protected boolean sslEnabled;
    protected boolean tlsEnabled;
    protected String authorizationId;
    protected String authenticationId;
    protected String mechanism;
    protected String realm;
    protected Map<String, String> saslProperties;
    protected boolean debug;
    protected boolean trace;
    protected PrintStream traceOut;
    protected SSLSocketFactory sslSocketFactory;
    protected AuthenticatorFactory authenticatorFactory;
    protected int timeout;
    protected boolean rawMode;

    public MailConfig() {
        traceOut = System.out;
        authenticationId = System.getProperty("user.name");
    }

    public MailConfig(String host, boolean sslEnabled) {
        this.host = host;
        this.sslEnabled = sslEnabled;
    }

    public abstract String getProtocol();
                                                             
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setDebug(boolean debug) { this.debug = debug; }
    public void setTrace(boolean trace) { this.trace = trace; }
    public void setRealm(String realm) { this.realm = realm; }
    
    public boolean isTrace() { return trace; }
    public boolean isDebug() { return debug; }
    public boolean isSslEnabled() { return sslEnabled; }
    public boolean isTlsEnabled() { return tlsEnabled; }
    public String getMechanism() { return mechanism; }
    public String getHost() { return host; }
    public String getRealm() { return realm; }
    public int getPort() { return port; }
    public PrintStream getTraceOut() { return traceOut; }
    public int getTimeout() { return timeout; }

    public void setMechanism(String mech) {
        mechanism = mech;
    }

    public void setTraceStream(PrintStream ps) {
        traceOut = ps;
    }

    public void setSslEnabled(boolean enabled) {
        sslEnabled = enabled;
    }

    public void setTlsEnabled(boolean enabled) {
        tlsEnabled = enabled;
    }
    
    public void setAuthorizationId(String id) {
        authorizationId = id;
    }

    public void setAuthenticationId(String id) {
        authenticationId = id;
    }

    public String getAuthenticationId() {
        return authenticationId;
    }

    public String getAuthorizationId() {
        return authorizationId;
    }

    public void setSaslProperty(String name, String value) {
        if (saslProperties == null) {
            saslProperties = new HashMap<String, String>();
        }
        saslProperties.put(name, value);
    }

    public Map<String, String> getSaslProperties() {
        return saslProperties;
    }

    public String getSaslProperty(String name) {
        return saslProperties != null ? saslProperties.get(name) : null;
    }

    public void setTimeout(int secs) {
        timeout = secs;
    }

    public void setSSLSocketFactory(SSLSocketFactory sf) {
        sslSocketFactory = sf;
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return sslSocketFactory;
    }

    public boolean isPasswordRequired() {
        return mechanism == null ||
               mechanism.equals(SaslAuthenticator.MECHANISM_PLAIN);
    }

    public AuthenticatorFactory getAuthenticatorFactory() {
        return authenticatorFactory;
    }

    public void setAuthenticatorFactory(AuthenticatorFactory af) {
        authenticatorFactory = af;
    }
    
    public void setRawMode(boolean raw) {
        rawMode = raw;
    }

    public boolean isRawMode() {
        return rawMode;
    }
}
