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

package dk.netdesign.common.osgi.config.osgi;

import dk.netdesign.common.osgi.config.service.ManagedPropertiesProvider;
import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationTarget;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.ParsingException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public class ConfigurationAdminProvider extends ManagedPropertiesProvider implements MetaTypeProvider, ManagedService{
    public static final String BindingID = "ManagedPropertiesBinding";
    public static final String ConfigID = "ManagedPropertiesID";
    public static final String ConfigName = "ManagedPropertiesName";
    
    private static final String VALUE= "value";

    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationAdminProvider.class);
    private final BundleContext bundleContext;
    private final OCD ocd;
    private final ManagedPropertiesController controller;
    private Dictionary<String, ?> lastAppliedProperties;
    
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configurationAdminTracker;
    private ServiceRegistration<ManagedService> managedServiceReg;
    private ServiceRegistration<MetaTypeProvider> metatypeServiceReg;
    private ServiceRegistration<ManagedPropertiesController> selfReg;
    private ServiceRegistration proxyRegistration;
    
    private final Pattern listPattern = Pattern.compile("\\(\\ ((\\w\\\".+?\\\"),\\s*)*\\)");  
    private final Pattern listElementPattern = Pattern.compile("(\\w\\\".+?\\\")");
    private final Pattern doublePattern = Pattern.compile("D\"(?<"+VALUE+">.+)\"");
    private final Pattern floatPattern = Pattern.compile("F\"(?<"+VALUE+">.+)\"");
    private final Pattern integerPattern = Pattern.compile("I\"(?<"+VALUE+">.+)\"");
    private final Pattern longPattern = Pattern.compile("L\"(?<"+VALUE+">.+)\"");
    private final Pattern booleanPattern = Pattern.compile("B\"(?<"+VALUE+">.+)\"");
    private final Pattern bytePattern = Pattern.compile("X\"(?<"+VALUE+">.+)\"");
    private final Pattern charPattern = Pattern.compile("C\"(?<"+VALUE+">.+)\"");
    private final Pattern shortPattern = Pattern.compile("S\"(?<"+VALUE+">.+)\"");

    public ConfigurationAdminProvider(BundleContext bundleContext, ManagedPropertiesController controller, ConfigurationTarget target) throws InvalidTypeException {
	super(target);
	this.bundleContext = bundleContext;
	this.controller = controller;
	Class type = getTarget().getConfigurationType();
	List<AttributeDefinition> attributes = new ArrayList<>();
	for(Attribute attribute : target.getAttributes()){
	    attributes.add(new MetaTypeAttributeDefinition(attribute));
	}
	ocd = buildOCD(target.getID(), target.getName(), target.getDescription(), target.getIconFile(), attributes);
        
        
    }

    @Override
    public void persistConfiguration(Map<String, Object> newConfiguration) throws InvocationException {
        Dictionary<String, Object> newConfigDictionary = new Hashtable<>();
        for(String key : newConfiguration.keySet()){
            newConfigDictionary.put(key, newConfiguration.get(key));
        }
        
        ConfigurationAdmin admin = configurationAdminTracker.getService();
        
        
        try {
            Configuration config = admin.getConfiguration(ocd.getID());
            config.update(newConfigDictionary);
        } catch (IOException | IllegalArgumentException | IllegalStateException ex) {
            throw new InvocationException("Failed to update configuration", ex);
        }
    }

    @Override
    public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
	if(properties == null){
	    logger.info("Update called but the properties dictionary was null. Returning");
	    return;
	}
	Map<String, Object> configurationToReturn = new HashMap<>();
	Enumeration<String> keys = properties.keys();
	while (keys.hasMoreElements()) {
		String key = keys.nextElement();

		if (key.equals("service.pid") || key.equals("felix.fileinstall.filename")) {
		    continue;
		}
                //D"(?<value>.+)"
                
                Object value = properties.get(key);
                if(value instanceof List){
                    List valueAsList = (List)value;
                    for(int i = 0 ; i<valueAsList.size() ; i++){
                        Object valueFromList = valueAsList.get(i);
                        if(valueFromList instanceof String){
                            valueAsList.set(i, parseStringValue((String)valueFromList));
                        }
                        
                    }
                    
                }else if(value instanceof String){
                    value = parseStringValue((String)value);
                }
                
                
                
		configurationToReturn.put(key, value);

	    }
	try {
	    //Do actual update
	    Map<String, Object> remainingConfig = getTarget().updateConfig(configurationToReturn);
	    
            if(!remainingConfig.isEmpty()){
                logger.info("Couldn't add the following configurations: "+remainingConfig);
            }	    
	} catch (ParsingException ex) {
	    try {
		resetConfiguration();
	    } catch (IOException ex1) {
		logger.error("Could not reset the last configuration", ex1);
	    }
	    throw new ConfigurationException(ex.getKey(), "Could not update configuration for "+getTarget().getName()+"["+getTarget().getID()+"]", ex);
	}
	lastAppliedProperties = properties;
    }
    
    protected Object parseStringValue(String value){
        Matcher listMatcher = listPattern.matcher(value);
        if(listMatcher.find()){
            List<Object> elements = new ArrayList<>();
            Matcher listElementPatcher = listElementPattern.matcher(value);
            while(listElementPatcher.find()){
                elements.add(parseStringValue(listElementPatcher.group()));
            }
            return elements;
        }
        
        Matcher doubleMatcher = doublePattern.matcher(value);
        if(doubleMatcher.find()){
            return Double.parseDouble(doubleMatcher.group(VALUE));
        }
        Matcher floatMatcher = floatPattern.matcher(value);
        if(floatMatcher.find()){
            return Float.parseFloat(floatMatcher.group(VALUE));
        }
        Matcher integerMatcher = integerPattern.matcher(value);
        if(integerMatcher.find()){
            return Integer.parseInt(integerMatcher.group(VALUE));
        }
        Matcher longMatcher = longPattern.matcher(value);
        if(longMatcher.find()){
            return Long.parseLong(longMatcher.group(VALUE));
        }
        Matcher shortMatcher = shortPattern.matcher(value);
        if(shortMatcher.find()){
            return Short.parseShort(shortMatcher.group(VALUE));
        }
        Matcher byteMatcher = bytePattern.matcher(value);
        if(byteMatcher.find()){
            return Byte.parseByte(byteMatcher.group(VALUE));
        }
        Matcher booleanMatcher = booleanPattern.matcher(value);
        if(booleanMatcher.find()){
            return Boolean.parseBoolean(booleanMatcher.group(VALUE));
        }
        Matcher charMatcher = charPattern.matcher(value);
        if(charMatcher.find()){
            return charMatcher.group(VALUE).charAt(0);
        }
        
        return value;
    }
    
    protected synchronized void resetConfiguration() throws IOException{
	if(lastAppliedProperties == null){
	    return;
	}
	ServiceReference<ConfigurationAdmin> adminRef = null;
	try{
	    logger.info("Resetting the configuration of "+ocd.getName()+"["+ocd.getID()+"] to "+ lastAppliedProperties);
	    adminRef = bundleContext.getServiceReference(ConfigurationAdmin.class);
	    ConfigurationAdmin admin = bundleContext.getService(adminRef);
	    admin.getConfiguration(ocd.getID()).update(lastAppliedProperties);
	}finally{
	    if(adminRef != null){
		bundleContext.ungetService(adminRef);
	    }
	}
    }

    @Override
    public void start() throws Exception {
	Class configBindingClass = getTarget().getConfigurationType();
	Hashtable<String, Object> managedServiceProps = new Hashtable<>();
	managedServiceProps.put(Constants.SERVICE_PID, ocd.getID());
	managedServiceReg = bundleContext.registerService(ManagedService.class, this, managedServiceProps);

	Hashtable<String, Object> metaTypeProps = new Hashtable<>();
	metaTypeProps.put(Constants.SERVICE_PID, ocd.getID());
	metaTypeProps.put("metadata.type", "Server");
	metaTypeProps.put("metadata.version", "1.0.0");
	metatypeServiceReg = bundleContext.registerService(MetaTypeProvider.class, this, metaTypeProps);

	Hashtable<String, Object> selfRegProps = new Hashtable<>();
	selfRegProps.put(Constants.SERVICE_PID, ocd.getID());
	selfRegProps.put(BindingID, configBindingClass.getCanonicalName());
	selfReg = bundleContext.registerService(ManagedPropertiesController.class, controller, selfRegProps);
        
        Hashtable<String, Object> proxyRegistrationProps = new Hashtable<>();
        proxyRegistrationProps.put(ConfigID, controller.getID());
        proxyRegistrationProps.put(ConfigName, controller.getName());
        proxyRegistration = bundleContext.registerService(controller.getConfigurationType(), ManagedPropertiesFactory.castToProxy(controller.getConfigurationType(), controller), proxyRegistrationProps);
        
        configurationAdminTracker = new ServiceTracker<>(bundleContext, ConfigurationAdmin.class, null);
        configurationAdminTracker.open();
	
    }

    @Override
    public void stop() throws Exception {
	selfReg.unregister();
	metatypeServiceReg.unregister();
	managedServiceReg.unregister();
        proxyRegistration.unregister();
        configurationAdminTracker.close();
    }
    
    
    private static OCD buildOCD(String id, String name, String description, String file, Collection<AttributeDefinition> attributes) throws InvalidTypeException {

	if (logger.isDebugEnabled()) {
	    logger.debug("Building ObjectClassDefinition for '" + name + "'");
	}

	File iconFile = null;
	if (!file.isEmpty()) {
	    iconFile = new File(file);
	}

	OCD newocd = new OCD(id, attributes.toArray(new MetaTypeAttributeDefinition[attributes.size()]));
	newocd.setName(name);
	newocd.setDescription(description);
	newocd.setIconFile(iconFile);
	return newocd;
    }

    @Override
    public Class getReturnType(String configID) throws UnknownValueException {
	for(MetaTypeAttributeDefinition definition : ocd.getRequiredADs()){
	    if(definition.getID().equals(configID)){
		return definition.getAttribute().getInputType();
	    }
	}
	throw new UnknownValueException("No type found in OCD for configID: "+configID);
    }
    
    
    
    
    @Override
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
	logger.info("Getting OCD for " + id + " " + locale);
	return ocd;
    }

    @Override
    public String[] getLocales() {
	return null;
    }
    
}
