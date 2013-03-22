
package com.akiban.http;

import com.akiban.server.error.AkibanInternalException;
import com.akiban.server.service.Service;
import com.akiban.server.service.config.ConfigurationService;
import com.akiban.server.service.security.SecurityService;
import com.google.inject.Inject;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.JDBCLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class HttpConductorImpl implements HttpConductor, Service {
    private static final Logger logger = LoggerFactory.getLogger(HttpConductorImpl.class);

    private static final String CONFIG_HTTP_PREFIX = "akserver.http.";
    private static final String CONFIG_PORT_PROPERTY = CONFIG_HTTP_PREFIX + "port";
    private static final String CONFIG_SSL_PROPERTY = CONFIG_HTTP_PREFIX + "ssl";
    private static final String CONFIG_LOGIN_PROPERTY = CONFIG_HTTP_PREFIX + "login";
    private static final String CONFIG_XORIGIN_PREFIX = CONFIG_HTTP_PREFIX + "cross_origin.";
    private static final String CONFIG_XORIGIN_ENABLED = CONFIG_XORIGIN_PREFIX + "enabled";
    private static final String CONFIG_XORIGIN_ORIGINS = CONFIG_XORIGIN_PREFIX + "allowed_origins";
    private static final String CONFIG_XORIGIN_METHODS = CONFIG_XORIGIN_PREFIX + "allowed_methods";
    private static final String CONFIG_XORIGIN_HEADERS = CONFIG_XORIGIN_PREFIX + "allowed_headers";
    private static final String CONFIG_XORIGIN_MAX_AGE = CONFIG_XORIGIN_PREFIX + "preflight_max_age";
    private static final String CONFIG_XORIGIN_CREDENTIALS = CONFIG_XORIGIN_PREFIX + "allow_credentials";

    private static final String REST_ROLE = "rest-user";

    private final ConfigurationService configurationService;
    private final SecurityService securityService;

    private final Object lock = new Object();
    private SimpleHandlerList handlerList;
    private Server server;
    private Set<String> registeredPaths;
    private boolean xOriginFilterEnabled;
    private volatile int port = -1;

    // Need reference to prevent GC and setting loss
    private final java.util.logging.Logger jerseyLogging;

    @Inject
    public HttpConductorImpl(ConfigurationService configurationService,
                             SecurityService securityService) {
        this.configurationService = configurationService;
        this.securityService = securityService;

        jerseyLogging = java.util.logging.Logger.getLogger("com.sun.jersey");
        jerseyLogging.setLevel(java.util.logging.Level.OFF);
    }

    @Override
    public void registerHandler(ContextHandler handler) {
        String contextBase = getContextPathPrefix(handler.getContextPath());
        synchronized (lock) {
            if (registeredPaths == null)
                registeredPaths = new HashSet<>();
            if (!registeredPaths.add(contextBase))
                throw new IllegalPathRequest("context already reserved: " + contextBase);
            try {
                if(xOriginFilterEnabled) {
                    addCrossOriginFilter(handler);
                }
                handlerList.addHandler(handler);
                if (!handler.isStarted()) {
                        handler.start();
                }
            } catch (Exception e) {
                throw new HttpConductorException(e);
            }
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void unregisterHandler(ContextHandler handler) {
        String contextBase = getContextPathPrefix(handler.getContextPath());
        synchronized (lock) {
            if (registeredPaths == null || (!registeredPaths.remove(contextBase))) {
                logger.warn("Path not registered (for " + handler + "): " + contextBase);
            }
            else {
                handlerList.removeHandler(handler);
                if (!handler.isStopped()) {
                    try {
                        handler.stop();
                    }
                    catch (Exception e) {
                        throw new HttpConductorException(e);
                    }
                }
            }
        }
    }

    static enum AuthenticationType {
        NONE, BASIC, DIGEST
    }

    @Override
    public void start() {
        String portProperty = configurationService.getProperty(CONFIG_PORT_PROPERTY);
        String sslProperty = configurationService.getProperty(CONFIG_SSL_PROPERTY);
        String loginProperty = configurationService.getProperty(CONFIG_LOGIN_PROPERTY);
        int portLocal;
        boolean ssl;
        AuthenticationType login;
        try {
            portLocal = Integer.parseInt(portProperty);
        }
        catch (NumberFormatException e) {
            logger.error("bad port descriptor: " + portProperty);
            throw e;
        }
        ssl = Boolean.parseBoolean(sslProperty);
        if ("none".equals(loginProperty)) {
            login = AuthenticationType.NONE;
        }
        else if ("basic".equals(loginProperty)) {
            login = AuthenticationType.BASIC;
        }
        else if ("digest".equals(loginProperty)) {
            login = AuthenticationType.DIGEST;
        }
        else {
            throw new IllegalArgumentException("Invalid " + CONFIG_LOGIN_PROPERTY +
                                               " property: " + loginProperty);
        }
        xOriginFilterEnabled = Boolean.parseBoolean(configurationService.getProperty(CONFIG_XORIGIN_ENABLED));
        logger.info("Starting {} service on port {} with authentication {} and CORS {}",
                    new Object[] { ssl ? "HTTPS" : "HTTP", portProperty, login, xOriginFilterEnabled ? "on" : "off"});
                    
        Server localServer = new Server();
        SelectChannelConnector connector;
        if (!ssl) {
            connector = new SelectChannelConnector();
        }
        else {
            // Share keystore configuration with PSQL.
            SslContextFactory sslFactory = new SslContextFactory();
            sslFactory.setKeyStorePath(System.getProperty("javax.net.ssl.keyStore"));
            sslFactory.setKeyStorePassword(System.getProperty("javax.net.ssl.keyStorePassword"));
            connector = new SslSelectChannelConnector(sslFactory);
        }
        connector.setPort(portLocal);
        connector.setThreadPool(new QueuedThreadPool(200));
        connector.setAcceptors(4);
        connector.setMaxIdleTime(300000);
        connector.setAcceptQueueSize(12000);
        connector.setLowResourcesConnections(25000);

        localServer.setConnectors(new Connector[]{connector});

        SimpleHandlerList localHandlerList = new SimpleHandlerList();

        try {
            if (login == AuthenticationType.NONE) {
                localServer.setHandler(localHandlerList);
            }
            else {
                String resource;
                Authenticator authenticator;
                switch (login) {
                case BASIC:
                    resource = "basic.properties";
                    authenticator = new BasicAuthenticator();
                    break;
                case DIGEST:
                    resource = "digest.properties";
                    authenticator = new DigestAuthenticator();
                    break;
                default:
                    assert false : "Unexpected authentication type " + login;
                    resource = null;
                    authenticator = null;
                }
                Constraint constraint = new Constraint(authenticator.getAuthMethod(),
                                                       REST_ROLE);
                constraint.setAuthenticate(true);

                ConstraintMapping cm = new ConstraintMapping();
                cm.setPathSpec("/*");
                cm.setConstraint(constraint);

                ConstraintSecurityHandler sh = new ConstraintSecurityHandler();
                sh.setAuthenticator(authenticator);
                sh.setConstraintMappings(Collections.singletonList(cm));

                LoginService loginService =
                    new SingleThreadJDBCLoginService(SecurityService.REALM, 
                                                     HttpConductorImpl.class.getResource(resource).toString());
                sh.setLoginService(loginService);

                sh.setHandler(localHandlerList);
                localServer.setHandler(sh);
            }
            localHandlerList.addDefaultHandler(new NoResourceHandler());
            localServer.start();
        }
        catch (Exception e) {
            logger.error("failed to start HTTP server", e);
            throw new HttpConductorException(e);
        }

        synchronized (lock) {
            this.server = localServer;
            this.handlerList = localHandlerList;
            this.registeredPaths = null;
            this.port = portLocal;
        }
    }

    @Override
    public void stop() {
        Server localServer;
        synchronized (lock) {
            xOriginFilterEnabled = false;
            localServer = server;
            server = null;
            handlerList = null;
            registeredPaths = null;
            port = -1;
        }
        try {
            localServer.stop();
        }
        catch (Exception e) {
            logger.error("failed to stop HTTP server", e);
            throw new HttpConductorException(e);
        }
    }

    @Override
    public void crash() {
        stop();
    }

    private void addCrossOriginFilter(ContextHandler handler) throws ServletException {
        FilterRegistration reg = handler.getServletContext().addFilter("CrossOriginFilter", CrossOriginFilter.class);
        reg.addMappingForServletNames(null /*default = REQUEST*/, false, "*");
        reg.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM,
                             configurationService.getProperty(CONFIG_XORIGIN_ORIGINS));
        reg.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM,
                             configurationService.getProperty(CONFIG_XORIGIN_METHODS));
        reg.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
                             configurationService.getProperty(CONFIG_XORIGIN_HEADERS));
        reg.setInitParameter(CrossOriginFilter.PREFLIGHT_MAX_AGE_PARAM,
                             configurationService.getProperty(CONFIG_XORIGIN_MAX_AGE));
        reg.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM,
                             configurationService.getProperty(CONFIG_XORIGIN_CREDENTIALS));
    }

    static String getContextPathPrefix(String contextPath) {
        if (!contextPath.startsWith("/"))
            throw new IllegalPathRequest("registered paths must start with '/'");
        int contextBaseEnd = contextPath.indexOf("/", 1);
        if (contextBaseEnd < 0)
            contextBaseEnd = contextPath.length();
        String result = contextPath.substring(1, contextBaseEnd);
        if (result.contains("*"))
            throw new IllegalPathRequest("can't ask for a glob within the first URL segment");
        return result;
    }

    // Embedded JDBC is single-threaded, but login service assumes it
    // is thread-safe.  Also, it does not close its ResultSet, which
    // leaves a transaction active. So just close connection around
    // each use.  Login service is already
    // synchronized. Unfortunately, this needs a private method.
    static class SingleThreadJDBCLoginService extends JDBCLoginService {
        private Method closeMethod;

        public SingleThreadJDBCLoginService(String name, String config)
                throws IOException {
            super(name, config);
            try {
                closeMethod = JDBCLoginService.class.getDeclaredMethod("closeConnection", null);
                closeMethod.setAccessible(true);
            }
            catch (Exception ex) {
                throw new AkibanInternalException("Cannot get JDBC close method", ex);
            }
        }

        @Override
        protected org.eclipse.jetty.server.UserIdentity loadUser(String username) {
            try {
                return super.loadUser(username);
            }
            finally {
                try {
                    closeMethod.invoke(this);
                }
                catch (Exception ex) {
                    logger.warn("Cannot call JDBC close method", ex);
                }
            }
        }
    }
}
