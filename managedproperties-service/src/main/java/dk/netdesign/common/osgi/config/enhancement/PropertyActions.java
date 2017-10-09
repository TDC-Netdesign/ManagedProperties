/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.enhancement;

import java.util.concurrent.locks.Lock;

/**
 * This is a convenience interface, which defines a number of methods which can be called upon the proxy returned from 
 * {@link dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory#register(Class) register}. The proxy is created with this interface, and can be cast 
 * if necessary.
 * Implementations of this interface can lock the set so it will not receive updates, and unregister the ConfigurationProvider.
 * @author mnn
 */
public interface PropertyActions {

    /**
     * When this method is called, the backing @see dk.netdesign.common.osgi.config.ManagedPropertiesController object is locked for further updates.
     * This means that the configuration will not be updated, as long as the lock is held.
     * This is useful for context-sensitive properties, like username-password combinations.
     * Remember to release the lock again, and preferably as quickly as possible, as you may be blocking a ConfigAdmin thread.
     * @return The lock to release when the properties can update again.
     */
    public Lock lockPropertiesUpdate();

    /**
     * Calling this method will unregister the properties object so it will no longer receive new updated configurations. Only call this method as cleanup,
     * as it will effectively kill the object. It cannot recover.
     * @throws java.lang.Exception Throw an exception if unregistering the backing handler fails
     */
    public void unregisterProperties() throws Exception;
    
}
