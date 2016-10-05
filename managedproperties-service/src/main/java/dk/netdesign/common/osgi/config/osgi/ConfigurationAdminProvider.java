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

import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.ConfigurationTarget;
import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.ManagedPropertiesProvider;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.enhancement.PropertyActions;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import java.io.File;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
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
    private ServiceRegistration<ManagedService> managedServiceReg;
    private ServiceRegistration<MetaTypeProvider> metatypeServiceReg;
    private ServiceRegistration<PropertyActions> selfReg;

    public ConfigurationAdminProvider(BundleContext bundleContext, ConfigurationTarget target) {
	super(target);
	this.bundleContext = bundleContext;
	Class type = getTarget().getConfigurationType();
	PropertyDefinition typeDefinition = ManagedPropertiesController.getDefinitionAnnotation(type);
	ocd = buildOCD(ManagedPropertiesController.getDefinitionID(type), ManagedPropertiesController.getDefinitionName(type), typeDefinition.description(), typeDefinition.iconFile(), attributeToMethodMapping.values());
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
	//Do actual update
	
	//Add remaining config to context properties as managedservice requests
    }

    @Override
    public void start() throws Exception {
	register(bundleContext, getTarget().getConfigurationType());
    }

    @Override
    public void stop() throws Exception {
	throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
        /**
     * Registers this ManagedProperties object with the bundle context. This is done when the proxy is first created.
     *
     * @param context The context in which to register this ManagedProperties object.
     * @param configBindingClass The interface which was bound to this ManagedProperties
     */
    public void register(BundleContext context, Class configBindingClass) {
	Hashtable<String, Object> managedServiceProps = new Hashtable<>();
	managedServiceProps.put(Constants.SERVICE_PID, ocd.getID());
	managedServiceReg = context.registerService(ManagedService.class, this, managedServiceProps);

	Hashtable<String, Object> metaTypeProps = new Hashtable<>();
	metaTypeProps.put(Constants.SERVICE_PID, ocd.getID());
	metaTypeProps.put("metadata.type", "Server");
	metaTypeProps.put("metadata.version", "1.0.0");
	metatypeServiceReg = context.registerService(MetaTypeProvider.class, this, metaTypeProps);

	Hashtable<String, Object> selfRegProps = new Hashtable<>();
	selfRegProps.put(Constants.SERVICE_PID, ocd.getID());
	selfRegProps.put(BindingID, configBindingClass.getCanonicalName());
	selfReg = context.registerService(PropertyActions.class, this, selfRegProps);
    }

    private static ObjectClassDefinition buildOCD(String id, String name, String description, String file, Collection<Attribute> attributes) throws InvalidTypeException {

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
    public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
	logger.info("Getting OCD for " + id + " " + locale);
	return ocd;
    }

    @Override
    public String[] getLocales() {
	return null;
    }
    
}
