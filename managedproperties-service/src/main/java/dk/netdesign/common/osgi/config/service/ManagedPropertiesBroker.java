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
 * This is the broker component for the ManagedProperties service.
 * @author mnn
 */
public class ManagedPropertiesBroker implements ManagedPropertiesService {

    private static final Logger logger = LoggerFactory.getLogger(ManagedPropertiesBroker.class);
    private BundleContext context;
    private Map<String, ManagedPropertiesRegistration> propertyInstances = new HashMap<>();

    /**
     * This is the only constructor for the broker.
     * @param context The context that the broker will use to register configurations
     */
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
