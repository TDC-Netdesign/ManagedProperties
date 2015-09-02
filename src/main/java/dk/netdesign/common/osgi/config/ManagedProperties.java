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
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.builder.ToStringBuilder;
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
 * The ManagedProperties class is used as a simple setup mechanism for the Configuration Admin and MetaType OSGi services. Using the register method, the
 * ManagedProperties register itself as both a Configuration Admin ManagedService and as a MetaType service MetaTypeProvider. This class can be used directly,
 * but is best used as a superclass. For the best utilization, it is advised to extends this class, and add get methods for each configuration item. Then
 * annotate the get methods with the @Property annotation, and use the get(String, Class) method to cast the object to the expected type. Reflection will take
 * care of creating the MetaTypeProvider items and ensure that the type returned to the service matches the @Property.Updated configuration is done
 * asynchronized with using of Queue for putting and taking all new configuration set.
 *
 * @author mnn
 * @author azem
 */
public class ManagedProperties implements InvocationHandler, MetaTypeProvider, ManagedService, ConfigurationCallbackHandler, EnhancedProperty {

    private static final Logger logger = LoggerFactory.getLogger(ManagedProperties.class);

    private ServiceRegistration managedServiceReg;
    private ServiceRegistration metatypeServiceReg;
    private ObjectClassDefinition ocd;
    private Map<String, Object> config = new HashMap<>();
    private final List<ConfigurationCallback> callbacks;
    private final ReadWriteLock lock;
    private final Lock r, w;
    private final PropertyDefinition typeDefinition;
    private final Map<String, AD> attributeToMethodMapping;
    private final List<Method> allowedMethods;
    private final Class type;
    private final Object defaults;
    private final List<String> requiredIds;
    
    public <E> ManagedProperties(Class<? super E> type, E defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException{
	callbacks = new ArrayList<>();
	lock = new ReentrantReadWriteLock();
	r = lock.readLock();
	w = lock.writeLock();
	attributeToMethodMapping = new HashMap<>();
	typeDefinition = ManagedPropertiesService.getDefinitionAnnotation(type);
	requiredIds = new ArrayList<>();
	for (Method classMethod : type.getMethods()) {
	    if (classMethod.isAnnotationPresent(Property.class)) {
		if(classMethod.getParameterCount() > 0){
		    throw new InvalidMethodException("Could not create handler for this method. Methods annotated with "+Property.class.getName()+
			    " must not take parameters");
		}
		AD methodDefinition = new AD(classMethod);
		if(methodDefinition.getCardinalityDef().equals(Property.Cardinality.Required)){
		    requiredIds.add(methodDefinition.getID());
		}
		attributeToMethodMapping.put(classMethod.getName(), methodDefinition);
	    }
	}

	ensureUniqueIDs(attributeToMethodMapping.values());

	ocd = buildOCD(typeDefinition.id(), typeDefinition.name(), typeDefinition.description(), typeDefinition.iconFile(), attributeToMethodMapping.values());

	allowedMethods = new ArrayList<>();

	allowedMethods.addAll(Arrays.asList(EnhancedProperty.class.getMethods()));
	allowedMethods.addAll(Arrays.asList(ConfigurationCallbackHandler.class.getMethods()));
	this.type = type;
	this.defaults = defaults;
    }

    public ManagedProperties(Class type) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
	this(type, null);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws InvalidTypeException, TypeFilterException, InvocationException, UnknownValueException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	if (method.isAnnotationPresent(Property.class)) {
	    AD propertyDefinition = attributeToMethodMapping.get(method.getName());
	    Object returnValue = getConfigItem(propertyDefinition.getID());
	    if (returnValue == null) {
		returnValue = getDefaultItem(method);
		if(returnValue == null){
		    throw new UnknownValueException("Could not return the value for method " + method.getName() + " the value did not exist in the config set.");
		}
	    }
	    if (!method.getReturnType().isAssignableFrom(returnValue.getClass())) {
		throw new InvalidTypeException("Could not return the value for method " + method.getName() + " the value " + returnValue + " had Type " + returnValue.getClass() + " expected " + method.getReturnType());
	    }
	    return returnValue;
	} else if (allowedMethods.contains(method)) {
	    try {
		return method.invoke(this, args);
	    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
		throw new InvocationException("Could not execute method. Execution of method " + method + " failed", ex);
	    }
	}

	throw new UnsupportedOperationException("The method " + method + " was not recognized and was not annotated with the annotation " + Property.class.getName());
    }

    private Object getConfigItem(String id) {
	r.lock();
	try {
	    return config.get(id);
	} finally {
	    r.unlock();
	}
    }
    
    private Object getDefaultItem(Method method) throws InvocationException, InvalidTypeException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
	if(defaults == null){
	    throw new UnknownValueException("Could not return the value for method " + method.getName() + " the value did not exist in the config set "
		    + "and no defaults were found");
	}
	if(!type.isAssignableFrom(defaults.getClass())){
	    //This SHOULD not be possible, given the generic constructor. But still. Better safe than sorry.
	    throw new InvocationException("Could not get defaults. The defaults "+defaults+" are not of the expected type "+type);
	}
	Method defaultValueProvider;
	try {
	    defaultValueProvider = defaults.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
	} catch (NoSuchMethodException ex) {
	    throw new UnknownValueException("Could not return the value for method " + method.getName() + " the value did not exist in the config set "
		    + "and no matching method existed in the defaults", ex);
	}
	return defaultValueProvider.invoke(defaults, new Object[0]);

	
	
    }

    @Override
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
	return ocd;
    }

    @Override
    public String[] getLocales() {
	return null;
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
	w.lock();
	TreeSet<String> required = new TreeSet<String>();
	required.addAll(requiredIds);
	Map<String, Object> newprops = new HashMap<>();
	try {
	    Enumeration<String> keys = properties.keys();
	    while (keys.hasMoreElements()) {
		String key = keys.nextElement();

		if (key.equals("service.pid")) {
		    continue;
		}

		AD definition = null;
		for (AD possibleDefinition : attributeToMethodMapping.values()) {
		    if (possibleDefinition.getID().equals(key)) {
			definition = possibleDefinition;
			break;
		    }
		}
		if (definition == null) {
		    throw new ConfigurationException(key, "Could not load properties. The property " + key + " is not known to this configuration. Supported methods: " + attributeToMethodMapping.keySet());
		}
		Object configValue = null;
		switch (definition.cardinalityDef) {
		    case Optional:
			configValue = retrieveOptionalObject(key, properties.get(key));
			ensureCorrectType(key, configValue, definition.getInputType());
			if (definition.getFilter() != null) {
			    configValue = filterObject(key, configValue, definition.getFilter(), definition.getInputType());
			}
			break;
		    case Required:
			required.remove(definition.getID());
			configValue = properties.get(key);
			ensureCorrectType(key, configValue, definition.getInputType());
			if (definition.getFilter() != null) {
			    configValue = filterObject(key, configValue, definition.getFilter(), definition.getInputType());
			}
			break;
		    case List:
			List<Object> valueAsList = retrieveList(key, properties.get(key), definition.getInputType());
			for(Object configObject : valueAsList){
			    ensureCorrectType(key, configObject, definition.getInputType());
			}
			if (definition.getFilter() != null) {
			    List filteredList = new ArrayList();
			    for (Object value : valueAsList) {
				filteredList.add(filterObject(key, value, definition.getFilter(), definition.getInputType()));
			    }
			    configValue = filteredList;
			} else {
			    configValue = valueAsList;
			}
			break;
		}
		newprops.put(key, configValue);
		/*if (definition.filter != null) {
		 try {
		 TypeFilter filterinstance = definition.filter.newInstance();
		 Object filterValue = filterinstance.parse(configValue);
		 newprops.put(key, filterValue);
		 } catch (InstantiationException | IllegalAccessException ex) {
		 throw new ConfigurationException(key, "Could not load properties. Could not instantiate filter.", ex);
		 } catch (TypeFilterException ex) {
		 throw new ConfigurationException(key, "Could not load properties. Could not filter value.", ex);
		 }
		 } else {
		 if (definition.getInputType().isAssignableFrom(configValue.getClass())) {
		 newprops.put(key, configValue);
		 } else {
		 throw new ConfigurationException(key, "Could not load properties. The property " + key + " is not of the expected type. Expected type was " + definition.getInputType().getName() + " but was " + configValue.getClass().getName());
		 }
		 }*/

	    }
	    if(!required.isEmpty()){
		throw new ConfigurationException(required.pollFirst(), "Could not update configuration. Missing required fields: "+new ArrayList<>(required));
	    }
	    config = newprops;
	} finally {
	    w.unlock();
	}
    }
    
    private void ensureCorrectType(String key, Object configurationValue, Class expectedType) throws ConfigurationException{
	if(!expectedType.isAssignableFrom(configurationValue.getClass())){
	    throw new ConfigurationException(key, "Could not assign "+configurationValue+" to "+key+". It did not match the expected type: "+expectedType);
	}
    }

    private void ensureUniqueIDs(Collection<AD> attributeDefinitions) throws DoubleIDException {
	TreeSet<AD> adSet = new TreeSet<>(new Comparator<AD>() {

	    @Override
	    public int compare(AD o1, AD o2) {
		return o1.getID().compareTo(o2.getID());
	    }
	});

	for (AD attributeDefinition : attributeDefinitions) {
	    if (adSet.contains(attributeDefinition)) {
		throw new DoubleIDException("Could not add the attribute " + attributeDefinition + " with id " + attributeDefinition + ". ID already used");
	    }
	    adSet.add(attributeDefinition);
	}

    }

    private Object retrieveOptionalObject(String key, Object configItemObject) throws ConfigurationException {
	if (!List.class.isAssignableFrom(configItemObject.getClass())) {
	    throw new ConfigurationException(key, "This value should be optional, and be represented by a list: " + configItemObject);
	}
	List configItemList = (List) configItemObject;
	if (configItemList.isEmpty()) {
	    return null;
	} else if (configItemList.size() == 1) {
	    return configItemList.get(0);
	} else {
	    throw new ConfigurationException(key, "The optional value " + key + " had more than one item assigned to it: " + configItemList);
	}
    }

    private List retrieveList(String key, Object configItemObject, Class expectedClass) throws ConfigurationException {
	if (!List.class.isAssignableFrom(configItemObject.getClass())) {
	    throw new ConfigurationException(key, "This value should be a List: " + configItemObject);
	}
	List configItemList = (List) configItemObject;
	for (Object listMember : configItemList) {
	    if (listMember != null && !expectedClass.isAssignableFrom(listMember.getClass())) {
		throw new ConfigurationException(key, "Could not match this object to the expected object. Expected a list of " + expectedClass + " but list contained a " + listMember.getClass() + ": " + listMember);
	    }
	}
	return configItemList;
    }

    private Object filterObject(String key, Object input, Class<? extends TypeFilter> filterType, Class expectedType) throws ConfigurationException {
	if (!expectedType.isAssignableFrom(input.getClass())) {
	    throw new ConfigurationException(key, "Could not match this object to the expected object. Expected " + expectedType + " found " + input.getClass());
	}
	try {
	    TypeFilter filterinstance = filterType.newInstance();
	    Object filterValue = filterinstance.parse(input);
	    return filterValue;
	} catch (InstantiationException | IllegalAccessException ex) {
	    throw new ConfigurationException(key, "Could not load properties. Could not instantiate filter.", ex);
	} catch (TypeFilterException ex) {
	    throw new ConfigurationException(key, "Could not load properties. Could not filter value.", ex);
	}
    }

    @Override
    public void addConfigurationCallback(ConfigurationCallback callback) {
	synchronized (callbacks) {
	    callbacks.add(callback);
	}
    }

    @Override
    public void removeConfigurationCallback(ConfigurationCallback callback) {
	synchronized (callbacks) {
	    callbacks.remove(callback);
	}
    }

    @Override
    public List<ConfigurationCallback> getConfigurationCallbacks() {
	synchronized (callbacks) {
	    return new ArrayList<>(callbacks);
	}
    }

    @Override
    public Lock lockPropertiesUpdate() {
	r.lock();
	return r;
    }

    @Override
    public void unregisterProperties() {
	managedServiceReg.unregister();
	metatypeServiceReg.unregister();
	managedServiceReg = null;
	metatypeServiceReg = null;
    }

    /**
     * Registers this ManagedProperties object with the bundle context. This should be done in the bundle activator right just after this object is added to its
     * intended service.
     *
     * @param context The context in which to register this ManagedProperties object.
     */
    public void register(BundleContext context) {
	Hashtable<String, Object> managedServiceProps = new Hashtable<>();
	managedServiceProps.put(Constants.SERVICE_PID, typeDefinition.id());
	managedServiceReg = context.registerService(ManagedService.class, this, managedServiceProps);

	Hashtable<String, Object> metaTypeProps = new Hashtable<>();
	metaTypeProps.put(Constants.SERVICE_PID, typeDefinition.id());
	metaTypeProps.put("metadata.type", "Server");
	metaTypeProps.put("metadata.version", "1.0.0");
	metatypeServiceReg = context.registerService(MetaTypeProvider.class, this, metaTypeProps);
    }

    private static ObjectClassDefinition buildOCD(String id, String name, String description, String file, Collection<AD> attributes) throws InvalidTypeException {

	if (logger.isDebugEnabled()) {
	    logger.debug("Building ObjectClassDefinition for '" + name + "'");
	}

	File iconFile = null;
	if (!file.isEmpty()) {
	    iconFile = new File(file);
	}

	OCD newocd = new OCD(id, attributes.toArray(new AttributeDefinition[attributes.size()]));
	newocd.setName(name);
	newocd.setDescription(description);
	newocd.setIconFile(iconFile);
	return newocd;
    }

}
