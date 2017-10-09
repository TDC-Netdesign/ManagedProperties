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
package dk.netdesign.common.osgi.config.enhancement;

import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author mnn
 */
public class ConfigurationChangeSet<T> implements InvocationHandler{
    private final String WRONG_METHOD_EXCEPTION="Methods called though the configurationset must be known setters. Methods must take exactly one paramter and"
            + " must target a known property";
    private final ManagedPropertiesController configController;
    private final Class<T> configType;
    private final HashMap<String, Object> objectsToUpdate = new HashMap<>();
    

    public ConfigurationChangeSet(T configuration) {
        InvocationHandler handler = Proxy.getInvocationHandler(configuration);
        if(!(handler instanceof ManagedPropertiesController)){
            throw new RuntimeException("Could not get configuration changeset. This configuration object was a proxy, but was not a ManagedProperties contorller: "+handler.getClass().getCanonicalName());
        }
        
        configController = (ManagedPropertiesController)handler;
        configType = (Class<T>)configuration.getClass();
    }
    
    public T getConfigSet(){
        return configType.cast(Proxy.newProxyInstance(ConfigurationChangeSet.class.getClassLoader(), new Class<?>[]{configType} , this));
    }
    
    public void commitChangeSet() throws InvocationException{
        configController.getProvider().persistConfiguration(objectsToUpdate);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(args.length != 0){
            throw new InvalidTypeException(WRONG_METHOD_EXCEPTION);
        }
        
        String setterMethodName = method.getName();
        
        List<Attribute> allAttributes = configController.getAttributes();
        for(Attribute possibleAttribute : allAttributes){
            if(possibleAttribute.getSetterName().equals(setterMethodName)){
                String configID = possibleAttribute.getID();
                
                Object parsedConfig = configController.parseToConfig(configID, args[0]);
                objectsToUpdate.put(configID, parsedConfig);
                
                break;
            }
        }
        return null;
    }
   
    
    
}
