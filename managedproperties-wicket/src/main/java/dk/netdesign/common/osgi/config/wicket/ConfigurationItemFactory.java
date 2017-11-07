/*
 * Copyright 2017 mnn.
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
package dk.netdesign.common.osgi.config.wicket;

import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public abstract class ConfigurationItemFactory implements Serializable{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationItemFactory.class);
    
    
    public <E> E getConfigurationItem(Class<E> configurationItemClass) {
        LOGGER.debug("Getting configuration item from "+configurationItemClass);
        if (!configurationItemClass.isInterface() || !configurationItemClass.isAnnotationPresent(PropertyDefinition.class)) {
            throw new RuntimeException("Could not create ConfigurationItemModel from " + configurationItemClass + ". Must be Interface and must be annotated with " + PropertyDefinition.class);
        }
        
        return retrieveConfigurationItem(configurationItemClass);
    }
    
    public Object getConfigurationItem(String configurationID) {
        LOGGER.debug("Getting configuration item from ID "+configurationID);
        Object configuration = retrieveConfigurationItem(configurationID);
        
        return configuration;
    }

    protected abstract <E> E retrieveConfigurationItem(Class<E> configurationItem);
    
    protected abstract Object retrieveConfigurationItem(String configurationID);

}
