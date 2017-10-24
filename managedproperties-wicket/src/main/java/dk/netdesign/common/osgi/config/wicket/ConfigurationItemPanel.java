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
import java.io.Serializable;
import java.net.URL;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

/**
 *
 * @author mnn
 */
public class ConfigurationItemPanel extends Panel {
    private Attribute attribute;
    private final IModel<? extends Serializable> configValue;

    public ConfigurationItemPanel(final Attribute attribute, final IModel<Serializable> currentValue, final String errorMessage, String id) {
        super(id);

        this.attribute = attribute;
        
        String panelTextFieldID = attribute.getID() + "Input";
        String panelSmallLabelID = attribute.getID() + "Details";

        String attributeDefault = attribute.getDefaultValue().length > 0 && attribute.getDefaultValue()[0] != null ? attribute.getDefaultValue()[0] : "";

        String methodReturnSimpleName = attribute.getMethodReturnType().getSimpleName();

        final boolean usingDefault;

        configValue = currentValue;
        
        if(currentValue.getObject() == null || (currentValue.getObject() instanceof String && ((String)currentValue.getObject()).isEmpty())){
            currentValue.setObject(attributeDefault);
            usingDefault = true;
        }else{
            usingDefault = false;
        }
        

        Label formLabel = new Label("label", Model.of(attribute.getName()+"("+attribute.getCardinalityDef().name()+")"));
        formLabel.add(AttributeModifier.replace("for", panelTextFieldID));
        formLabel.add(AttributeModifier.replace("title", attribute.getInputType().getSimpleName()));
        add(formLabel);

        Component formInput;
        final Class attributeInputType = attribute.getInputType();
        final Class attributeMethodType = attribute.getMethodReturnType();

        if (Boolean.class.isAssignableFrom(attributeInputType)) {
            formInput = new CheckBox("input", (Model<Boolean>)configValue);
        } else {
            formInput = new TextField("input", configValue);
        }
        
        formInput.setEnabled(isKnownType());

        formInput.add(AttributeModifier.replace("id", panelTextFieldID));
        formInput.add(AttributeModifier.replace("aria-describedby", panelSmallLabelID));
        
        formInput.add(AttributeModifier.replace("placeholder", methodReturnSimpleName));
        formInput.add(AttributeModifier.replace("type", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                if (attributeInputType == Character[].class) {
                    return "password";
                } else if (Number.class.isAssignableFrom(attributeInputType)) {
                    return "number";
                } else if(URL.class.isAssignableFrom(attributeMethodType)){
                    return "url";
                }
                else{
                    return "text";
                }
            }
        }));
        if (attributeInputType == Float.class || attributeInputType == Double.class) {
            formInput.add(AttributeModifier.replace("step", new LoadableDetachableModel<String>() {
                @Override
                protected String load() {
                    return "0.0001";
                }

            }));
        }
        if(errorMessage != null){
            formInput.add(AttributeModifier.replace("title", errorMessage));
        }
        formInput.add(AttributeModifier.append("class", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                if(errorMessage != null){
                    return ".bg-danger";
                }else if(!usingDefault){
                    return ".bg-info";
                }else{
                    return ".bg-warning";
                }
            }

        }));

        add(formInput);
        
        Label smallLabel = new Label("smalllabel", attribute.getDescription());
        
        smallLabel.add(AttributeModifier.replace("id", panelSmallLabelID));
        
        add(smallLabel);

    }

    protected Serializable parseValue(Class inputType, Serializable value) {
        if (char[].class.isAssignableFrom(value.getClass()) || Character[].class.isAssignableFrom(value.getClass())) {
            return new String((char[]) value);
        } else {
            return value;
        }
    }
    
    protected boolean isKnownType(){
        Class inputType = attribute.getInputType();
        if(Number.class.isAssignableFrom(inputType)){
            return true;
        }else if(inputType.equals(String.class) || inputType.equals(Character[].class) || inputType.equals(Character.class) || inputType.equals(Boolean.class) || inputType.equals(Byte.class)){
            return true;
        }else return false;
    }

}
