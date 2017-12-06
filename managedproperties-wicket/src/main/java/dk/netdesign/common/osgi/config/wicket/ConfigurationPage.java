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
import dk.netdesign.common.osgi.config.exception.MultiParsingException;
import dk.netdesign.common.osgi.config.exception.ParsingException;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 */
public abstract class ConfigurationPage<E> extends WebPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationPage.class);

    public static final String CONFIGID = "configID";

    private final AttributeModel attributeModel;
    private final ManagedPropertiesControllerModel controllerModel;

    public ConfigurationPage(Class configurationInterface, ConfigurationItemFactory factory) {
        controllerModel = new ManagedPropertiesControllerModel(configurationInterface);
        attributeModel = new AttributeModel(controllerModel);
        setUpPage();
    }

    public ConfigurationPage(PageParameters parameters) {
        super(parameters);
        StringValue configurationIDValue = parameters.get(CONFIGID);

        controllerModel = new ManagedPropertiesControllerModel(configurationIDValue.toString());
        attributeModel = new AttributeModel(controllerModel);
        setUpPage();
    }

    public final void setUpPage() {
        String name = controllerModel.getObject().getName();
        String id = controllerModel.getObject().getID();
        Label configName = new Label("configName", Model.of(name));
        add(configName);
        Label configID = new Label("configID", Model.of(id));
        add(configID);

        final ListView<AttributeValue> attributePanels = new ListView<AttributeValue>("attribute-panels", attributeModel) {

            @Override
            protected void populateItem(ListItem<AttributeValue> item) {
                AttributeValue attributeAndValue = item.getModelObject();
                item.add(new ConfigurationItemPanel(attributeAndValue.getAttribute(), attributeAndValue.getValue(), attributeAndValue.errorMessageModel, "attribute-panel"));
            }
        };

        Form configForm = new Form("configForm");

        configForm.add(new AjaxFormSubmitBehavior("onsubmit") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);

                LOGGER.info("Attempting to persist new configuration");
                for (AttributeValue value : attributeModel.getObject()) {
                    value.setErrorMessage(null);
                }

                Map<String, ParsingException> exceptions = new HashMap<>();
                for (AttributeValue value : attributeModel.getObject()) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Parsing " + value.getValue().getObject());
                        }
                        Object castObject = value.getValue().getCastObject();
                        LOGGER.debug("Parsed object: " + castObject);
                        controllerModel.getObject().setItem(value.getAttribute(), castObject);
                    } catch (ParsingException ex) {
                        ParsingException previousException = exceptions.put(ex.getKey(), ex);
                        if (previousException != null) {
                            LOGGER.info("Exception overwritten for key " + ex.getKey() + ": " + previousException.getMessage());
                        }
                    }

                }

                if (!exceptions.isEmpty()) {

                    resetCommit(controllerModel.getObject());
                    for (AttributeValue value : attributeModel.getObject()) {
                        ParsingException ex = exceptions.get(value.attribute.getID());
                        if (ex != null) {
                            value.setErrorMessage(ex.getMessage());
                        }
                    }

                } else {
                    try {
                        controllerModel.getObject().commitProperties();
                    } catch (MultiParsingException ex) {
                        for(ParsingException pex : ex.getExceptions()){
                            for(AttributeValue value : attributeModel.getObject()){
                                if(value.attribute.getID().equals(pex.getKey())){
                                    value.setErrorMessage(pex.getMessage());
                                    break;
                                }
                            }
                        }
                    } catch (InvocationException ex) {
                        LOGGER.warn("Attempted to commit configuration, but controller was not in set-state", ex);
                    }
                }
                LOGGER.debug("Committing configuration: " + controllerModel.getObject());

                target.add(ConfigurationPage.this);                
            }
        });

        configForm.add(attributePanels);

        add(configForm);
    }

    private void resetCommit(ManagedPropertiesController controller) {
        try {
            controller.abortCommitProperties();
        } catch (InvocationException ex) {
            LOGGER.warn("Could not abort setting configuration. The controller was not locked in set-state", ex);
        }

    }

    protected abstract ConfigurationItemFactory getFactory();

    private class AttributeModel extends ListModel<AttributeValue> {

        private final ManagedPropertiesControllerModel controllerModel;

        public AttributeModel(ManagedPropertiesControllerModel controllerModel) {
            this.controllerModel = controllerModel;
        }

        @Override
        public List<AttributeValue> getObject() {
            List<AttributeValue> values = super.getObject();
            if (values == null) {
                values = retrieve();
                super.setObject(values);
            }
            return values;
        }

        protected List<AttributeValue> retrieve() {
            List<AttributeValue> values = new ArrayList<>();

            for (Attribute attribute : controllerModel.getObject().getAttributes()) {
                Object value = controllerModel.getObject().getConfigItem(attribute.getID());

                AttributeCastingModel<Serializable> valueModel = new AttributeCastingModel<>(attribute);
                if (value != null && value instanceof Serializable) {
                    valueModel.setObject((Serializable) value);
                } else {
                    LOGGER.warn("Could not retrieve value for " + attribute.getName() + "[" + attribute.getID() + "]. Value was not serializable: " + value);
                }

                values.add(new AttributeValue(attribute, valueModel));
            }

            Collections.sort(values);

            return values;
        }

    }

    private class ManagedPropertiesControllerModel extends LoadableDetachableModel<ManagedPropertiesController> {

        private final Class configurationType;
        private final String configurationID;

        public ManagedPropertiesControllerModel(Class<E> configurationType) {
            this.configurationType = configurationType;
            configurationID = null;
        }

        public ManagedPropertiesControllerModel(String configurationID) {
            this.configurationType = null;
            this.configurationID = configurationID;
        }

        @Override
        protected ManagedPropertiesController load() {
            ConfigurationItemFactory factory = getFactory();
            LOGGER.debug("Loading ManagedPropertiesController for "
                    + (configurationType != null ? configurationType : "") + " " + (configurationID != null ? configurationID : "")
                    + " using " + factory);
            Object configInstance;

            if (configurationType != null) {
                configInstance = factory.getConfigurationItem(configurationType);
            } else {
                configInstance = factory.getConfigurationItem(configurationID);
            }

            ManagedPropertiesController controller = (ManagedPropertiesController) Proxy.getInvocationHandler(configInstance);

            return controller;
        }

    }

    protected class AttributeValue implements Serializable, Comparable<AttributeValue> {

        private final Attribute attribute;
        private final AttributeCastingModel<Serializable> value;
        private final IModel<String> errorMessageModel = new Model<>();

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
        
        public void setErrorMessage(String errorMessage){
            errorMessageModel.setObject(errorMessage);
        }
        
        public String getErrorMessage(){
            return errorMessageModel.getObject();
        }
        
        public IModel<String> getErrorMessageModel(){
            return errorMessageModel;
        }
        
        

        @Override
        public int compareTo(AttributeValue o) {
            return attribute.getName().toUpperCase().compareTo(o.getAttribute().getName().toUpperCase());
        }

    }

    protected class AttributeCastingModel<E extends Serializable> extends Model<E> {

        private final Attribute attribute;
        private String ID = UUID.randomUUID().toString();

        public AttributeCastingModel(Attribute attribute) {
            this.attribute = attribute;
        }

        public AttributeCastingModel(Attribute attribute, E object) {
            super(object);
            this.attribute = attribute;
        }

        @Override
        public void setObject(E object) {
            LOGGER.debug("Setting configuration for " + attribute.getID() + ": " + object);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Before: " + getObject());
            }

            super.setObject(object);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("After: " + getObject());
            }
        }

        public Object getCastObject() throws ParsingException {
            E currentObject = getObject();
            if (currentObject == null) {
                return null;
            }
            Class inputType = attribute.getInputType();
            if (currentObject instanceof String) {

                String stringObject = (String) currentObject;
                if (inputType == Integer.class) {
                    return Integer.parseInt(stringObject);
                } else if (inputType == Long.class) {
                    return Long.parseLong(stringObject);
                } else if (inputType == Short.class) {
                    return Short.parseShort(stringObject);
                } else if (inputType == Double.class) {
                    return Double.parseDouble(stringObject);
                } else if (inputType == Float.class) {
                    return Float.parseFloat(stringObject);
                } else if (inputType == Character.class) {
                    return new Character(stringObject.charAt(0));
                } else if (inputType == Byte.class) {
                    return Byte.parseByte(stringObject);
                } else if (inputType == Boolean.class) {
                    return Boolean.parseBoolean(stringObject);
                } else if (inputType == Character[].class) {
                    return stringObject.toCharArray();
                } else if (inputType == BigInteger.class) {
                    return new BigInteger(stringObject, 10);
                } else if (inputType == BigDecimal.class) {
                    return new BigDecimal(stringObject);
                } else if (inputType == String.class) {
                    return stringObject;
                } else {
                    throw new ParsingException(attribute.getID(), "Could not parse value " + attribute.getName() + " unknown type not supported: " + attribute.getInputType());
                }
            } else if (attribute.getInputType() == Boolean.class && attribute.getInputType().isAssignableFrom(currentObject.getClass())) {
                return getObject();
            } else if (attribute.getInputType() == Character[].class && attribute.getInputType().isAssignableFrom(currentObject.getClass())) {
                return getObject();
            } else {
                throw new ParsingException(attribute.getID(), "Could not parse configuration. Unsupported type: " + currentObject.getClass());
            }

        }

    }

}
