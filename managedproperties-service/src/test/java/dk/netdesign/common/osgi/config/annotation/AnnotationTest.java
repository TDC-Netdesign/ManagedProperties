/*
 * Copyright 2015 mnn.
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

package dk.netdesign.common.osgi.config.annotation;

import dk.netdesign.common.osgi.config.MockContext;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.service.TypeFilter;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 *
 * @author mnn
 */
public class AnnotationTest {
    BundleContext context;
    public AnnotationTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
	context = new MockContext();
	
    }
    
    @After
    public void tearDown() {
	context = null;
    }

    /**
     * Test of name method, of class Property.
     */
    @Test
    public void testInheritance() throws Exception{
	SuperConfiguration superConfig = ManagedPropertiesFactory.register(SuperConfiguration.class, context);
	SubConfiguration subConfig = ManagedPropertiesFactory.register(SubConfiguration.class, context);
    }
    
    @Test(expected = InvalidTypeException.class)
    public void testBadInheritance() throws Exception{
	SuperConfiguration config = ManagedPropertiesFactory.register(FaultyConfiguration.class, context);

    }

    @PropertyDefinition
    interface SuperConfiguration{
	
	@Property
	public String getSuperString();
    
    }
    
    interface SubConfiguration extends SuperConfiguration{
	
	@Property
	public String getSubString();
    }
    
    @PropertyDefinition
    interface FaultyConfiguration extends SuperConfiguration{
	
	@Property
	public String getSubString();
    }
    
    
}
