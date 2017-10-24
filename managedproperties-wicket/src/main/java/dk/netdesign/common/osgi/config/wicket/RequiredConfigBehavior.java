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

import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.annotation.Property;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 *
 * @author mnn
 */
public class RequiredConfigBehavior extends Behavior{
    private final List<Class> configurationClasses;
    private final ConfigurationItemFactory factory;
    
    public RequiredConfigBehavior(ConfigurationItemFactory factory, Class... configurationClassesToMonitor) {
        configurationClasses = Arrays.asList(configurationClassesToMonitor);
        this.factory = factory;
    }

    @Override
    public void beforeRender(Component component) {
        for(Class configClass : configurationClasses){
            Object configurationItem = factory.getConfigurationItem(configClass);
            
            ManagedPropertiesController controller = (ManagedPropertiesController)Proxy.getInvocationHandler(configurationItem);
            
            for(Attribute attribute : controller.getAttributes()){
                if(attribute.getCardinalityDef().equals(Property.Cardinality.Required)){
                    if(controller.getConfigItem(attribute.getID()) == null){
                        PageParameters params = new PageParameters();
                        params.set(ConfigurationPage.CONFIGID, controller.getID());
                        component.setResponsePage(ConfigurationPage.class, params);
                        return;
                    }
                }
            }
        }
    }
    
    
    

    
    
}
