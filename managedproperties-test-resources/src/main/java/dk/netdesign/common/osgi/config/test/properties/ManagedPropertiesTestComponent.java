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
package dk.netdesign.common.osgi.config.test.properties;

import dk.netdesign.common.osgi.config.osgi.service.ManagedPropertiesService;
import dk.netdesign.common.osgi.config.service.PropertyAccess;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author mnn
 */
@Component
public class ManagedPropertiesTestComponent implements WrapperTypes{
    private ManagedPropertiesService service;
    private WrapperTypes config;

    @Override
    public Long getLong() {
        return config.getLong();
    }

    @Override
    public Integer getInt() {
        return config.getInt();
    }

    @Override
    public Short getShort() {
        return config.getShort();
    }

    @Override
    public Double getDouble() {
        return config.getDouble();
    }

    @Override
    public Float getFloat() {
        return config.getFloat();
    }

    @Override
    public Byte getByte() {
        return config.getByte();
    }

    @Override
    public Boolean getBoolean() {
        return config.getBoolean();
    }

    @Override
    public Character getChar() {
        return config.getChar();
    }

    @Override
    public Character[] getPassword() {
        return config.getPassword();
    }
    
    
    
    
    
    @Activate
    public void Activate(BundleContext context) throws Exception{
        config = service.register(WrapperTypes.class, context);
    }
    
    @Deactivate
    public void Deactivate(BundleContext context) throws Exception{
        PropertyAccess.actions(config).unregisterProperties();
    }
            
    
    
    @Reference
    public void bindManagedPropertiesService(ManagedPropertiesService service){
        this.service = service;
        
    }
    public void unbindManagedPropertiesService(ManagedPropertiesService service){
        this.service = null;
    }
    
}
