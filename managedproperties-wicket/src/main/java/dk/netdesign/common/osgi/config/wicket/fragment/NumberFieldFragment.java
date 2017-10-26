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
package dk.netdesign.common.osgi.config.wicket.fragment;

import dk.netdesign.common.osgi.config.Attribute;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 *
 * @author mnn
 */
public class NumberFieldFragment extends InputFragment<Number>{

    public NumberFieldFragment(String id, String markupId, MarkupContainer markupProvider, Attribute attribute, String panelFormInputID, String panelSmallLabelID, IModel<Number> model) {
        super(id, markupId, markupProvider, attribute, panelFormInputID, panelSmallLabelID, model);
    }
    
    

    @Override
    protected Component getFormInput(String wicketID, IModel<Number> model) {
        NumberTextField field = new NumberTextField(wicketID, model);
        Class attributeInputType = getAttribute().getInputType();
        if (attributeInputType == Float.class || attributeInputType == Double.class) {
            formInput.add(AttributeModifier.replace("step", new LoadableDetachableModel<String>() {
                @Override
                protected String load() {
                    return "0.0001";
                }

            }));
        }
        
        return field;
    }
    
    
    
}
