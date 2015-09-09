/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.service;

import dk.netdesign.common.osgi.config.ManagedProperties;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesService;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationCallbackHandler;
import dk.netdesign.common.osgi.config.enhancement.EnhancedProperty;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public class ManagedPropertiesBroker implements ManagedPropertiesService {

    private static final Logger logger = LoggerFactory.getLogger(ManagedPropertiesBroker.class);
    private BundleContext context;
    private Map<String, ManagedPropertiesRegistration> propertyInstances = new HashMap<>();

    public ManagedPropertiesBroker(BundleContext context) {
	this.context = context;
    }

    @Override
    public synchronized <T extends Object> T register(Class<T> type) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
	return register(type, null);
    }
    
    @Override
    public <I, T extends I> I register(Class<I> type, T defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
	ManagedProperties handler;
	if (!type.isInterface()) {
	    throw new InvalidTypeException("Could  not register the type " + type.getName() + " as a Managed Property. The type must be an interface");
	}
	PropertyDefinition propertyDefinition = getDefinitionAnnotation(type);

	if (propertyInstances.containsKey(propertyDefinition.id())) {
	    ManagedPropertiesRegistration currentRegistration = propertyInstances.get(propertyDefinition.id());
	    if (!currentRegistration.registeredInterface.isAssignableFrom(type)) {
		throw new DoubleIDException("Could not register the interface" + type + ". This id is already in use by " + currentRegistration.registeredInterface);
	    }
	    handler = currentRegistration.properties;
	} else {
	    handler = getInvocationHandler(type, defaults);
	    handler.register(context);
	    propertyInstances.put(propertyDefinition.id(), new ManagedPropertiesRegistration(type, handler));
	    logger.info("Registered "+handler);
	}
	
	return type.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{type, EnhancedProperty.class, ConfigurationCallbackHandler.class}, handler));
    }
    
    

    protected <E> ManagedProperties getInvocationHandler(Class<? super E> type, E defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
	return new ManagedProperties(type, defaults);
    }

    

    public static PropertyDefinition getDefinitionAnnotation(Class<?> type) throws InvalidTypeException {
	if (type == null) {
	    throw new InvalidTypeException("Could not build OCD. Type was null");
	}
	if (!type.isAnnotationPresent(PropertyDefinition.class)) {
	    throw new InvalidTypeException("Could not build OCD for " + type.getName() + ". Type did not contain the annotation " + PropertyDefinition.class.getName());
	}

	PropertyDefinition typeDefinition = type.getAnnotation(PropertyDefinition.class);

	if (typeDefinition.id().isEmpty()) {
	    throw new InvalidTypeException("Could not build OCD for " + type.getName() + ". ID was not set on " + PropertyDefinition.class.getSimpleName());
	}

	if (typeDefinition.name().isEmpty()) {
	    throw new InvalidTypeException("Could not build OCD for " + type.getName() + ". Name was not set on " + PropertyDefinition.class.getSimpleName());
	}

	return typeDefinition;
    }

    private class ManagedPropertiesRegistration {

	Class registeredInterface;
	ManagedProperties properties;

	public ManagedPropertiesRegistration(Class registeredInterface, ManagedProperties properties) {
	    this.registeredInterface = registeredInterface;
	    this.properties = properties;
	}

	public ManagedPropertiesRegistration() {
	}

	public Class getRegisteredInterface() {
	    return registeredInterface;
	}

	public void setRegisteredInterface(Class registeredInterface) {
	    this.registeredInterface = registeredInterface;
	}

	public ManagedProperties getProperties() {
	    return properties;
	}

	public void setProperties(ManagedProperties properties) {
	    this.properties = properties;
	}

    }

}
