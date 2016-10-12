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

package dk.netdesign.common.osgi.config.service;

import dk.netdesign.common.osgi.config.exception.ControllerPersistenceException;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.filters.FileFilter;
import dk.netdesign.common.osgi.config.filters.StringToBooleanFilter;
import dk.netdesign.common.osgi.config.filters.StringToByteFilter;
import dk.netdesign.common.osgi.config.filters.StringToDoubleFilter;
import dk.netdesign.common.osgi.config.filters.StringToFloatFilter;
import dk.netdesign.common.osgi.config.filters.StringToIntegerFilter;
import dk.netdesign.common.osgi.config.filters.StringToLongFilter;
import dk.netdesign.common.osgi.config.filters.StringToShortFilter;
import dk.netdesign.common.osgi.config.filters.URLFilter;
import dk.netdesign.common.osgi.config.osgi.OSGiHandlerFactory;
import dk.netdesign.common.osgi.config.osgi.ServiceBasedPersistenceProvider;
import java.util.ArrayList;
import java.util.List;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.dependency.interceptors.DefaultDependencyInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.ServiceBindingInterceptor;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
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
@Provides
@Component(service = ManagedPropertiesService.class)
public class ManagedPropertiesServiceComponent implements ManagedPropertiesService, DefaultFilterProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedPropertiesServiceComponent.class);
    
    private ServiceTracker<DefaultFilterProvider, DefaultFilterProvider> tracker;
    

    public ManagedPropertiesServiceComponent() {
    
    }
    
    @Activate
    public void Activate(BundleContext context) throws Exception{
	tracker = new ServiceTracker<>(context, DefaultFilterProvider.class, null);
    }
    
    @Deactivate
    public void Deactivate(BundleContext context) throws Exception{
	
    }
    
    
    @Override
    public <T> T register(Class<T> type, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	return register(type, null, context);
    }

    @Override
    public <I, T extends I> I register(Class<I> type, T defaults, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	OSGiHandlerFactory handlerFactory = new OSGiHandlerFactory(context);
	ServiceBasedPersistenceProvider persistenceProvider = new ServiceBasedPersistenceProvider(context);
	ManagedPropertiesFactory factory = new ManagedPropertiesFactory(handlerFactory, persistenceProvider, this);
	
	return factory.register(type, defaults);
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
	    try{
		filters.addAll(provider.getFilters());
	    }catch(Exception ex){
		LOGGER.error("Could not add filters from provider: "+provider, ex);
	    }
	    
	}
	return filters;
    }
     
}
