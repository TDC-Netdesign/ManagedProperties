/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import java.util.Dictionary;
import java.util.Map;

/**
 *
 * @author mnn
 */
public interface ConfigurationCallback {
    
    /**
     * This method is called whenever a ManagedProperties object that this callback is registered with is called. The method is singlethreaded as long
     * as the callback is only registered in one ManagedProperties object.
     * @param newProperties The new properties object that has been sent to the ManagedProperties object. This <b>can</b> be used to set the properties
     * for the service, but it is recommended to contact the ManagedProperties and get the configuration from there, as the Dictionary is not type safe.
     */
    public void configurationUpdated(Map<String, ?> newProperties);
    
}
