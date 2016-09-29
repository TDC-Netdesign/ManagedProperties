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

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import org.ops4j.pax.exam.options.MavenUrlReference;

/**
 *
 * @author Martin
 */
public class PropertiesTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesTest.class);
    
        @Configuration
	public Option[] config() {
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
            .unpackDirectory(new File("target/exam"))
            .useDeployFolder(false),
        //keepRuntimeFolder(),
        features(karafStandardRepo, "webconsole"),
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-service").versionAsInProject(),
	
//	mavenBundle("org.apache.commons", "commons-lang3").versionAsInProject().startLevel(91),
//	mavenBundle("dk.netdesign", "managedproperties-service").versionAsInProject().startLevel(92),
//	mavenBundle("org.bouncycastle","bcprov-jdk15on").versionAsInProject().startLevel(91),
//	mavenBundle("org.bouncycastle","bcpkix-jdk15on").versionAsInProject().startLevel(91),
//	mavenBundle("commons-codec", "commons-codec").versionAsInProject().startLevel(91),
//	mavenBundle("org.apache.shiro", "shiro-core").versionAsInProject().startLevel(91),
//	mavenBundle("com.polis.licensing", "subject-core").versionAsInProject().startLevel(93),
//	mavenBundle("com.polis.licensing", "subject-storage").versionAsInProject().startLevel(94),
//	mavenBundle("com.polis.licensing", "subject-userstore-xml").versionAsInProject().startLevel(94),
//	mavenBundle("com.polis.licensing", "subject-servicestore-xml").versionAsInProject().startLevel(94),
//	mavenBundle("com.polis.licensing", "subject-entitystore-xml").versionAsInProject().startLevel(94),
//	mavenBundle("com.polis.licensing", "subject-customerstore-xml").versionAsInProject().startLevel(94),
//	mavenBundle("com.polis.licensing", "certbuilder").versionAsInProject().startLevel(94),
//	mavenBundle("com.polis.licensing", "Common").versionAsInProject().startLevel(93),
//	mavenBundle("com.polis.licensing", "certificatestore-file").versionAsInProject().startLevel(94),
//	logLevel(LogLevelOption.LogLevel.DEBUG),
	
	
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
    public void testSomeMethod() {
	  // TODO review the generated test code and remove the default call to fail.
	  fail("The test case is a prototype.");
    }
    
}
