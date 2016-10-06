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

import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.osgi.OSGiHandlerFactory;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.dependency.interceptors.DefaultDependencyInterceptor;
import org.apache.felix.ipojo.dependency.interceptors.ServiceBindingInterceptor;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author mnn
 */
@Provides
@Component(service = ManagedPropertiesService.class)
public class ManagedPropertiesServiceProvider extends DefaultDependencyInterceptor implements ManagedPropertiesService {
    

    public ManagedPropertiesServiceProvider() {
    
    }
    
    
    
    
    @Override
    public <T> T register(Class<T> type, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException {
	return register(type, null, context);
    }

    @Override
    public <I, T extends I> I register(Class<I> type, T defaults, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException {
	OSGiHandlerFactory handlerFactory = new OSGiHandlerFactory(context);
	ManagedPropertiesFactory factory = new ManagedPropertiesFactory(handlerFactory);
	
	return factory.register(type, defaults);
    }

    @Override
    public <T> T register(Class<T> type) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException {
	BundleContext context = BundleReference.class.cast(type.getClassLoader()).getBundle().getBundleContext();
	return register(type, context);
    }

    @Override
    public <I, T extends I> I register(Class<I> type, T defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException {
	BundleContext context = BundleReference.class.cast(type.getClassLoader()).getBundle().getBundleContext();
	return register(type, defaults, context);
    }


     
}
