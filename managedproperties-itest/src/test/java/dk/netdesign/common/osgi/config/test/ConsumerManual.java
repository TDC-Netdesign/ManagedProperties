/*
 * Copyright 2016 mnn.
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
import dk.netdesign.common.osgi.config.service.PropertyAccess;
import dk.netdesign.common.osgi.config.test.properties.WrapperTypes;
import java.io.File;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 *
 * @author mnn
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ConsumerManual {
    
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
        features(karafStandardRepo, "webconsole", "scr"),
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-service").versionAsInProject(),
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-test-resources").versionAsInProject(),	  
	  mavenBundle().groupId("dk.netdesign").artifactId("managedproperties-consumer").versionAsInProject().startLevel(100),
	  mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
	  replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.logging.cfg").toURI())),
	  replaceConfigurationFile("etc/org.ops4j.pax.url.mvn.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.url.mvn.cfg").toURI())),
	    	
	
   };
}
    @Test
    public void testImmediateAccess() throws Exception {
	System.in.read();
    }
}
