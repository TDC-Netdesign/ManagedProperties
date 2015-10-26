/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.enhancement;

import dk.netdesign.common.osgi.config.ConfigurationCallback;
import java.util.List;

/**
 * This is a convenience interface, which defines a number of methods which can be called upon the proxy returned from 
 * {@link dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory#register(Class, BundleContext) register}. The proxy is created with this interface, and can be cast 
 * if necessary.
 * Implementations of this interface can register a number of {@link ConfigurationCallback} that will be executed when new configurations are loaded.
 *
 * @author mnn
 */
public interface ConfigurationCallbackHandler {

    /**
     * Adds a @see ConfigurationCallback
     *
     * @param callback The callback to add
     */
    public void addConfigurationCallback(ConfigurationCallback callback);

    /**
     * Removes a @see ConfigurationCallback that equals (==) the provided @see ConfigurationCallback.
     *
     * @param callback The callback to remove
     */
    public void removeConfigurationCallback(ConfigurationCallback callback);

    /**
     * Returns a list of @see ConfigurationCallback registered to this handler. The list is a clone of the internal list, but contains the original elements.
     *
     * @return A cloned list of @see ConfigurationCallback
     */
    public List<ConfigurationCallback> getConfigurationCallbacks();

}
