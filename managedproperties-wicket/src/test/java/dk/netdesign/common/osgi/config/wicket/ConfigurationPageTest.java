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

import org.apache.wicket.util.tester.WicketTester;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mnn
 */
public class ConfigurationPageTest {
    WicketTester tester;
    
    
    public ConfigurationPageTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        
        tester = new WicketTester(InjectingConfigurationPage.class);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setUpPage method, of class ConfigurationPage.
     */
    @Test
    public void testSetUpPage() {
        tester.startPage(InjectingConfigurationPage.class);
        tester.assertRenderedPage(InjectingConfigurationPage.class);
    }

    
}
