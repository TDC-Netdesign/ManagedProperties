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
	return new ManagedPropertiesService.ManagedProperties(type);
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
	for(ManagedPropertiesRegistration registration : propertyInstances.values()){
	    registration.properties.unregisterProperties();
	}
	this.context = null;
    }
    
    private static PropertyDefinition getDefinitionAnnotation(Class<?> type) throws InvalidTypeException {
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
    
    protected static class ManagedProperties implements InvocationHandler, MetaTypeProvider, ManagedService, ConfigurationCallbackHandler, EnhancedProperty {

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

	public ManagedProperties(Class<?> type) throws InvalidTypeException, TypeFilterException, DoubleIDException {
	    callbacks = new ArrayList<>();
	    lock = new ReentrantReadWriteLock();
	    r = lock.readLock();
	    w = lock.writeLock();
	    attributeToMethodMapping = new HashMap<>();
	    typeDefinition = getDefinitionAnnotation(type);
	    for (Method classMethod : type.getMethods()) {
		if (classMethod.isAnnotationPresent(Property.class)) {
		    attributeToMethodMapping.put(classMethod.getName(), new AD(classMethod));
		}
	    }
	    
	    ensureUniqueIDs(attributeToMethodMapping.values());
	    
	    
	    
	    ocd = buildOCD(typeDefinition.id(), typeDefinition.name(), typeDefinition.description(), typeDefinition.iconFile(), attributeToMethodMapping.values());
	    
	    allowedMethods = new ArrayList<>();
	    
	    allowedMethods.addAll(Arrays.asList(EnhancedProperty.class.getMethods()));
	    allowedMethods.addAll(Arrays.asList(ConfigurationCallbackHandler.class.getMethods()));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws InvalidTypeException, TypeFilterException, InvocationException {
	    if (method.isAnnotationPresent(Property.class)) {
		AD propertyDefinition = attributeToMethodMapping.get(method.getName());
		Object returnValue = getConfigItem(propertyDefinition.id);
		if (returnValue == null) {
		    throw new InvalidTypeException("Could not return the value for method " + method.getName() + " the value did not exist in the config set.");
		}
		if (!method.getReturnType().isAssignableFrom(returnValue.getClass())) {
		    throw new InvalidTypeException("Could not return the value for method " + method.getName() + " the value " + returnValue + " had Type " + returnValue.getClass() + " expected " + method.getReturnType());
		}
		return returnValue;
	    } else if (allowedMethods.contains(method)) {
		try {
		    return method.invoke(this, args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
		    throw new InvocationException("Could not execute method. Execution of method "+method+" failed", ex);
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
			    if (definition.filter != null) {
				configValue = filterObject(key, configValue, definition.filter, definition.inputType);
			    }
			    break;
			case Required:
			    configValue = properties.get(key);
			    if (definition.filter != null) {
				configValue = filterObject(key, configValue, definition.filter, definition.inputType);
			    }
			    break;
			case List:
			    List<Object> valueAsList = retrieveList(key, properties.get(key), definition.inputType);
			    if (definition.filter != null) {
				List filteredList = new ArrayList();
				for(Object value : valueAsList){
				    filteredList.add(filterObject(key, value, definition.filter, definition.inputType));
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
		config = newprops;
	    } finally {
		w.unlock();
	    }
	}
	
	private void ensureUniqueIDs(Collection<AD> attributeDefinitions) throws DoubleIDException{
	    TreeSet<AD> adSet = new TreeSet<>(new Comparator<AD>() {

		@Override
		public int compare(AD o1, AD o2) {
		    return o1.getID().compareTo(o2.getID());
		}
	    });
	    
	    for(AD attributeDefinition : attributeDefinitions){
		if(adSet.contains(attributeDefinition)){
		    throw new DoubleIDException("Could not add the attribute "+attributeDefinition+" with id "+attributeDefinition+". ID already used");
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
	 * Registers this ManagedProperties object with the bundle context. This should be done in the bundle activator right just after this object is added to
	 * its intended service.
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

    

    private static class OCD implements ObjectClassDefinition {

	private String description;
	private String name;
	// Represents the attributes of this object class
	private AttributeDefinition[] requiredADs = new AttributeDefinition[2];
	private String id;
	private File iconFile;

	protected OCD(String id, AttributeDefinition[] requiredADs) {
	    this.id = id;
	    this.requiredADs = requiredADs;
	}

	@Override
	public AttributeDefinition[] getAttributeDefinitions(int filter) {
	    if (filter == ObjectClassDefinition.OPTIONAL) {
		return null;
	    }
	    return requiredADs;
	}

	@Override
	public String getDescription() {
	    return description;
	}

	@Override
	public InputStream getIcon(int iconDimension) throws IOException {
	    BufferedImage img = null;
	    img = ImageIO.read(iconFile);

	    BufferedImage resizedImage = scaleImage(img, iconDimension, iconDimension, Color.DARK_GRAY);

	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(resizedImage, "jpg", os);
	    return new ByteArrayInputStream(os.toByteArray());

	}

	@Override
	public String getID() {
	    return id;
	}

	@Override
	public String getName() {
	    return name;
	}

	protected void setName(String name) {
	    this.name = name;
	}

	protected void setDescription(String description) {
	    this.description = description;
	}

	protected void setIconFile(File iconFile) {
	    this.iconFile = iconFile;
	}

	public BufferedImage scaleImage(BufferedImage img, int width, int height,
		Color background) {
	    int imgWidth = img.getWidth();
	    int imgHeight = img.getHeight();
	    if (imgWidth * height < imgHeight * width) {
		width = imgWidth * height / imgHeight;
	    } else {
		height = imgHeight * width / imgWidth;
	    }
	    BufferedImage newImage = new BufferedImage(width, height,
		    BufferedImage.TYPE_INT_RGB);
	    Graphics2D g = newImage.createGraphics();
	    try {
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setBackground(background);
		g.clearRect(0, 0, width, height);
		g.drawImage(img, 0, 0, width, height, null);
	    } finally {
		g.dispose();
	    }
	    return newImage;
	}

	@Override
	public String toString() {
	    ToStringBuilder builder = new ToStringBuilder(this);
	    builder.append("id", id).append("name", name).append("description", description).append("iconFile", iconFile).append("ADs", requiredADs);
	    return builder.toString();
	}
    }

    private static class AD implements AttributeDefinition {

	private String id;
	private int type;
	private int cardinality;
	private String name;
	private String description;
	private String[] defValue;
	private String[] optionalLabels;
	private String[] optionalValues;
	private Class inputType;
	private Class<? extends TypeFilter> filter;
	Property.Cardinality cardinalityDef;

	protected AD(Method method) throws TypeFilterException, InvalidTypeException {
	    Property methodProperty = method.getAnnotation(Property.class);
	    name = getAttributeName(method);
	    id = name;

	    cardinalityDef = methodProperty.cardinality();
	    inputType = getMethodReturnType(method);
	    type = getAttributeType(inputType);
	    if (logger.isTraceEnabled()) {
		logger.trace("Found @Property on " + method.getName() + "[" + inputType + "]");
	    }

	    if (!methodProperty.id().isEmpty()) {
		id = methodProperty.id();
	    }

	    cardinality = getCardinality(cardinalityDef);

	    Class<? extends TypeFilter> filterFromAnnotation = methodProperty.typeMapper();
	    if (filterFromAnnotation == TypeFilter.class) {
		filter = null;
		if(!method.getReturnType().isAssignableFrom(inputType)){
		    throw new InvalidTypeException("Could not create method definition. The returntype of the method '"+method.getReturnType()+"' is not compatible with the returntype of the Property '"+inputType+"'.");
		}
	    } else {
		filter = filterFromAnnotation;
		Method parseMethod;
		try {
		    parseMethod = filter.getDeclaredMethod("parse", new Class[]{inputType});
		} catch (NoSuchMethodException | SecurityException ex) {
		    throw new TypeFilterException("Could not add filter. An exception occured while trying to examine the parse method", ex);
		}
		if(!method.getReturnType().isAssignableFrom(parseMethod.getReturnType())){
		    throw new TypeFilterException("Could not use Filter type "+filter.getName()+" with the method "+method.getName()+
		    " The returntype of the method "+method.getReturnType()+"must be assignable to the return of the filter parse method"+parseMethod.getReturnType());
		}
		if(method.getReturnType().isInterface() || Modifier.isAbstract(method.getReturnType().getModifiers())){
		    throw new TypeFilterException("Could not instantiate filter for "+method+". Filter does not return an instantiable value.");
		}
	    }
	    
	    

	    logger.trace("Building AttributeDefinition with attributeID '" + id + "' attributeType '" + type + "' cardinality '" + cardinality + "'");
	    defValue = methodProperty.defaultValue();
	    description = methodProperty.description();
	    optionalLabels = methodProperty.optionLabels();
	    optionalValues = methodProperty.optionValues();
	}

	protected AD(String id, int type, int cardinality) {
	    this.id = id;
	    this.type = type;
	    this.cardinality = cardinality;
	}

	private static String getAttributeName(Method classMethod) {
	    String attributeName = classMethod.getName().replaceAll("^get", "");
	    if (classMethod.isAnnotationPresent(Property.class)) {
		Property methodProperty = classMethod.getAnnotation(Property.class);
		if (!methodProperty.name().isEmpty()) {
		    attributeName = methodProperty.name();
		}
	    }
	    return attributeName;
	}

	private static Class getMethodReturnType(Method classMethod) {
	    Class methodReturnType = classMethod.getReturnType();
	    if (classMethod.isAnnotationPresent(Property.class)) {
		Property methodProperty = classMethod.getAnnotation(Property.class);
		if (methodProperty.type() != void.class) {
		    methodReturnType = methodProperty.type();
		}
	    }
	    return methodReturnType;
	}

	private static Integer getAttributeType(Class methodReturnType) throws InvalidTypeException {
	    Integer attributeType = null;
	    if (methodReturnType.equals(String.class)) {
		attributeType = AttributeDefinition.STRING;
	    } else if (methodReturnType.equals(Long.class)) {
		attributeType = AttributeDefinition.LONG;
	    } else if (methodReturnType.equals(Integer.class)) {
		attributeType = AttributeDefinition.INTEGER;
	    } else if (methodReturnType.equals(Short.class)) {
		attributeType = AttributeDefinition.SHORT;
	    } else if (methodReturnType.equals(Character.class)) {
		attributeType = AttributeDefinition.CHARACTER;
	    } else if (methodReturnType.equals(Byte.class)) {
		attributeType = AttributeDefinition.BYTE;
	    } else if (methodReturnType.equals(Double.class)) {
		attributeType = AttributeDefinition.DOUBLE;
	    } else if (methodReturnType.equals(Float.class)) {
		attributeType = AttributeDefinition.FLOAT;
	    } else if (methodReturnType.equals(BigInteger.class)) {
		attributeType = AttributeDefinition.BIGINTEGER;
	    } else if (methodReturnType.equals(BigDecimal.class)) {
		attributeType = AttributeDefinition.BIGDECIMAL;
	    } else if (methodReturnType.equals(Boolean.class)) {
		attributeType = AttributeDefinition.BOOLEAN;
	    } else if (methodReturnType.equals(Character[].class)) {
		attributeType = AttributeDefinition.PASSWORD;
	    }
	    if(attributeType == null){
		throw new InvalidTypeException("Could not create Attribute definition. The type "+methodReturnType+" is not a valid type for an InputType");
	    }
	    return attributeType;
	}

	private static int getCardinality(Property.Cardinality cardinality) {
	    int adCardinality = 0;
	    switch (cardinality) {
		case Required:
		    adCardinality = 0;
		    break;
		case Optional:
		    adCardinality = -1;
		    break;
		case List:
		    adCardinality = Integer.MIN_VALUE;
		    break;
	    }
	    return adCardinality;
	}

	@Override
	public int getCardinality() {
	    return cardinality;
	}

	@Override
	public String[] getDefaultValue() {
	    return defValue;
	}

	@Override
	public String getDescription() {
	    return description;
	}

	@Override
	public String getID() {
	    return id;
	}

	@Override
	public String getName() {
	    return name;
	}

	@Override
	public String[] getOptionLabels() {
	    return optionalLabels;
	}

	@Override
	public String[] getOptionValues() {
	    return optionalValues;
	}

	@Override
	public int getType() {
	    return type;
	}

	@Override
	public String validate(String arg0) {
	    return null;
	}

	protected void setOptionLabels(String[] labels) {
	    optionalLabels = labels;
	}

	protected void setOptionValues(String[] values) {
	    optionalValues = values;
	}

	protected void setDescription(String description) {
	    this.description = description;
	}

	protected void setName(String name) {
	    this.name = name;
	}

	protected void setDefaultValue(String[] defValue) {
	    this.defValue = defValue;
	}

	public Class getInputType() {
	    return inputType;
	}

	public void setInputType(Class inputType) {
	    this.inputType = inputType;
	}

	public Class<? extends TypeFilter> getFilter() {
	    return filter;
	}

	public void setFilter(Class<? extends TypeFilter> filter) {
	    this.filter = filter;
	}

	public Property.Cardinality getCardinalityDef() {
	    return cardinalityDef;
	}

	public void setCardinalityDef(Property.Cardinality cardinalityDef) {
	    this.cardinalityDef = cardinalityDef;
	}

	@Override
	public String toString() {
	    ToStringBuilder builder = new ToStringBuilder(this);
	    builder.append("id", id).append("name", name).append("type", type).append("cardinality", cardinality).append("description", description)
		    .append("defValue", defValue).append("optionalLabels", optionalLabels).append("optionalValues", optionalValues);
	    return builder.toString();
	}

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
