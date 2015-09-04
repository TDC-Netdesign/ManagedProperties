/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.service.ManagedPropertiesService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public class ManagedPropertiesActivator implements BundleActivator{
    private static final Logger logger = LoggerFactory.getLogger(ManagedPropertiesActivator.class);
    private ServiceRegistration<ManagedPropertiesService> registration = null;
    private ManagedPropertiesBroker broker;
    
    @Override
    public void start(BundleContext context) throws Exception {
	broker = new ManagedPropertiesBroker(context);
	logger.info("Starting ManagedProperties");
	registration = context.registerService(ManagedPropertiesService.class, broker, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
	logger.info("Stopping ManagedProperties");
	registration.unregister();
	broker = null;
    }
    
}
