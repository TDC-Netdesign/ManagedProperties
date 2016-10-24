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

import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.service.HandlerFactory;
import dk.netdesign.common.osgi.config.osgi.ManagedPropertiesDefaultFiltersComponent;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesProvider;
import dk.netdesign.common.osgi.config.osgi.ManagedPropertiesServiceFactory;
import dk.netdesign.common.osgi.config.service.TypeFilter;
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
    ManagedPropertiesFactory factory;
    ManagedPropertiesFactory factoryWithFilters;
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
	HandlerFactory handlerfactory = new HandlerFactory() {

	    @Override
	    public <E> ManagedPropertiesProvider getProvider(Class<? super E> configurationType, ManagedPropertiesController controller, E defaults) throws InvocationException, InvalidTypeException, InvalidMethodException, DoubleIDException {
		return new ManagedPropertiesProvider(controller) {
		    
		    @Override
		    public Class getReturnType(String configID) throws UnknownValueException {
			return String.class;
		    }
		    
		    @Override
		    public void start() throws Exception {
			
		    }
		    
		    @Override
		    public void stop() throws Exception {
			
		    }
		};
	    }
	};
	
	factory = new ManagedPropertiesFactory(handlerfactory, null, null);
	factoryWithFilters = new ManagedPropertiesFactory(handlerfactory, null, new ManagedPropertiesDefaultFiltersComponent());
	
	
    }
    
    @After
    public void tearDown() {
	
    }

    /**
     * Test of name method, of class Property.
     */
    @Test
    public void testInheritance() throws Exception{
	SuperConfiguration superConfig = factory.register(SuperConfiguration.class);
	SubConfiguration subConfig = factory.register(SubConfiguration.class);
	
    }
    
    @Test(expected = InvalidTypeException.class)
    public void testBadInheritance() throws Exception{
	SuperConfiguration config = factory.register(FaultyConfiguration.class);

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
