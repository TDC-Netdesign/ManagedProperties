/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config.test.consumer;

import dk.netdesign.common.osgi.config.enhancement.PropertyActions;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.osgi.service.ManagedPropertiesService;
import dk.netdesign.common.osgi.config.osgi.ManagedPropertiesServiceComponent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public class Consumer implements BundleActivator{
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private InheritedProperties props;
    private Thread printer;
    private boolean run = true;
    private ServiceReference<ManagedPropertiesService> factoryReg;
    
    @Override
    public void start(BundleContext context) throws Exception {
	ManagedPropertiesService factory;
	factoryReg = context.getServiceReference(ManagedPropertiesService.class);
	
	factory = context.getService(factoryReg);
	
	if(factory == null){
	    logger.error("Could not get ManagedPropertiesService");
	}
	
	props = factory.register(InheritedProperties.class, new DefaultProperties(), context);
	logger.info("Getting properties");
	logger.info(props.getCharacterProperty()+"");
	logger.info(props.getDoubleProperty()+"");
	logger.info(props.getStringInteger()+"");
	logger.info(props.getStringProperty()+"");
	logger.info(props.getStringListProperty()+"");
	logger.info(props.getSubString());
	logger.info(props.getFileListProperty()+"");
	logger.info(props+"");	
	printer = new PrinterThread();
	printer.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
	((PropertyActions)props).unregisterProperties();
	context.ungetService(factoryReg);
	run = false;
	
    }
    
    private class PrinterThread extends Thread{

	public PrinterThread() {
	    setName("TestConsumer PrinterThread");
	}

	@Override
	public void run() {
	    while(run){
		logger.info(props.toString());
		try {
		    Thread.sleep(15000);
		} catch (InterruptedException ex) {
		    logger.warn("Consumer interrupted", ex);
		    run = false;
		}
	    }
	    
	    
	}
	
	
	
	
	
    }
    
    
    
}
