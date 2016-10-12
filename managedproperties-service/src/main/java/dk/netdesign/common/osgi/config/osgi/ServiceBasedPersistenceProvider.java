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
import dk.netdesign.common.osgi.config.exception.ControllerPersistenceException;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.service.ControllerPersistenceProvider;
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
public class ServiceBasedPersistenceProvider implements ControllerPersistenceProvider {

    Logger logger = LoggerFactory.getLogger(ServiceBasedPersistenceProvider.class);
    private final BundleContext context;

    public ServiceBasedPersistenceProvider(BundleContext context) {
	this.context = context;
    }

    @Override
    public ManagedPropertiesController getController(Class typeToRetrieve) throws ControllerPersistenceException, InvalidTypeException {
	ManagedPropertiesController controller = null;
	try {
	    String configurationID = ManagedPropertiesController.getDefinitionID(typeToRetrieve);
	    String configurationName = ManagedPropertiesController.getDefinitionName(typeToRetrieve);
	    for (ServiceReference<ManagedPropertiesController> ref : context.getServiceReferences(ManagedPropertiesController.class, "(" + Constants.SERVICE_PID + "=" + configurationID + ")")) {
		//The loop is inside the try/catch here. There should never be more than one service, there will be either 0 or 1.
		if (logger.isDebugEnabled()) {
		    logger.debug("Found ServiceReference for Configuration: " + configurationName + "[" + configurationID + "]");
		}
		controller = context.getService(ref);
		if (!ref.getProperty(ConfigurationAdminProvider.BindingID).equals(typeToRetrieve.getCanonicalName())) {
		    throw new ControllerPersistenceException("Could not register the interface " + typeToRetrieve + ". This id is already in use by " + ref.getProperty(ConfigurationAdminProvider.BindingID));
		}

	    }
	} catch (InvalidSyntaxException ex) {
	    throw new IllegalStateException("Could not register this service. There was an error in the search filter when searching existing mappings.", ex);
	} catch (IllegalStateException | NullPointerException ex) {
	    logger.warn("An error occured while attempting to get the current Configuration Proxies.", ex);
	    controller = null;
	}
	return controller;
    }

    @Override
    public void persistController(Class type, ManagedPropertiesController controller) {

    }

}
