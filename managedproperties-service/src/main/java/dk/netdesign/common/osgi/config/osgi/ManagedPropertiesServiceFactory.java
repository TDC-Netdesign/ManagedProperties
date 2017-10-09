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

package dk.netdesign.common.osgi.config.osgi;

import dk.netdesign.common.osgi.config.enhancement.ConfigurationChangeSet;
import dk.netdesign.common.osgi.config.exception.ControllerPersistenceException;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.service.DefaultFilterProvider;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.osgi.service.ManagedPropertiesService;
import dk.netdesign.common.osgi.config.service.PropertyAccess;
import dk.netdesign.common.osgi.config.service.TypeFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
@Component(service = ManagedPropertiesService.class, immediate = true)
public class ManagedPropertiesServiceFactory implements ManagedPropertiesService, DefaultFilterProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedPropertiesServiceFactory.class);
    
    private ServiceTracker<DefaultFilterProvider, DefaultFilterProvider> tracker;
    private ManagedPropertiesConfig config;
    

    public ManagedPropertiesServiceFactory() {
	
    }
    
    @Activate
    public void activate(BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	LOGGER.info("Starting "+getClass().getName());
	tracker = new ServiceTracker<>(context, DefaultFilterProvider.class, null);
	tracker.open();
	config = registerProperties(ManagedPropertiesConfig.class, new ManagedPropertiesConfigDefaults(), context);
    }
    
    @Deactivate
    public void deactivate(BundleContext context) throws Exception{
	try{
	    PropertyAccess.actions(config).unregisterProperties();
	}finally{
	    tracker.close();
	}
    }

    protected ServiceTracker<DefaultFilterProvider, DefaultFilterProvider> getTracker() {
	return tracker;
    }

    protected void setTracker(ServiceTracker<DefaultFilterProvider, DefaultFilterProvider> tracker) {
	this.tracker = tracker;
    }

    protected ManagedPropertiesConfig getConfig() {
	return config;
    }

    protected void setConfig(ManagedPropertiesConfig config) {
	this.config = config;
    }
    
    
    
    public static <T> T registerProperties(Class<T> type, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	return registerProperties(type, null, context);
    }

    public static <I, T extends I> I registerProperties(Class<I> type, T defaults, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	LOGGER.info("Registering new configuration: "+type.getName()+" with defaults "+defaults);
	ServiceTracker<DefaultFilterProvider, DefaultFilterProvider> registerTracker = new ServiceTracker<>(context, DefaultFilterProvider.class, null);
	registerTracker.open();
	ManagedPropertiesServiceFactory osgiPropertiesFactory = new ManagedPropertiesServiceFactory();
	osgiPropertiesFactory.setTracker(registerTracker);
	osgiPropertiesFactory.setConfig(new ManagedPropertiesConfigDefaults());
	try{
	    return osgiPropertiesFactory.register(type, defaults, context);
	}finally{
	    registerTracker.close();
	}
    }
    
    
    @Override
    public <T> T register(Class<T> type, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	return register(type, null, context);
    }

    @Override
    public <I, T extends I> I register(Class<I> type, T defaults, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	LOGGER.info("Registering new configuration: "+type.getName()+" with defaults "+defaults);
	OSGiHandlerFactory handlerFactory = new OSGiHandlerFactory(context);
	ServiceBasedPersistenceProvider persistenceProvider = new ServiceBasedPersistenceProvider(context);
	ManagedPropertiesFactory factory = new ManagedPropertiesFactory(handlerFactory, persistenceProvider, this);
	
	I properties = factory.register(type, defaults);
	PropertyAccess.configuration(properties).setPropertyWriteDelay((int)(config.secondsToWaitForValue()*1000), TimeUnit.MILLISECONDS);
	return properties;
    }

    @Override
    public <T> T register(Class<T> type) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	BundleContext context = BundleReference.class.cast(type.getClassLoader()).getBundle().getBundleContext();
	return register(type, context);
    }

    @Override
    public <I, T extends I> I register(Class<I> type, T defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	BundleContext context = BundleReference.class.cast(type.getClassLoader()).getBundle().getBundleContext();
	return register(type, defaults, context);
    }

    @Override
    public List<Class<? extends TypeFilter>> getFilters() {
	List<Class<? extends TypeFilter>> filters = new ArrayList<>();
	for(DefaultFilterProvider provider : tracker.getServices(new DefaultFilterProvider[tracker.size()])){
	    LOGGER.debug("Getting defaults provider: "+provider);
	    try{
		filters.addAll(provider.getFilters());
	    }catch(Exception ex){
		LOGGER.error("Could not add filters from provider: "+provider, ex);
	    }
	    
	}
	if(LOGGER.isDebugEnabled()){
	    LOGGER.debug("Found filters: "+filters);
	}
	return filters;
    }

    @Override
    public <T> ConfigurationChangeSet<T> getChangeSet(T configuration) throws InvalidTypeException {
        return new ConfigurationChangeSet<>(configuration);
    }
     
}
