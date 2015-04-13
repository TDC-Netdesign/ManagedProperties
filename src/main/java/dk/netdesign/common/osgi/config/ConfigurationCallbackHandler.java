/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import java.util.List;

/**
 *
 * @author mnn
 */
public interface ConfigurationCallbackHandler {
    
    public void addConfigurationCallback(ConfigurationCallback callback);
    public void removeConfigurationCallback(ConfigurationCallback callback);
    public List<ConfigurationCallback> getConfigurationCallbacks();
    
}
