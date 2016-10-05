/*
 * Copyright 2016 mnn.
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

package dk.netdesign.common.osgi.config.osgi;

import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.osgi.service.metatype.AttributeDefinition;

/**
 *
 * @author mnn
 */
public class MetaTypeAttributeDefinition implements AttributeDefinition{
    private final Attribute attribute; 
    private int inputTypeAsInt;
     private int cardinality;

    public MetaTypeAttributeDefinition(Attribute attribute) throws InvalidTypeException {
	this.attribute = attribute;
	cardinality = getCardinality(attribute.getCardinalityDef());
	inputTypeAsInt = getAttributeType(attribute.getInputType());

    }
   
     

    
    
    private static Integer getAttributeType(Class methodReturnType) throws InvalidTypeException {
	Integer attributeType = null;
	if (methodReturnType.equals(String.class)) {
	    attributeType = AttributeDefinition.STRING;
	} else if (methodReturnType.equals(Long.class)) {
	    attributeType = AttributeDefinition.LONG;
	} else if (methodReturnType.equals(Integer.class)) {
	    attributeType = AttributeDefinition.INTEGER;
	} else if (methodReturnType.equals(Short.class)) {
	    attributeType = AttributeDefinition.SHORT;
	} else if (methodReturnType.equals(Character.class)) {
	    attributeType = AttributeDefinition.CHARACTER;
	} else if (methodReturnType.equals(Byte.class)) {
	    attributeType = AttributeDefinition.BYTE;
	} else if (methodReturnType.equals(Double.class)) {
	    attributeType = AttributeDefinition.DOUBLE;
	} else if (methodReturnType.equals(Float.class)) {
	    attributeType = AttributeDefinition.FLOAT;
	} else if (methodReturnType.equals(BigInteger.class)) {
	    attributeType = AttributeDefinition.BIGINTEGER;
	} else if (methodReturnType.equals(BigDecimal.class)) {
	    attributeType = AttributeDefinition.BIGDECIMAL;
	} else if (methodReturnType.equals(Boolean.class)) {
	    attributeType = AttributeDefinition.BOOLEAN;
	} else if (methodReturnType.equals(Character[].class)) {
	    attributeType = AttributeDefinition.PASSWORD;
	}
	if (attributeType == null) {
	    throw new InvalidTypeException("Could not create Attribute definition. The type " + methodReturnType + " is not a valid type for an InputType");
	}
	return attributeType;
    }

    private static int getCardinality(Property.Cardinality cardinality) {
	int adCardinality = 0;
	switch (cardinality) {
	    case Required:
		adCardinality = 0;
		break;
	    case Optional:
		adCardinality = -1;
		break;
	    case List:
		adCardinality = Integer.MIN_VALUE;
		break;
	}
	return adCardinality;
    }

    @Override
    public String getName() {
	return attribute.getName();
    }

    @Override
    public String getID() {
	return attribute.getID();
    }

    @Override
    public String getDescription() {
	return attribute.getDescription();
    }

    @Override
    public String[] getOptionValues() {
	return attribute.getOptionValues();
    }

    @Override
    public String[] getOptionLabels() {
	return attribute.getOptionLabels();
    }

    @Override
    public String[] getDefaultValue() {
	return attribute.getDefaultValue();
    }
    
    
    
    /**
     * The cardinality of this Method
     * @return The cardinality of the configuration item defined by this method, returned as MetaType information
     */
    @Override
    public int getCardinality() {
	return cardinality;
    }

    /**
     * The type for this method
     * @return The type for this configuration item, defined as MetaType provider data.
     */
    @Override
    public int getType() {
	return inputTypeAsInt;
    }

    @Override
    public String validate(String arg0) {
	return null;
    }
    
}
