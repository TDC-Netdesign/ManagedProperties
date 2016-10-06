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

import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.PropertiesProvider;
import dk.netdesign.common.osgi.config.enhancement.PropertyActions;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.service.HandlerFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import java.util.logging.Level;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public class OSGiHandlerFactory implements HandlerFactory{
    private BundleContext context;
    private static final Logger logger = LoggerFactory.getLogger(OSGiHandlerFactory.class);

    public OSGiHandlerFactory(BundleContext context) {
	this.context = context;
    }
    
    
    
    
    @Override
    public <E> ManagedPropertiesController getController(Class<? super E> configurationType, E defaults) throws InvocationException, DoubleIDException, InvalidTypeException, TypeFilterException, InvalidMethodException {
	ManagedPropertiesController handler = null;
	try {
	    String configurationID = ManagedPropertiesController.getDefinitionID(configurationType);
	    String configurationName = ManagedPropertiesController.getDefinitionName(configurationType);
		for(ServiceReference<PropertiesProvider> ref : context.getServiceReferences(PropertiesProvider.class, "("+Constants.SERVICE_PID+"="+configurationID+")")){
		    if(logger.isDebugEnabled()){
			logger.debug("Found ServiceReference for Configuration: "+configurationName+"["+configurationID+"]");
		    }
		    PropertiesProvider service = context.getService(ref);
		    if(ManagedPropertiesController.class.isAssignableFrom(service.getClass())){
			  if(ref.getProperty(ConfigurationAdminProvider.BindingID).equals(configurationType.getCanonicalName())){
				handler = (ManagedPropertiesController)service;
			  }else{
				throw new DoubleIDException("Could not register the interface" + configurationType + ". This id is already in use by " + ref.getProperty(ConfigurationAdminProvider.BindingID));
			  }
		    }
		}
	  } catch (InvalidSyntaxException ex) {
		throw new IllegalStateException("Could not register this service. There was an error in the search filter when searching existing mappings.", ex);
	  } catch(IllegalStateException | NullPointerException ex){
	        logger.warn("An error occured while attempting to get the current Configuration Proxies.", ex);
	        handler = null;
	  }
	  
	  if(handler == null){
	       
		handler = getInvocationHandler(configurationType, defaults);
		ConfigurationAdminProvider provider = new ConfigurationAdminProvider(context, handler);
	    try {
		provider.start();
	    } catch (Exception ex) {
		throw new InvocationException("Could not start provider: "+provider, ex);
	    }
		handler.setProvider(provider);
		logger.info("Registered "+handler);
	  }
	  return handler;
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
    protected static <E> ManagedPropertiesController getInvocationHandler(Class<? super E> type, E defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException {
	return new ManagedPropertiesController(type, defaults);
    }
    
    
}
