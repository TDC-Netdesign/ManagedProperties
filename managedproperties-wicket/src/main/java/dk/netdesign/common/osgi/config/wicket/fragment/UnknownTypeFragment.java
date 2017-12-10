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
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 *
 * @author mnn
 */
public class UnknownTypeFragment<E extends Object> extends InputFragment<E>{

    public UnknownTypeFragment(String id, String markupId, MarkupContainer markupProvider, Attribute attribute, String panelFormInputID, String panelSmallLabelID, IModel<E> model) {
        super(id, markupId, markupProvider, attribute, panelFormInputID, panelSmallLabelID, model);
    }

    @Override
    protected Component getFormInput(String wicketID, IModel<E> model) {
        TextField field = new TextField(wicketID, new toStringModel(model));
        field.setEnabled(false);
        return field;
    }
    
    
    private class toStringModel extends AbstractReadOnlyModel<String>{
        private final IModel<E> inputModel;

        public toStringModel(IModel<E> inputModel) {
            this.inputModel = inputModel;
        }

        @Override
        public String getObject() {
            Object currentValue = inputModel.getObject();
            if(currentValue != null){
                return currentValue.toString();
            }else{
                return "";
            }
        }
        
        
        
    }
    
    
    
}
