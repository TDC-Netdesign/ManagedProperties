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
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

/**
 *
 * @author mnn
 */
public abstract class InputFragment<E> extends Fragment{
    Component formInput;
    Attribute attribute;
    
    public InputFragment(String id, String markupId, MarkupContainer markupProvider, Attribute attribute, String panelFormInputID, String panelSmallLabelID, IModel<E> model) {
        super(id, markupId, markupProvider);
        
        this.attribute = attribute;
        
        String methodReturnSimpleName = attribute.getMethodReturnType().getSimpleName();
        
        formInput = getFormInput("input", model);
        
        formInput.add(AttributeModifier.replace("id", panelFormInputID));
        formInput.add(AttributeModifier.replace("aria-describedby", panelSmallLabelID));
        
        formInput.add(AttributeModifier.replace("placeholder", methodReturnSimpleName));
     
        add(formInput);
        
    }
    
    protected abstract Component getFormInput(String wicketID, IModel<E> model);

    public Attribute getAttribute() {
        return attribute;
    }
    
    public Component getFormInput(){
        return formInput;
    }

    
    
    
}
