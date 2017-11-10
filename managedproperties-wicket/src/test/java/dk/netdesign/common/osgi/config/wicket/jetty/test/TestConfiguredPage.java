package dk.netdesign.common.osgi.config.wicket.jetty.test;


import dk.netdesign.common.osgi.config.exception.*;
import dk.netdesign.common.osgi.config.wicket.jetty.EmbeddableJettyWebTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


public class TestConfiguredPage {


//    @Rule
//    public OsgiContext context = new OsgiContext();

    private EmbeddableJettyWebTest embeddableJettyWebTest;

    @Before
    public void before() throws Exception {
        embeddableJettyWebTest =new EmbeddableJettyWebTest();
        embeddableJettyWebTest.start();
    }

    @After
    public void after() throws Exception {
        embeddableJettyWebTest.stop();

    }

    @Test
    public void test() throws IOException, InvocationException, TypeFilterException, DoubleIDException, InvalidTypeException, InvalidMethodException, ControllerPersistenceException, ParsingException, InterruptedException {
/*
        context.registerService(ManagedPropertiesServiceFactory.class);
        SetterConfig setterConfig = ManagedPropertiesServiceFactory.registerProperties(SetterConfig.class, context.bundleContext());
*/
        System.in.read();


    }


}
