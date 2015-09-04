/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.enhancement;

import java.util.concurrent.locks.Lock;

/**
 *
 * @author mnn
 */
public interface EnhancedProperty {

    public Lock lockPropertiesUpdate();

    public void unregisterProperties();
    
    @Override
    public String toString();

}
