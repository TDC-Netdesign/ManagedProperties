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
import org.apache.wicket.model.IModel;

/**
 *
 * @author mnn
 */
public class TextFieldFragment extends InputFragment<String>{

    public TextFieldFragment(String id, String markupId, MarkupContainer markupProvider, Attribute attribute, String panelFormInputID, String panelSmallLabelID, IModel<String> model) {
        super(id, markupId, markupProvider, attribute, panelFormInputID, panelSmallLabelID, model);
    }

    @Override
    protected Component getFormInput(String wicketID, IModel<String> model) {
        return new TextField(wicketID, model);
    }
    
    
    
}
