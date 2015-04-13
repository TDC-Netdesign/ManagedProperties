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
 *
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

    public ManagedProperties(String name, String id, String description) {
	this(null, name, id, description, null);
    }

    public ManagedProperties(String name, String id, String description, File iconFile) {
	this(null, name, id, description, iconFile);
    }

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
    
    public ManagedProperties(String name, String description, Class configuredClass) {
	this(name, configuredClass.getName(), description);
    }

    public ManagedProperties(String name, String description, File iconFile, Class configuredClass) {
	this(name, configuredClass.getName(), description, iconFile);
    }

    public ManagedProperties(Map<String, Object> defaults, String name, String description, File iconFile, Class configuredClass) {
	this(defaults, name, configuredClass.getName(), description, iconFile);
    }

    public Map<String, Object> getDefaults() {
	return defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
	this.defaults = defaults;
    }
    
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
    
    public void unregister(BundleContext context){
	managedServiceReg.unregister();
	metatypeServiceReg.unregister();
    }

    @Override
    public void addConfigurationCallback(ConfigurationCallback callback) {
	callbacks.add(callback);
    }

    @Override
    public void removeConfigurationCallback(ConfigurationCallback callback) {
	callbacks.remove(callback);
    }

    @Override
    public List<ConfigurationCallback> getConfigurationCallbacks() {
	return new ArrayList<>(callbacks);
    }
    

    @Override
    public void updated(Dictionary<String, ?> dctnr) throws ConfigurationException {
	logger.info("Configuration updated for "+id);
	HashMap<String, Object> newprops = new HashMap<>();
	if (dctnr != null) {
	    for (Enumeration<String> keys = dctnr.keys(); keys.hasMoreElements();) {
		String key = keys.nextElement();
		newprops.put(key, dctnr.get(key));
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
    
    public <T> T get(String key, Class<T> type) {
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

    @Override
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
	return ocd;
    }

    @Override
    public String[] getLocales() {
	return null;
    }
    
    private ObjectClassDefinition buildOCD() {
	List<AttributeDefinition> attributes = new ArrayList<>();
	for (Method classMethod : this.getClass().getMethods()) {
	    if (classMethod.isAnnotationPresent(Property.class)) {
		Property methodProperty = classMethod.getAnnotation(Property.class);
		String attributeName = classMethod.getName().replaceAll("^get", "");
		String attributeID = attributeName;
		Integer attributeType = null;
		Integer cardinality = -1;

		Class methodReturnType = classMethod.getReturnType();
		if (methodProperty.type() != void.class) {
		    methodReturnType = methodProperty.type();
		}
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
		} else if (methodReturnType.equals(byte[].class)) {
		    attributeType = AttributeDefinition.PASSWORD;
		}

		if (!methodProperty.name().isEmpty()) {
		    attributeName = methodProperty.name();
		    attributeID = attributeName;
		}
		
		if (methodProperty.cardinality() != -1) {
		    cardinality = methodProperty.cardinality();
		}
		if (!methodProperty.id().isEmpty()) {
		    attributeID = methodProperty.id();
		}
		AD ad = new AD(attributeID, attributeType, cardinality);
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
	private int cardinality = -1;
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
