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

import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesServiceComponent;
import dk.netdesign.common.osgi.config.service.PropertyAccess;
import dk.netdesign.common.osgi.config.test.properties.FilteringConfig;
import dk.netdesign.common.osgi.config.test.properties.WrapperTypes;
import dk.netdesign.common.osgi.config.test.properties.WrapperTypesDefaults;
import java.io.File;
import javax.inject.Inject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.osgi.framework.BundleContext;

/**
 *
 * @author Martin
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class DefaultsTest {
    @Inject
    private BundleContext context;
    
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
        //keepRuntimeFolder(),
        features(karafStandardRepo, "webconsole"),
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-service").versionAsInProject(),
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-test-resources").versionAsInProject(),
	  mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
	  replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.logging.cfg").toURI())),
	  replaceConfigurationFile("etc/org.ops4j.pax.url.mvn.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.url.mvn.cfg").toURI())),
	  	  
   };
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
    public void testDefaults() throws Exception {
	ManagedPropertiesServiceComponent factory = new ManagedPropertiesServiceComponent();
	WrapperTypes defaults = new WrapperTypesDefaults();
	
	WrapperTypes types = null;
	try{
	    types = factory.register(WrapperTypes.class, defaults, context);
	    assertEquals(defaults.getBoolean(), types.getBoolean());
	    assertEquals(defaults.getByte(), types.getByte());
	    assertEquals(defaults.getDouble(), types.getDouble());
	    assertEquals(defaults.getFloat(), types.getFloat());
	    assertEquals(defaults.getInt(), types.getInt());
	    assertEquals(defaults.getLong(), types.getLong());
	    assertEquals(defaults.getShort(), types.getShort());
	}finally{
	    if(types != null){
		PropertyAccess.actions(types).unregisterProperties();
	    }
	}
    }
    
    
    
	
    
}
