/*
 * Copyright 2015 TDC Netdesign.
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

import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.PropertiesProvider;
import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationCallbackHandler;
import dk.netdesign.common.osgi.config.enhancement.PropertyActions;
import dk.netdesign.common.osgi.config.enhancement.PropertyConfig;
import dk.netdesign.common.osgi.config.exception.ControllerPersistenceException;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.osgi.ConfigurationAdminProvider;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the factory for the ManagedProperties service.
 * @author mnn
 */
public class ManagedPropertiesFactory {
    private static final Logger logger = LoggerFactory.getLogger(ManagedPropertiesFactory.class);
    private HandlerFactory handlerFactory;
    private ControllerPersistenceProvider persistenceProvider;
    private DefaultFilterProvider filterProvider;

    public ManagedPropertiesFactory(HandlerFactory handlerFactory, ControllerPersistenceProvider persistenceProvider, DefaultFilterProvider filterProvider) {
	this.handlerFactory = handlerFactory;
	this.persistenceProvider = persistenceProvider;
	this.filterProvider = filterProvider;
    }
    
    
    
    
    /**
     * Registers a configuration that is based on the Interface referenced by {@code type}.
     * Equivalent to {@link #register(Class, Object, BundleContext)}, with a null defaults object.
     * @param <T> The return type of the configuration.
     * @param type The type of configuration to create. The type of interface must be be annotated by 
     * {@link dk.netdesign.common.osgi.config.annotation.Property}, and each parameter must be annotated by 
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition}
     * @return A proxy representing a Configuration Admin configuration.
     * @throws InvalidTypeException If a method/configuration item mapping uses an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any restriction not defined in the other exceptions.
     */
    public synchronized <T extends Object> T register(Class<T> type) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	return register(type, null);
    }
    
    /**
     * Registers a configuration backed by the Configuration Admin. When this method is called, a {@link Proxy} object is created based on the {@code type} 
     * provided to the method. The configuration is automatically registered as a {@link org.osgi.service.cm.ManagedService} and {@link org.osgi.service.metatype.MetaTypeProvider}.
     * The configuration can also be created with a Defaults object. In order to create a Defaults object, implement the Interface provided by {@code type} 
     * and supplying an instance of that class to {@code defaults}. {@code defaults} may be null.
     * 
     * * Example:
     * SomeInterface properties = ManagedPropertiesFactory.register(SomeInterface.class, new SomeInterfaceImpl(), context);
     * 
     * @param <I> The return type ofs the configuration.
     * @param <T> The return type of the default.
     * @param type The type of configuration to create. The type of interface must be be annotated by 
     * {@link dk.netdesign.common.osgi.config.annotation.Property}, and each parameter must be annotated by 
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition}
     * @param defaults The defaults object to create. When a configuration item is not found in the Configuration Admin, the defaults method is called.
     * @return A proxy representing a Configuration Admin configuration.
     * @throws InvalidTypeException If a method/configuration item mapping uses an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any restriction not defined in the other exceptions.
     */
    public synchronized <I, T extends I> I register(Class<I> type, T defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException {
	ManagedPropertiesController controller = null;
	if (!type.isInterface()) {
	    throw new InvalidTypeException("Could  not register the type " + type.getName() + " as a Managed Property. The type must be an interface");
	}
	  
	if(persistenceProvider != null){
	    controller = persistenceProvider.getController(type);
	}
	
	
	  
	  if(controller == null){
	        ArrayList<Class<? extends TypeFilter>> filters = new ArrayList<>();
		if(filterProvider != null){
		    filters.addAll(filterProvider.getFilters());
		}
	        controller = getInvocationHandler(type, defaults, filters);
	        ManagedPropertiesProvider provider = handlerFactory.getProvider(type, controller, defaults);
		validateInputTypesWithProvider(provider, controller.getAttributes(), filters);
	    try {
		provider.start();
	    } catch (Exception ex) {
		throw new InvocationException("Could not start provider: "+provider, ex);
	    }
	    
		controller.setProvider(provider);
		if(persistenceProvider != null){
		    persistenceProvider.persistController(type, controller);
		}
		logger.info("Registered "+controller);
	  }
	  
	return type.cast(Proxy.newProxyInstance(ManagedPropertiesFactory.class.getClassLoader(), new Class[]{type, PropertyActions.class, PropertyConfig.class, ConfigurationCallbackHandler.class}, controller));
    }
    
    /**
     * Builds the ManagedProperties object for use as an invocation handler in the {@link Proxy proxy}.
     * @param <E> The return type of the invocation handler.
     * @param type The interface used to create the ManagedProperties object. This Interface must at least be annotated with the 
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition PropertyDefinition} and have one method annotated with the 
     * {@link dk.netdesign.common.osgi.config.annotation.Property Property}. The interface will be parsed in order to build the configuration metadata.
     * @param defaults The defaults to use for the ManagedProperties object. Can be null. The defaults must implement the same interface as used in
     * {@code type}.
     * @return The finished ManagedProperties object
     * @throws InvalidTypeException If a method/configuration item mapping uses an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any restriction not defined in the other exceptions.
     */
    protected static <E> ManagedPropertiesController getInvocationHandler(Class<? super E> type, E defaults, List<Class<? extends TypeFilter>> defaultFilters) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
	return new ManagedPropertiesController(type, defaults, defaultFilters);
    }
    
    protected void validateInputTypesWithProvider(ManagedPropertiesProvider provider, List<Attribute> attributes, List<Class<? extends TypeFilter>> defaultFilters) throws UnknownValueException, TypeFilterException, InvalidTypeException{
	for(Attribute attribute : attributes){
	    Class providerReturnType = provider.getReturnType(attribute.getID());
	    Class attributeInputType = attribute.getInputType();
	    if(!attributeInputType.isAssignableFrom(providerReturnType)){
		if(logger.isInfoEnabled()){
		    logger.info("Provider "+provider.getClass().getName()+" returns "+providerReturnType+" for config "+attribute.getName()+"["+attribute.getID()+
			"] but attribute requires "+attributeInputType+" searching for default filters");
		}
		boolean matchingFilterFound = false;
		for(Class<? extends TypeFilter> filterType : defaultFilters){
		    Method method = getParseMethod(filterType);
		    Class filterInput = method.getParameterTypes()[0];
		    Class filterOutput = method.getReturnType();
		    if(providerReturnType == filterInput && attributeInputType == filterOutput){
			if(logger.isDebugEnabled()){
			    logger.debug("Found filter for "+providerReturnType.getName()+"->"+attributeInputType.getName()+": "+filterType.getName());
			}
			matchingFilterFound = true;
			break;
		    }
		}
		if(!matchingFilterFound){
		    throw new InvalidTypeException("Could not create a controller for this configuration. The attribute "+attribute.getName()+" requires an "
			    + "input type of "+attributeInputType+" but the provider returns "+providerReturnType+" and no default filter was found to parse");
		}
	    }
	}
    }
    
    public static Method getParseMethod(Class<? extends TypeFilter> filterType) throws TypeFilterException{
	    try {
		for(Method method : filterType.getDeclaredMethods()){
		    if(method.getName().equals("parse") && method.getParameterCount() == 1){
			return method;
		    }
		}
		
	    } catch (SecurityException ex) {
		throw new TypeFilterException("Could not add filter. An exception occured while examining the parse method", ex);
	    }
	    throw new TypeFilterException("The filter did not contain a valid parse method");//This should be pretty much impossible, but never say never.
    }
    
}
