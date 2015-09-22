
package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.service.ManagedPropertiesBroker;
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
import dk.netdesign.common.osgi.config.service.TypeFilter;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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
 * The ManagedProperties class is used as a simple setup mechanism for the Felix Configuration Admin and MetaType OSGi services. Using the register method, the
 * ManagedProperties register itself as both a Configuration Admin ManagedService and as a MetaType service MetaTypeProvider. This class acts as the service
 * anchor for the configuration, as well as an InvocationHandler for java.util.Proxy. The class also takes care of creating both the ObjectClassDefinition and
 * AttributeDefinition to reflect the methods in the registered interface.
 *
 * @author mnn
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
    
    /**
     * Returns a ManagedProperties object with the defined defaults.
     * @param <E> The type of object to return.
     * @param type The type of configuration to create. This MUST be an interface and the interface must be annotated with 
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition PropertyDefinition}.
     * @param defaults The default values to use if a configuration item is not currently in the configuration set. The defaults must be an object
     * of a type that implements the type denoted by @see I.
     * @throws InvalidTypeException If a method/configuration item mapping uses an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any restriction not defined in the other exceptions.
     */
    public <E> ManagedProperties(Class<? super E> type, E defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException{
	callbacks = new ArrayList<>();
	lock = new ReentrantReadWriteLock();
	r = lock.readLock();
	w = lock.writeLock();
	attributeToMethodMapping = new HashMap<>();
	typeDefinition = ManagedPropertiesBroker.getDefinitionAnnotation(type);
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

	allowedMethods.addAll(Arrays.asList(EnhancedProperty.class.getDeclaredMethods()));
	allowedMethods.addAll(Arrays.asList(ConfigurationCallbackHandler.class.getDeclaredMethods()));
	allowedMethods.addAll(Arrays.asList(Object.class.getDeclaredMethods()));
	this.type = type;
	this.defaults = defaults;
    }

    /**
     * The same as the other constructor, but does not set defaults.
     * @see #ManagedProperties(java.lang.Class, java.lang.Object) 
     * @param type The type of configuration to create. This MUST be an interface and the interface must be annotated with 
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition PropertyDefinition}.
     * @throws InvalidTypeException If a method/configuration item mapping uses an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any restriction not defined in the other exceptions.
     */
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

	throw new UnsupportedOperationException("The method " + method + " was not recognized and was not annotated with the annotation " + Property.class.getName()+" allowed methods: "+allowedMethods);
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
	logger.info("Getting OCD for "+id+" "+locale);
	return ocd;
    }

    @Override
    public String[] getLocales() {
	return null;
    }

    /**
     * The updated method is called when the ConfigAdmin detects a change in the configuration bound to this class.
     * When called, the method will parse the Dictionary of properties and parse the data for each Method/Configuration item.
     * For each Configuration Item, the type of the object is checked against the return types, the filters are run, and the data parsed.
     * The result is that it is the final, parsed and validated versions of the configuration that is actually returned. The original properties are never
     * stored in this object.
     * @param properties The properties that are sent from the Configuration Admin
     * @throws ConfigurationException If the data is invalid, or a filter fails.
     */
    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
	logger.info("Attempting to update properties on "+typeDefinition.name()+"["+typeDefinition.id()+"] with "+properties);
	if(properties == null){
	    return;
	}
	w.lock();
	TreeSet<String> required = new TreeSet<String>();
	required.addAll(requiredIds);
	Map<String, Object> newprops = new HashMap<>();
	try {
	    Enumeration<String> keys = properties.keys();
	    while (keys.hasMoreElements()) {
		String key = keys.nextElement();

		if (key.equals("service.pid") ||key.equals("felix.fileinstall.filename")) {
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
	logger.info("updated configuration\n"+this);
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
     * Registers this ManagedProperties object with the bundle context. This is done when the proxy is first created.
     *
     * @param context The context in which to register this ManagedProperties object.
     */
    public void register(BundleContext context) {
	Hashtable<String, Object> managedServiceProps = new Hashtable<>();
	managedServiceProps.put(Constants.SERVICE_PID, typeDefinition.id());
	managedServiceReg = context.registerService(ManagedService.class.getName(), this, managedServiceProps);

	Hashtable<String, Object> metaTypeProps = new Hashtable<>();
	metaTypeProps.put(Constants.SERVICE_PID, typeDefinition.id());
	metaTypeProps.put("metadata.type", "Server");
	metaTypeProps.put("metadata.version", "1.0.0");
	metatypeServiceReg = context.registerService(MetaTypeProvider.class.getName(), this, metaTypeProps);
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

    @Override
    public String toString() {
	ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
	builder.append("id", typeDefinition.id());
	builder.append("name", typeDefinition.name());
	builder.append("description", typeDefinition.description());
	r.lock();
	try{
	    for(String key : config.keySet()){
		builder.append(key, config.get(key));
	    }
	}finally{
	    r.unlock();
	}
	return builder.build();
    }
    
    

}
