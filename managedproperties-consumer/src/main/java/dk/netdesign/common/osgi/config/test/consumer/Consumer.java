/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config.test.consumer;

import dk.netdesign.common.osgi.config.enhancement.EnhancedProperty;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public class Consumer implements BundleActivator{
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private static ServiceTracker<ManagedPropertiesService, ManagedPropertiesService> tracker;
    PropertiesWithStandardTypes props;

    @Override
    public void start(BundleContext context) throws Exception {
	ServiceReference<ManagedPropertiesService> ref = context.getServiceReference(ManagedPropertiesService.class);
	logger.info(ref != null ? ref.toString() : "ref was null");
	
	logger.info("Getting tracker");
	tracker = new ServiceTracker(context, ManagedPropertiesService.class, null);
        tracker.open();
	logger.info("Tracker open");
	ManagedPropertiesService service = tracker.getService();
	
	props = service.register(PropertiesWithStandardTypes.class);
	logger.info("Getting properties");
	System.out.println(props.getCharacterProperty());
	System.out.println(props.getDoubleProperty());
	System.out.println(props.getStringInteger());
	System.out.println(props.getStringProperty());
	System.out.println(props.getStringListProperty());
	System.out.println(props);	
    }

    @Override
    public void stop(BundleContext context) throws Exception {
	tracker.close();
	((EnhancedProperty)props).unregisterProperties();
	
    }
    
    
    
}
