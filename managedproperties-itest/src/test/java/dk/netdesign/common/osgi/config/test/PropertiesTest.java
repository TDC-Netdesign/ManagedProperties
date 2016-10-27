/*
 * Copyright 2016 Martin.
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

package dk.netdesign.common.osgi.config.test;

import dk.netdesign.common.osgi.config.osgi.service.ManagedPropertiesService;
import dk.netdesign.common.osgi.config.osgi.ManagedPropertiesServiceFactory;
import dk.netdesign.common.osgi.config.service.PropertyAccess;
import dk.netdesign.common.osgi.config.test.properties.AutoFilteringListTypes;
import dk.netdesign.common.osgi.config.test.properties.FilteringConfig;
import dk.netdesign.common.osgi.config.test.properties.WrapperTypes;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import javax.inject.Inject;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import static org.ops4j.pax.exam.CoreOptions.*;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martin
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PropertiesTest {
    @Inject
    private BundleContext context;
    
    @Inject 
    private ConfigurationAdmin configAdmin;
 
    @Inject
    private ManagedPropertiesService factory;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesTest.class);
    
        @Configuration
	public Option[] config() throws Exception{
    MavenArtifactUrlReference karafUrl = maven()
        .groupId("org.apache.karaf")
        .artifactId("apache-karaf")
        .versionAsInProject()
        .type("tar.gz");
    MavenUrlReference karafStandardRepo = maven()
        .groupId("org.apache.karaf.features")
        .artifactId("standard")
        .classifier("features")
        .type("xml")
        .versionAsInProject();
    MavenUrlReference karafEnterpriseRepo = maven()
        .groupId("org.apache.karaf.features")
        .artifactId("enterprise")
        .classifier("features")
        .type("xml")
        .versionAsInProject();
    
    
    return new Option[] {
        // KarafDistributionOption.debugConfiguration("5005", true),
          karafDistributionConfiguration()
            .frameworkUrl(karafUrl)
            .unpackDirectory(new File("exam"))
            .useDeployFolder(false),
          keepRuntimeFolder(),
          features(karafStandardRepo, "scr"),
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-service").versionAsInProject(),
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-test-resources").versionAsInProject(),
	  mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
	  replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.logging.cfg").toURI())),
	  replaceConfigurationFile("etc/org.ops4j.pax.url.mvn.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.url.mvn.cfg").toURI())),
	  replaceConfigurationFile("etc/WrapperTypes.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/WrapperTypes.cfg").toURI())),
	  replaceConfigurationFile("etc/FilteringConfig.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/FilteringConfig.cfg").toURI())),
	  replaceConfigurationFile("etc/AutoFilteringListTypes.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/AutoFilteringListTypes.cfg").toURI())),
	    	
	
   };
}
    
    public PropertiesTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    
    @Test
    public void testImmediateAccess() throws Exception {
	
	WrapperTypes types = null;
	try{
	    types = factory.register(WrapperTypes.class, context);
	    assertEquals(new Double(55.12), types.getDouble());
	    assertEquals(new Float(22.22), types.getFloat());
	    assertEquals(new Integer(42), types.getInt());
	    assertEquals(true, types.getBoolean());
	    assertEquals(new Byte((byte)1), types.getByte());
	    assertEquals(new Long(100), types.getLong());
	    assertEquals(new Short((short)3), types.getShort());
	}finally{
	    if(types != null){
		PropertyAccess.actions(types).unregisterProperties();
	    }
	}
    }
    
    @Test
    public void testAutomaticFiltering() throws Exception {
	
	FilteringConfig types = null;
	try{
	    types = factory.register(FilteringConfig.class, context);
	    assertEquals(new URL("http://test.dk"), types.getURL());
	    assertEquals(new File("some/file"), types.getFile());

	}finally{
	    if(types != null){
		PropertyAccess.actions(types).unregisterProperties();
	    }
	}
    }
    
    @Test 
    public void testListFiltering() throws Exception {
	AutoFilteringListTypes types = null;
	try{
	    types = factory.register(AutoFilteringListTypes.class, context);
	    String configPid = PropertyAccess.configuration(types).getID();
	    org.osgi.service.cm.Configuration config = configAdmin.getConfiguration(configPid);
	    
	    Dictionary newConfig = new Hashtable();
	    newConfig.put("service.pid", configPid);
	    List<String> files = Arrays.asList(new String[]{"test1", "test2"});
	    newConfig.put("Files", files);
	    List<String> urls = Arrays.asList(new String[]{"http://test1.dk", "http://test2.dk", "http://test3.dk"});
	    newConfig.put("URLs", urls);
	    
	    Thread.sleep(1000);
	    
	    config.update(newConfig);
	    config.update();
	    
	    LOGGER.info("RESULTS [FILE] "+types.getFiles());
	    LOGGER.info("RESULTS [URL] "+types.getURLs());
	    
	    assertEquals(3, types.getURLs().size());
	    assertEquals(2, types.getFiles().size());
	    
	    

	}finally{
	    if(types != null){
		PropertyAccess.actions(types).unregisterProperties();
	    }
	}
    }
    
    @Test
    public void testConfigurationRollback() throws Exception{
	FilteringConfig types = null;
	try{
	    types = factory.register(FilteringConfig.class, context);
	    String configPid = PropertyAccess.configuration(types).getID();
	    org.osgi.service.cm.Configuration config = configAdmin.getConfiguration(configPid);
	    
	    Dictionary newConfig = new Hashtable();
	    newConfig.put("service.pid", configPid);
	    String validfile = "testfile";
	    newConfig.put("File", validfile);
	    String validurl = "http://test1.dk";
	    newConfig.put("URL", validurl);
	    
	    config.update(newConfig);
	    config.update();
	    
	    Thread.sleep(1000);
	    
	    assertEquals(new File(validfile), types.getFile());
	    assertEquals(new URL(validurl), types.getURL());
	    
	    newConfig = new Hashtable();
	    newConfig.put("service.pid", configPid);
	    newConfig.put("File", "badFile");
	    newConfig.put("URL", "badURL");
	    
	    config.update(newConfig);
	    config.update();
	    assertEquals(new File(validfile), types.getFile());
	    assertEquals(new URL(validurl), types.getURL());
	}finally{
	    if(types != null){
		PropertyAccess.actions(types).unregisterProperties();
	    }
	}
    }
    
    @Test
    public void testFactoryMethodRollback() throws Exception {
	FilteringConfig types = null;
	try{
	    types = ManagedPropertiesServiceFactory.registerProperties(FilteringConfig.class, context);
	    String configPid = PropertyAccess.configuration(types).getID();
	    org.osgi.service.cm.Configuration config = configAdmin.getConfiguration(configPid);
	    
	    Dictionary newConfig = new Hashtable();
	    newConfig.put("service.pid", configPid);
	    String validfile = "tesetfile";
	    newConfig.put("File", validfile);
	    String validurl = "http://test1.dk";
	    newConfig.put("URL", validurl);
	    
	    config.update(newConfig);
	    config.update();
	    
	    Thread.sleep(1000);
	    
	    assertEquals(new File(validfile), types.getFile());
	    assertEquals(new URL(validurl), types.getURL());
	    
	    newConfig = new Hashtable();
	    newConfig.put("service.pid", configPid);
	    newConfig.put("File", "badFile");
	    newConfig.put("URL", "badURL");
	    
	    config.update(newConfig);
	    config.update();
	    assertEquals(new File(validfile), types.getFile());
	    assertEquals(new URL(validurl), types.getURL());
	}finally{
	    if(types != null){
		PropertyAccess.actions(types).unregisterProperties();
	    }
	}
    }

	
    
}
