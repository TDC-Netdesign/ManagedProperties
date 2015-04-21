/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;



/**
 * The ManagedProperties class is used as a simple setup mechanism for the Configuration Admin and MetaType OSGi services.
 * Using the register method, the ManagedProperties register itself as both a Configuration Admin ManagedService and as a MetaType service MetaTypeProvider.
 * This class can be used directly, but is best used as a superclass. For the best utliziation, it is advised to extends this class, and add get methods for each
 * configuration item. Then annotate the get methods with the @Property annotation, and use the get(String, Class) method to cast the object to the expected type.
 * Reflection will take care of creating the MetaTypeProvider items and ensure that the type returned to the service matches the @Property.
 * @author mnn
 */
public class ManagedProperties implements Map<String, Object>, ManagedService, MetaTypeProvider, ConfigurationCallbackHandler {
    private static Logger logger = Logger.getLogger(ManagedProperties.class);
    //private ServiceRegistration<ManagedService> managedServiceReg;
    //private ServiceRegistration<MetaTypeProvider> metatypeServiceReg;
    private ServiceRegistration managedServiceReg;
    private ServiceRegistration metatypeServiceReg;
    
    private ReadWriteLock lock;
    private Lock r;
    private Lock w;
    private Map<String, Object> props;
    private Map<String, Object> defaults;
    private String name;
    private String id;
    private String description;
    private File iconFile;
    private List<ConfigurationCallback> callbacks;

    private ObjectClassDefinition ocd;

    /**
     * Create a ManagedProperties object
     * @param name The name of the properties. This is used to create the name for the MetaData service.
     * @param id This is the PID used to specify the path for the configuration file in the Configuration Admin service.
     * @param description This is used to specify the Description for the bundle for the MetaData service.
     */
    public ManagedProperties(String name, String id, String description) {
	this(null, name, id, description, null);
    }

    /**
     * Create a ManagedProperties object
     * @param name The name of the properties. This is used to create the name for the MetaData service.
     * @param id This is the PID used to specify the path for the configuration file in the Configuration Admin service.
     * @param description This is used to specify the Description for the bundle for the MetaData service.
     * @param iconFile This is used to define the file which is used to generate an Icon for use in the MetaData service.
     */
    public ManagedProperties(String name, String id, String description, File iconFile) {
	this(null, name, id, description, iconFile);
    }

    /**
     * Create a ManagedProperties object
     * @param defaults This map is used to specify a default set of properties, which can be used if no data is returned from the Configuration Admin.
     * @param name The name of the properties. This is used to create the name for the MetaData service.
     * @param id This is the PID used to specify the path for the configuration file in the Configuration Admin service.
     * @param description This is used to specify the Description for the bundle for the MetaData service.
     * @param iconFile This is used to define the file which is used to generate an Icon for use in the MetaData service.
     */
    public ManagedProperties(Map<String, Object> defaults, String name, String id, String description, File iconFile) {
	this.defaults = defaults;
	this.name = name;
	this.id = id;
	this.description = description;
	this.iconFile = iconFile;
	
	props = new HashMap<>();
	lock = new ReentrantReadWriteLock(true);
	r = lock.readLock();
	w = lock.writeLock();
	ocd = this.buildOCD();
	callbacks = new ArrayList<>();
	logger.info("Created new ManagedProperties for "+id+" with name: '"+name+"' and ObjectClassDefinition: "+ocd);
    }
    
    /**
     * Create a ManagedProperties object
     * @param name The name of the properties. This is used to create the name for the MetaData service.
     * @param description This is used to specify the Description for the bundle for the MetaData service.
     * @param configuredClass This is used to generate the PID used to specify the path for the configuration file in the Configuration Admin service.
     */
    public ManagedProperties(String name, String description, Class configuredClass) {
	this(name, configuredClass.getName(), description);
    }

    /**
     * Create a ManagedProperties object
     * @param name The name of the properties. This is used to create the name for the MetaData service.
     * @param description This is used to specify the Description for the bundle for the MetaData service.
     * @param iconFile This is used to define the file which is used to generate an Icon for use in the MetaData service.
     * @param configuredClass This is used to generate the PID used to specify the path for the configuration file in the Configuration Admin service.
     */
    public ManagedProperties(String name, String description, File iconFile, Class configuredClass) {
	this(name, configuredClass.getName(), description, iconFile);
    }

    /**
     * Create a ManagedProperties object
     * @param defaults This map is used to specify a default set of properties, which can be used if no data is returned from the Configuration Admin.
     * @param name The name of the properties. This is used to create the name for the MetaData service.
     * @param description This is used to specify the Description for the bundle for the MetaData service.
     * @param iconFile This is used to define the file which is used to generate an Icon for use in the MetaData service.
     * @param configuredClass This is used to generate the PID used to specify the path for the configuration file in the Configuration Admin service.
     */
    public ManagedProperties(Map<String, Object> defaults, String name, String description, File iconFile, Class configuredClass) {
	this(defaults, name, configuredClass.getName(), description, iconFile);
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getDefaults() {
	return defaults;
    }

    /**
     *
     * @param defaults
     */
    public void setDefaults(Map<String, Object> defaults) {
	this.defaults = defaults;
    }
    
    /**
     * Registers this ManagedProperties object with the bundle context. This should be done in the bundle activator right just after this object is added to its
     * intended service.
     * @param context The context in which to register this ManagedProperties object.
     */
    public void register(BundleContext context){
	Hashtable<String, Object> managedServiceProps = new Hashtable<String, Object>();
	managedServiceProps.put(Constants.SERVICE_PID, id);
	managedServiceReg = context.registerService(ManagedService.class.getName(), this, managedServiceProps);
	
	Hashtable<String, Object> metaTypeProps = new Hashtable<>();
	metaTypeProps.put(Constants.SERVICE_PID, id);
	metaTypeProps.put("metadata.type", "Server");
	metaTypeProps.put("metadata.version", "1.0.0");
	metatypeServiceReg = context.registerService(MetaTypeProvider.class.getName(), this, metaTypeProps);
    }
    
    /**
     * Unregisters this ManagedProperties object in this bundle context.
     * @param context The context in which to unregister this ManagedProperties object.
     */
    public void unregister(BundleContext context){
	managedServiceReg.unregister();
	metatypeServiceReg.unregister();
    }

    /**
     * In case that some components in the service needs to be notified of a change in configuration, it can register an instance of ConfigurationCallback.
     * Whenever the configuration in this ManagedProperties object is updated, the configurationUpdated method of the ConfigurationCallback is called.
     * @param callback The callback instance to register
     */
    @Override
    public void addConfigurationCallback(ConfigurationCallback callback) {
	callbacks.add(callback);
    }

    /**
     * Unregisters the callback
     * @param callback The callback instance to unregister
     */
    @Override
    public void removeConfigurationCallback(ConfigurationCallback callback) {
	callbacks.remove(callback);
    }

    /**
     * Gets a list of all registered ConfigurationCallbacks
     * @return All registered callbacks
     */
    @Override
    public List<ConfigurationCallback> getConfigurationCallbacks() {
	return new ArrayList<>(callbacks);
    }
    
    /**
     * This method is called by the ConfigurationAdmin whenever the configuration is updated. This updates the configuration and updates all registered callbacks.
     * The method checks if the objects returned match the expected class of any configurations denoted by a @Property annotation.
     * @param dctnr The new configuration
     * @throws ConfigurationException A ConfigurationException is thrown if a configuration element is found not to match the expected class defined by the 
     * @Property
     */
    @Override
    public void updated(Dictionary<String, ?> dctnr) throws ConfigurationException {
	logger.info("Configuration updated for "+id);
	Map<String, Class> objectMappings = getObjectMappings();
	Map<String, Property.Cardinality> cardinalityMappings = getCardinalityMappings();
	
	HashMap<String, Object> newprops = new HashMap<>();
	if (dctnr != null) {
	    for (Enumeration<String> keys = dctnr.keys(); keys.hasMoreElements();) {
		String key = keys.nextElement();
		Object value = dctnr.get(key);
		Class configObjectClass = value.getClass();
		Class expectedClass = objectMappings.get(key);
		Property.Cardinality cardinality = cardinalityMappings.get(key);
		
		if(key.equals("service.pid")){
		    continue;
		}
		
		if(logger.isDebugEnabled()){
		    logger.debug("Attempting to assign "+dctnr.get(key)+" to "+key+" with cardinality "+cardinality.toString());
		}
		
		if(cardinality.equals(Property.Cardinality.Required)){
		    if(expectedClass != null && !expectedClass.isAssignableFrom(configObjectClass)){
			throw new ConfigurationException(key, "Could not match this object to the expected object. Expected "+expectedClass+" found "+configObjectClass);
		    }
		    newprops.put(key, value);
		}else if(cardinality.equals(Property.Cardinality.Optional)){
		    newprops.put(key, retrieveOptionalObject(key, value));   
		}else if(cardinality.equals(Property.Cardinality.List)){
		    if(!List.class.isAssignableFrom(configObjectClass)){
			throw new ConfigurationException(key, "This value should be a List: "+value); 
		    }
		    List configItemList = (List)value;
		    for(Object listMember : configItemList){
			if(listMember != null && !expectedClass.isAssignableFrom(listMember.getClass())){
			    throw new ConfigurationException(key, "Could not match this object to the expected object. Expected a list of "+expectedClass+" but list contained a "+listMember.getClass()+": "+listMember);
			}
		    }
		    newprops.put(key, dctnr.get(key));
		}
		
		
	    }
	} else {
	    if (defaults != null) {
		newprops.putAll(defaults);
	    }
	}
	try {
	    w.lock();
	    props = newprops;
	} finally {
	    w.unlock();
	}
	for(ConfigurationCallback callback : callbacks){
	    callback.configurationUpdated(dctnr);
	}
    }
    
    private Object retrieveOptionalObject(String key, Object configItemObject) throws ConfigurationException{
	if(!List.class.isAssignableFrom(configItemObject.getClass())){
	    throw new ConfigurationException(key, "This value should be optional, and be represented by a list: "+configItemObject); 
	}
	List configItemList = (List)configItemObject;
	if(configItemList.isEmpty()){
	    return null;
	}else if(configItemList.size() == 1){
	    return configItemList.get(0);
	}else{
	    throw new ConfigurationException(key, "The optional value "+key+ " had more than one item assigned to it: "+configItemList);
	}
    }
   
      
    private Map<String,Class> getObjectMappings(){
	HashMap<String,Class> mappings = new HashMap<>();
	for (Method classMethod : this.getClass().getMethods()) {
	    if (classMethod.isAnnotationPresent(Property.class)) {
		mappings.put(getAttributeName(classMethod), getMethodReturnType(classMethod));
	    }
	}
	return mappings;
    }
    
    private Map<String,Property.Cardinality> getCardinalityMappings(){
	HashMap<String,Property.Cardinality> mappings = new HashMap<>();
	for (Method classMethod : this.getClass().getMethods()) {
	    if (classMethod.isAnnotationPresent(Property.class)) {
		Property annotation = classMethod.getAnnotation(Property.class);
		mappings.put(getAttributeName(classMethod), annotation.cardinality());
	    }
	}
	return mappings;
    }

    @Override
    public int size() {
	try {
	    r.lock();
	    return props.size();
	} finally {
	    r.unlock();
	}
    }

    @Override
    public boolean isEmpty() {
	try {
	    r.lock();
	    return props.isEmpty();
	} finally {
	    r.unlock();
	}
    }

    @Override
    public boolean containsKey(Object key) {
	try {
	    r.lock();
	    return props.containsKey(key);
	} finally {
	    r.unlock();
	}
    }

    @Override
    public boolean containsValue(Object value) {
	try {
	    r.lock();
	    return props.containsValue(value);
	} finally {
	    r.unlock();
	}
    }

    @Override
    public Object get(Object key) {
	try {
	    r.lock();
	    return props.get(key);
	} finally {
	    r.unlock();
	}
    }
    
    /**
     * Gets the value denoted by the key, and casts it to the defined type
     * @param <T> The object type to return
     * @param key The key to get the configuration for
     * @param type The type to cast the configuration object to
     * @throws ClassCastException As this class casts the object, a ClassCastException can occur if the cast to the defined type is not possible.
     * @return
     */
    public <T> T get(String key, Class<T> type){
	try {
	    r.lock();
	    return type.cast(get(key));
	} finally {
	    r.unlock();
	}
    }

    @Override
    public Object put(String key, Object value) {
	try {
	    w.lock();
	    return props.put(key, value);
	} finally {
	    w.unlock();
	}
    }

    @Override
    public Object remove(Object key) {
	try {
	    w.lock();
	    return props.remove(key);
	} finally {
	    w.unlock();
	}
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
	try {
	    w.lock();
	    props.putAll(m);
	} finally {
	    w.unlock();
	}
    }

    @Override
    public void clear() {
	try {
	    w.lock();
	    props.clear();
	} finally {
	    w.unlock();
	}
    }

    @Override
    public Set<String> keySet() {
	try {
	    r.lock();
	    return props.keySet();
	} finally {
	    r.unlock();
	}
    }

    @Override
    public Collection<Object> values() {
	try {
	    r.lock();
	    return props.values();
	} finally {
	    r.unlock();
	}
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
	try {
	    r.lock();
	    return props.entrySet();
	} finally {
	    r.unlock();
	}
    }

    /**
     * Returns the ObjectClassDefinition used by the MetaType service
     * @param id OCD id to return
     * @param locale The OCD locale to return
     * @return
     */
    @Override
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
	return ocd;
    }

    /**
     * Returns supported locales
     * @return Supported locale names
     */
    @Override
    public String[] getLocales() {
	return null;
    }
    
    private ObjectClassDefinition buildOCD() {
	logger.debug("Building ObjectClassDefinition");
	List<AttributeDefinition> attributes = new ArrayList<>();
	for (Method classMethod : this.getClass().getMethods()) {
	    if (classMethod.isAnnotationPresent(Property.class)) {
		Property methodProperty = classMethod.getAnnotation(Property.class);
		String attributeName = getAttributeName(classMethod);
		String attributeID = attributeName;
		Integer attributeType = null;
		Property.Cardinality cardinality = methodProperty.cardinality();
		Class methodReturnType = getMethodReturnType(classMethod);
		logger.trace("Found @Property on "+classMethod.getName()+"["+methodReturnType+"]");

		
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

		if (!methodProperty.id().isEmpty()) {
		    attributeID = methodProperty.id();
		}
		
		int adCardinality = 0;
		switch (cardinality){
		    case Required:    adCardinality=0;
				      break;
		    case Optional:    adCardinality=-1;
				      break;
		    case List:	      adCardinality=Integer.MIN_VALUE;
				      break;
		}
		
		logger.trace("Building AttributeDefinition with attributeID '"+attributeID+"' attributeType '"+attributeType+"' cardinality '"+cardinality+"'");
		AD ad = new AD(attributeID, attributeType, adCardinality);
		ad.setDefaultValue(methodProperty.defaultValue());
		ad.setDescription(methodProperty.description());
		ad.setName(attributeName);
		ad.setOptionLabels(methodProperty.optionLabels());
		ad.setOptionValues(methodProperty.optionValues());

		attributes.add(ad);
	    }
	}
	OCD newocd = new OCD(id, attributes.toArray(new AttributeDefinition[attributes.size()]));
	newocd.setName(name);
	newocd.setDescription(description);
	newocd.setIconFile(iconFile);
	return newocd;
    }
    
    private String getAttributeName(Method classMethod){
	String attributeName = classMethod.getName().replaceAll("^get", "");
	 if (classMethod.isAnnotationPresent(Property.class)) {
	    Property methodProperty = classMethod.getAnnotation(Property.class);
	    if (!methodProperty.name().isEmpty()) {
		attributeName = methodProperty.name();
	    }
	}
	return attributeName;
    }
    
    private Class getMethodReturnType(Method classMethod){
	Class methodReturnType = classMethod.getReturnType();
	if (classMethod.isAnnotationPresent(Property.class)) {
	    Property methodProperty = classMethod.getAnnotation(Property.class);
	    if (methodProperty.type() != void.class) {
		methodReturnType = methodProperty.type();
	    }
	}
	return methodReturnType;
    }

    private class OCD implements ObjectClassDefinition {

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

    private class AD implements AttributeDefinition {

	private String id;
	private int type = 0;
	private int cardinality = 0;
	private String name;
	private String description;
	private String[] defValue;
	private String[] optionalLabels;
	private String[] optionalValues;

	protected AD(String id, int type, int cardinality) {
	    this.id = id;
	    this.type = type;
	    this.cardinality = cardinality;
	}

	public int getCardinality() {
	    return cardinality;
	}

	public String[] getDefaultValue() {
	    return defValue;
	}

	public String getDescription() {
	    return description;
	}

	public String getID() {
	    return id;
	}

	public String getName() {
	    return name;
	}

	public String[] getOptionLabels() {
	    return optionalLabels;
	}

	public String[] getOptionValues() {
	    return optionalValues;
	}

	public int getType() {
	    return type;
	}

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
	
	@Override
	public String toString() {
	    ToStringBuilder builder = new ToStringBuilder(this);
	    builder.append("id", id).append("name", name).append("type", type).append("cardinality", cardinality).append("description", description)
		    .append("defValue", defValue).append("optionalLabels", optionalLabels).append("optionalValues", optionalValues);
	    return builder.toString();
	}
    }

}
