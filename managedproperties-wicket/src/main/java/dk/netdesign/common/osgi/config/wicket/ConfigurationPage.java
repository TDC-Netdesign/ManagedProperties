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

import dk.netdesign.common.osgi.config.wicket.panel.ConfigurationItemPanel;
import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.ParsingException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.wicket.markup.html.WebPage;   
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public abstract class ConfigurationPage <E> extends WebPage{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationPage.class);
    
    public static final String CONFIGID = "configID";
    
    private final AttributeModel<E> attributeModel;
    private final ManagedPropertiesControllerModel controllerModel;
    
    

    public ConfigurationPage(Class configurationInterface, ConfigurationItemFactory factory) {
        controllerModel = new ManagedPropertiesControllerModel(factory, configurationInterface);
        attributeModel = new AttributeModel<>(controllerModel);
        setUpPage();
    }
    
    public ConfigurationPage(PageParameters parameters) {
        super(parameters);
        StringValue configurationIDValue = parameters.get(CONFIGID);
        
        
        controllerModel = new ManagedPropertiesControllerModel(getFactory(), configurationIDValue.toString());
        attributeModel = new AttributeModel<>(controllerModel);
        setUpPage();
    }
    
    public final void setUpPage(){
        final ListView<AttributeValue> attributePanels = new ListView<AttributeValue>("attribute-panels", attributeModel) {
            
            @Override
            protected void populateItem(ListItem<AttributeValue> item) {
                AttributeValue attributeAndValue = item.getModelObject();
                item.add(new ConfigurationItemPanel(attributeAndValue.getAttribute(), attributeAndValue.getValue(), null, "attribute-panel"));
            }
        };
        
        
        Form form = new Form("configForm"){
            @Override
            protected void onSubmit() {
                try {
                    ManagedPropertiesController controller = controllerModel.getObject();
                    
                    for(AttributeValue value : attributePanels.getList()){
                        controller.setItem(value.getAttribute(), value.getValue().getObject());
                    }
                    
                    controller.commitProperties();
                } catch (InvocationException | ParsingException ex) {
                    LOGGER.error("Could not save the configuration. ",ex);
                }
                
            }
            
        };
        
        
        form.add(attributePanels);
        
        add(form);
    }
    
    protected abstract ConfigurationItemFactory getFactory();
    
    private class AttributeModel <E> extends LoadableDetachableModel<List<AttributeValue>>{
        private final ManagedPropertiesControllerModel controllerModel;

        public AttributeModel(ManagedPropertiesControllerModel controllerModel) {
            this.controllerModel = controllerModel;
        }
        

        @Override
        protected List<AttributeValue> load() {
            ManagedPropertiesController controller = controllerModel.getObject();
            List<AttributeValue> values = new ArrayList<>();
            
            
            for(Attribute attribute : controller.getAttributes()){
                Object value = controller.getConfigItem(attribute.getID());
                
                AttributeCastingModel<Serializable> valueModel = new AttributeCastingModel<Serializable>(attribute);
                if(value != null && value instanceof Serializable){
                    valueModel.setObject((Serializable)value);
                }else{
                    LOGGER.warn("Could not retrieve value for "+attribute.getName()+"["+attribute.getID()+"]. Value was not serializable");
                }
                
                values.add(new AttributeValue(attribute, valueModel));
            }
            
            return values;
        }
        
        
        
        
    }
    
    private class ManagedPropertiesControllerModel extends LoadableDetachableModel<ManagedPropertiesController>{
        private final ConfigurationItemFactory factory;
        private final Class configurationType;
        private final String configurationID;
        
        public ManagedPropertiesControllerModel(ConfigurationItemFactory factory, Class<E> configurationType) {
            this.factory = factory;
            this.configurationType = configurationType;
            configurationID = null;
        }

        public ManagedPropertiesControllerModel(ConfigurationItemFactory factory, String configurationID) {
            this.factory = factory;
            this.configurationType = null;
            this.configurationID = configurationID;
        }

        @Override
        protected ManagedPropertiesController load() {
            Object configInstance;
            
            if(configurationType != null){
                configInstance = factory.getConfigurationItem(configurationType);
            }else{
                configInstance = factory.getConfigurationItem(configurationID);
            }
            
            ManagedPropertiesController controller = (ManagedPropertiesController)Proxy.getInvocationHandler(configInstance);
            
            return controller;
        }

       
        
        
        
        
    }
    
    protected class AttributeValue implements Serializable{
        private final Attribute attribute;
        private final AttributeCastingModel<Serializable> value;

        public AttributeValue(Attribute attribute, AttributeCastingModel<Serializable> value) {
            this.attribute = attribute;
            this.value = value;
        }

        public Attribute getAttribute() {
            return attribute;
        }

        public AttributeCastingModel<Serializable> getValue() {
            return value;
        }
        

    }
    
    protected class AttributeCastingModel<E extends Serializable> extends Model<E>{
        private final Attribute attribute;

        public AttributeCastingModel(Attribute attribute) {
            this.attribute = attribute;
        }

        public AttributeCastingModel(Attribute attribute, E object) {
            super(object);
            this.attribute = attribute;
        }
        
        public Object getCastObject() throws ParsingException{
            E currentObject = getObject();
            Class inputType = attribute.getInputType();
            if(currentObject instanceof String){

                String stringObject = (String)currentObject;
                if(inputType == Integer.class){
                    return Integer.parseInt(stringObject);
                }else if(inputType == Long.class){
                    return Long.parseLong(stringObject);
                }else if(inputType == Short.class){
                    return Short.parseShort(stringObject);
                }else if(inputType == Double.class){
                    return Double.parseDouble(stringObject);
                }else if(inputType == Float.class){
                    return Float.parseFloat(stringObject);
                }else if(inputType == Character.class){
                    return new Character(stringObject.charAt(0));
                }else if(inputType == Byte.class){
                    return Byte.parseByte(stringObject);
                }else if(inputType == Boolean.class){
                    return Boolean.parseBoolean(stringObject);
                }else if(inputType == Character[].class){
                    return stringObject.toCharArray();
                }else if(inputType == BigInteger.class){
                    return new BigInteger(stringObject, 10);
                }else if(inputType == BigDecimal.class){
                    return new BigDecimal(stringObject);
                }else if(inputType == String.class){
                    return stringObject;
                }else{
                    throw new ParsingException(attribute.getID(), "Could not parse value "+attribute.getName()+" unknown type not supported: "+attribute.getInputType());
                }
            }else if(attribute.getInputType() == Boolean.class && attribute.getInputType().isAssignableFrom(currentObject.getClass())){
                return getObject();
            }else if(attribute.getInputType() == Character[].class && attribute.getInputType().isAssignableFrom(currentObject.getClass())){
                return getObject();
            }else{
                throw new ParsingException(attribute.getID(), "Could not parse configuration. Unsupported type: "+currentObject.getClass());
            }
            
        }
        
    }
    
    
}
