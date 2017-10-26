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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IModel;

/**
 *
 * @author mnn
 */
public class PasswordFragment extends InputFragment<Character[]>{

    public PasswordFragment(String id, String markupId, MarkupContainer markupProvider, Attribute attribute, String panelFormInputID, String panelSmallLabelID, IModel<Character[]> model) {
        super(id, markupId, markupProvider, attribute, panelFormInputID, panelSmallLabelID, model);
    }

    @Override
    protected Component getFormInput(String wicketID, IModel<Character[]> model) {
        PasswordTextField area = new PasswordTextField(wicketID, new CharArrayToStringModel(model));
        return area;
        
    }
    
    private class CharArrayToStringModel implements IModel<String>{
        IModel<Character[]> arrayModel;

        public CharArrayToStringModel(IModel<Character[]> array) {
            this.arrayModel = array;
        }
        
        
        
        
        @Override
        public String getObject() {
            char[] array = ArrayUtils.toPrimitive(arrayModel.getObject());
            return new String(array);
        }

        @Override
        public void setObject(String object) {
            Character[] chars = ArrayUtils.toObject(object.toCharArray());
            arrayModel.setObject(chars);
        }

        @Override
        public void detach() {
            
        }
        
        
        
    }
    
}
