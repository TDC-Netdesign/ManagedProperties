/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationCallbackHandler;
import dk.netdesign.common.osgi.config.enhancement.EnhancedProperty;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public class ManagedPropertiesService implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(ManagedPropertiesService.class);
    private BundleContext context;
    private Map<String, ManagedPropertiesRegistration> propertyInstances = new HashMap<>();
    
    

    public synchronized <T extends Object> T register(Class<T> type) throws InvalidTypeException, TypeFilterException, DoubleIDException {
	ManagedProperties handler;
	if (!type.isInterface()) {
	    throw new InvalidTypeException("Could  not register the type " + type.getName() + " as a Managed Property. The type must be an interface");
	}
	PropertyDefinition propertyDefinition = getDefinitionAnnotation(type);
	
	if(propertyInstances.containsKey(propertyDefinition.id())){
	    ManagedPropertiesRegistration currentRegistration = propertyInstances.get(propertyDefinition.id());
	    if(!currentRegistration.registeredInterface.isAssignableFrom(type)){
		throw new DoubleIDException("Could not register the interface"+type+". This id is already in use by "+currentRegistration.registeredInterface);
	    }
	    handler = currentRegistration.properties;
	}else{
	    handler = getInvocationHandler(type);
	    handler.register(context);
	    propertyInstances.put(propertyDefinition.id(), new ManagedPropertiesRegistration(type, handler));
	}
	
	
	return type.cast(Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{type, EnhancedProperty.class, ConfigurationCallbackHandler.class}, handler));
    }

    protected ManagedProperties getInvocationHandler(Class<?> type) throws InvalidTypeException, TypeFilterException, DoubleIDException {
	return new ManagedProperties(type);
    }

    @Override
    public void start(BundleContext context) throws Exception {
	this.context = context;
	for(ManagedPropertiesRegistration registration : propertyInstances.values()){
	    registration.properties.register(context);
	}
    }

    @Override
    public void stop(BundleContext context) throws Exception {
	this.context = null;
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
    
    
    
    private class ManagedPropertiesRegistration{
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
