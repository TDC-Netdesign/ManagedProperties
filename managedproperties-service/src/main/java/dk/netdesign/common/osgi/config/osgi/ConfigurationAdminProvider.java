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
import dk.netdesign.common.osgi.config.exception.ParsingException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
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
public class ConfigurationAdminProvider extends ManagedPropertiesProvider implements MetaTypeProvider, ManagedService{
    public static final String BindingID = "ManagedPropertiesBinding";
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationAdminProvider.class);
    private final BundleContext bundleContext;
    private final OCD ocd;
    private final ManagedPropertiesController controller;
    
    private ServiceRegistration<ManagedService> managedServiceReg;
    private ServiceRegistration<MetaTypeProvider> metatypeServiceReg;
    private ServiceRegistration<ManagedPropertiesController> selfReg;

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
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
	Map<String, Object> configurationToReturn = new HashMap<>();
	Enumeration<String> keys = properties.keys();
	while (keys.hasMoreElements()) {
		String key = keys.nextElement();

		if (key.equals("service.pid") || key.equals("felix.fileinstall.filename")) {
		    continue;
		}
		configurationToReturn.put(key, properties.get(key));

	    }
	try {
	    //Do actual update
	    Map<String, Object> remainingConfig = getTarget().updateConfig(configurationToReturn);
	    //Add remaining config to context properties as managedservice requests
	} catch (ParsingException ex) {
	    throw new ConfigurationException(ex.getKey(), "Could not update configuration for "+getTarget().getName()+"["+getTarget().getID()+"]", ex);
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

    }

    @Override
    public void stop() throws Exception {
	selfReg.unregister();
	metatypeServiceReg.unregister();
	managedServiceReg.unregister();
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
		return String.class; //The configadmin/felix file install only supports String
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
