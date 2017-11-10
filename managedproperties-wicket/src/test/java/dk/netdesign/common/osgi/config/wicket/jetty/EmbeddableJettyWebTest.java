package dk.netdesign.common.osgi.config.wicket.jetty;

import org.apache.wicket.protocol.http.ContextParamWebApplicationFactory;
import org.apache.wicket.protocol.http.WicketFilter;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.management.MBeanServer;
import javax.servlet.DispatcherType;
import java.lang.management.ManagementFactory;
import java.util.EnumSet;

/**
 * Separate startup class for people that want to run the examples directly. Use
 * parameter -Dcom.sun.management.jmxremote to startup JMX (and e.g. connect
 * with jconsole).
 */
public class EmbeddableJettyWebTest {

    /**
     * Main function, starts the jetty server.
     *
     * @param args
     */
    public final static int PORT = 8080;

    private Server server = new Server();

    public EmbeddableJettyWebTest() {
        System.setProperty("wicket.configuration", "development");

        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
        http.setPort(PORT);
        http.setIdleTimeout(1000 * 60 * 60);

        server.addConnector(http);

        ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);
        FilterHolder fh2 = new FilterHolder(WicketFilter.class);
        fh2.setInitParameter(ContextParamWebApplicationFactory.APP_CLASS_PARAM, WicketTestApplication.class.getName());
        fh2.setInitParameter(WicketFilter.FILTER_MAPPING_PARAM, "/*");
        sch.addFilter(fh2, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));

        server.setHandler(sch);

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
        server.addEventListener(mBeanContainer);
        server.addBean(mBeanContainer);

    }

    public static void main(String[] args) throws Exception {
        EmbeddableJettyWebTest embeddableJettyWebTest = new EmbeddableJettyWebTest();

        embeddableJettyWebTest.start();

    }

    public void start() throws Exception {

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(100);
        }

    }

    public void stop() throws Exception {
        try {
            server.stop();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(100);
        }

    }
}
