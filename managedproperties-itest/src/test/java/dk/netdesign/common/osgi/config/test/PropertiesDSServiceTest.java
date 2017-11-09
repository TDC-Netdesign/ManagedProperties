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

import dk.netdesign.common.osgi.config.osgi.ManagedPropertiesServiceFactory;
import dk.netdesign.common.osgi.config.osgi.service.ManagedPropertiesService;
import dk.netdesign.common.osgi.config.service.PropertyAccess;
import dk.netdesign.common.osgi.config.test.properties.AutoFilteringListTypes;
import dk.netdesign.common.osgi.config.test.properties.ChangingConfig;
import dk.netdesign.common.osgi.config.test.properties.FilteringConfig;
import dk.netdesign.common.osgi.config.test.properties.WrapperTypes;
import org.junit.*;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

/**
 *
 * @author Martin
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class PropertiesDSServiceTest {
	@Inject
	private BundleContext context;

	@Inject
	private ManagedPropertiesService factory;

	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesDSServiceTest.class);

	@Configuration
	public Option[] config() throws Exception {
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
		MavenUrlReference managedPropertiesRepo = maven()
				.groupId("dk.netdesign")
				.artifactId("managedproperties-feature")
				.classifier("features")
				.type("xml")
				.versionAsInProject();


		return new Option[]{
			//	KarafDistributionOption.debugConfiguration("5005", true),
				karafDistributionConfiguration()
						.frameworkUrl(karafUrl)
						.unpackDirectory(new File("exam"))
						.useDeployFolder(false),
				keepRuntimeFolder(),
				features(karafStandardRepo, "scr"),
				features(managedPropertiesRepo, "ManagedProperties", "ManagedPropertiesTestResources"),
				replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.logging.cfg").toURI())),
				replaceConfigurationFile("etc/org.ops4j.pax.url.mvn.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/org.ops4j.pax.url.mvn.cfg").toURI())),
				replaceConfigurationFile("etc/WrapperTypes.cfg", new File(this.getClass().getClassLoader().getResource("dk/netdesign/common/osgi/config/test/WrapperTypes.cfg").toURI())),
			};
	}

	public PropertiesDSServiceTest() {
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
	public void testCanInjectService() throws Exception {


		WrapperTypes types = null;
		try {

			types = factory.register(WrapperTypes.class, context);
			//PropertyAccess.configuration(types).setPropertyWriteDelay(30, TimeUnit.SECONDS);

			//Should this be working?
			assertEquals(new Double(55.12), types.getDouble());
			assertEquals(new Float(22.22), types.getFloat());
			assertEquals(new Integer(42), types.getInt());
			assertEquals(true, types.getBoolean());
			assertEquals(new Byte((byte) 1), types.getByte());
			assertEquals(new Long(100), types.getLong());
			assertEquals(new Short((short) 3), types.getShort());
		} finally {
			if (types != null) {
				PropertyAccess.actions(types).unregisterProperties();
			}
		}
	}
}
