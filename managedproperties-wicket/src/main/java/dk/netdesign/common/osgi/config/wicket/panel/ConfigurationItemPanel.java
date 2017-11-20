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
package dk.netdesign.common.osgi.config.wicket.panel;

import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.wicket.fragment.BooleanFieldFragment;
import dk.netdesign.common.osgi.config.wicket.fragment.InputFragment;
import dk.netdesign.common.osgi.config.wicket.fragment.NumberFieldFragment;
import dk.netdesign.common.osgi.config.wicket.fragment.PasswordFragment;
import dk.netdesign.common.osgi.config.wicket.fragment.TextFieldFragment;
import dk.netdesign.common.osgi.config.wicket.fragment.URLFieldFragment;
import dk.netdesign.common.osgi.config.wicket.fragment.UnknownTypeFragment;
import java.io.Serializable;
import java.net.URL;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
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

        String attributeDefault = attribute.getDefaultValue() != null && attribute.getDefaultValue().length > 0 && attribute.getDefaultValue()[0] != null ? attribute.getDefaultValue()[0] : null;

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

        final Class attributeInputType = attribute.getInputType();
        final Class attributeMethodType = attribute.getMethodReturnType();

        //WebMarkupContainer inputArea = new WebMarkupContainer("inputArea");
        InputFragment fragment;
        
        if(attributeInputType.equals(Number.class)){
            fragment = new NumberFieldFragment("inputArea", "numberBox", this, attribute, panelTextFieldID, panelSmallLabelID, (IModel<Number>)configValue);
        }else if(attributeInputType.equals(String.class) && attributeMethodType.equals(URL.class)){
            fragment = new URLFieldFragment("inputArea", "urlBox", this, attribute, panelTextFieldID, panelSmallLabelID, (IModel<String>)configValue);
        }else if(attributeInputType.equals(Boolean.class)){
            fragment = new BooleanFieldFragment("inputArea", "booleanBox", this, attribute, panelTextFieldID, panelSmallLabelID, (IModel<Boolean>)configValue);
        }else if(attributeInputType.equals(String.class)){
            fragment = new TextFieldFragment("inputArea", "textBox", this, attribute, panelTextFieldID, panelSmallLabelID, (IModel<String>)configValue);
        }else if(attributeInputType.equals(Character[].class)){
            fragment = new PasswordFragment("inputArea", "passwordBox", this, attribute, panelTextFieldID, panelSmallLabelID, (IModel<Character[]>)configValue);
        }else{
            fragment = new UnknownTypeFragment("inputArea", "textBox", this, attribute, panelTextFieldID, panelSmallLabelID, configValue);
        }
        
        //inputArea.add(fragment);
        add(fragment);
        
//        formInput.add(AttributeModifier.replace("type", new LoadableDetachableModel<String>() {
//            @Override
//            protected String load() {
//                if (attributeInputType == Character[].class) {
//                    return "password";
//                } else if (Number.class.isAssignableFrom(attributeInputType)) {
//                    return "number";
//                } else if(URL.class.isAssignableFrom(attributeMethodType)){
//                    return "url";
//                }
//                else{
//                    return "text";
//                }
//            }
//        }));
           
        fragment.getFormInput().add(AttributeModifier.append("style", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                if(attribute.getCardinalityDef().equals(Property.Cardinality.Required) && currentValue.getObject() == null){
                    return "background-color:#d9534f;"; //#d9534f
                }else if(usingDefault){
                    return "background-color:#f0ad4e;";//#f0ad4e
                }else{
                    return null;
                }
            }

        }));

        
        Label smallLabel = new Label("smallLabel", attribute.getDescription());
        
        smallLabel.add(AttributeModifier.replace("id", panelSmallLabelID));
        
        add(smallLabel);

    }

    
    

}
