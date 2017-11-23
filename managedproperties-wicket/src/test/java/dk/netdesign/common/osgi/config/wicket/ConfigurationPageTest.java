/*
 * Copyright 2017 mnn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.netdesign.common.osgi.config.wicket;

import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.service.HandlerFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesProvider;
import dk.netdesign.common.osgi.config.service.PropertyAccess;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.application.IComponentInstantiationListener;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.proxy.IProxyTargetLocator;
import org.apache.wicket.proxy.LazyInitProxyFactory;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.Mock;

import static org.easymock.EasyMock.*;

import org.easymock.EasyMockRunner;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.runner.RunWith;

/**
 * @author mnn
 */
@RunWith(EasyMockRunner.class)
public class ConfigurationPageTest {
    @Mock
    private ManagedPropertiesProvider provider;

    private TestConfigurationItemFactory configFactory;

    private ManagedPropertiesFactory factory;


    WicketTester tester;

    File testFile;


    public ConfigurationPageTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
        testFile = new File("testFile.test");
        testFile.createNewFile();

        configFactory = new TestConfigurationItemFactory();


        HandlerFactory handlerfactory = new HandlerFactory() {

            @Override
            public <E> ManagedPropertiesProvider getProvider(Class<? super E> configurationType, final ManagedPropertiesController controller, E defaults) throws InvocationException, InvalidTypeException, InvalidMethodException, DoubleIDException {
                System.out.println("Adding " + configurationType + "->" + controller);
                configFactory.addConfigItem(configurationType, ManagedPropertiesFactory.castToProxy(configurationType, controller));
                return provider;
            }
        };

        factory = new ManagedPropertiesFactory(handlerfactory, null, null);


        tester = new WicketTester(new WebApplication() {
            @Override
            protected void init() {
                super.init(); //To change body of generated methods, choose Tools | Templates.


                getComponentInstantiationListeners().add(new IComponentInstantiationListener() {
                    @Override
                    public void onInstantiation(Component component) {

                        TestConfigurationItemFactory testConfigurationItemFactory = (TestConfigurationItemFactory) LazyInitProxyFactory.createProxy(TestConfigurationItemFactory.class,
                                new IProxyTargetLocator() {
                                    @Override
                                    public Object locateProxyTarget() {
                                        return configFactory;
                                    }
                                });

                        if (component instanceof InjectingConfigurationPage) {
                            InjectingConfigurationPage icp = (InjectingConfigurationPage) component;
                            System.out.println("Injecting " + configFactory + "\ninto\n" + component);
                            icp.setFactory(testConfigurationItemFactory);
                        } else if (component instanceof ConfiguredPage) {
                            ConfiguredPage cp = (ConfiguredPage) component;
                            System.out.println("Injecting " + configFactory + "\ninto\n" + component);
                            cp.setFactory(testConfigurationItemFactory);
                        }


                    }
                });
            }

            @Override
            public Class<? extends Page> getHomePage() {
                return InjectingConfigurationPage.class;
            }

        });


    }

    @After
    public void tearDown() throws Exception {
        verify(provider);
        testFile.delete();

    }

    @Test
    public void testRedirectBehavior() throws Exception {

        expect(provider.getReturnType("String")).andReturn(String.class).atLeastOnce();
        expect(provider.getReturnType("File")).andReturn(String.class).atLeastOnce();

        /*Expect*/
        provider.start();
        expectLastCall().once();
        /*Expect*/
        provider.stop();
        expectLastCall().once();

        replay(provider);

        SetterConfig config = factory.register(SetterConfig.class);

        tester.startPage(ConfiguredPage.class);
        tester.assertRenderedPage(InjectingConfigurationPage.class);


        PropertyAccess.actions(config).unregisterProperties();

    }

    @Test
    public void testFulfilledRedirectBehavior() throws Exception {

        String setString = "someString";

        Map<String, Object> expectedSetConfig = new HashMap<>();
        expectedSetConfig.put("String", setString);


        expect(provider.getReturnType("String")).andReturn(String.class).atLeastOnce();
        expect(provider.getReturnType("File")).andReturn(String.class).atLeastOnce();

        /*Expect*/
        provider.start();
        expectLastCall().once();
        /*Expect*/
        provider.persistConfiguration(expectedSetConfig);
        expectLastCall().once();
        /*Expect*/
        provider.stop();
        expectLastCall().once();

        replay(provider);

        SetterConfig config = factory.register(SetterConfig.class);

        config.setString(setString);
        PropertyAccess.actions(config).commitProperties();

        tester.startPage(ConfiguredPage.class);
        tester.assertRenderedPage(ConfiguredPage.class);


        PropertyAccess.actions(config).unregisterProperties();

    }

    /**
     * Test of setUpPage method, of class ConfigurationPage.
     */
    @Test
    public void testConfigPageSubmit() throws Exception {


        String setString = "newString";

        Map<String, Object> expectedSetConfig = new HashMap<>();
        expectedSetConfig.put("String", setString);
        expectedSetConfig.put("File", testFile.getCanonicalPath());
        
        /*Expect*/
        provider.start();
        expectLastCall().once();
        /*Expect*/
        provider.persistConfiguration(expectedSetConfig);
        expectLastCall().once();
        /*Expect*/
        provider.stop();
        expectLastCall().once();

        expect(provider.getReturnType("String")).andReturn(String.class).atLeastOnce();
        expect(provider.getReturnType("File")).andReturn(String.class).atLeastOnce();

        replay(provider);

        SetterConfig config = factory.register(SetterConfig.class);


        try {
            config.getString();
            fail("Exception exceptions when getting String configuration");
        } catch (UnknownValueException ex) {
        }
        try {
            config.getFile();
            fail("Exception exceptions when getting File configuration");
        } catch (UnknownValueException ex) {
        }

        PageParameters params = new PageParameters();
        params.add(ConfigurationPage.CONFIGID, ManagedPropertiesController.getDefinitionID(SetterConfig.class));
        tester.startPage(InjectingConfigurationPage.class, params);
        tester.assertRenderedPage(InjectingConfigurationPage.class);

        FormTester formTester = tester.newFormTester("configForm", false);


        formTester.setValue("attribute-panels:0:attribute-panel:inputArea:input", testFile.getCanonicalPath());
        formTester.setValue("attribute-panels:1:attribute-panel:inputArea:input", setString);

        formTester.submit();
        PropertyAccess.actions(config).unregisterProperties();

        assertEquals(setString, config.getString());
        assertEquals(testFile.getCanonicalFile(), config.getFile());

    }


}
