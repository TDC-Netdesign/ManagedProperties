/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import java.util.Dictionary;

/**
 *
 * @author mnn
 */
public interface ConfigurationCallback {
    
    public void configurationUpdated(Dictionary<String, Object> newProperties);
    
}
