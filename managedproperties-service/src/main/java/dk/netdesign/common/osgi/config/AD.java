/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.service.TypeFilter;
import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.osgi.service.metatype.AttributeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implemnetation of AttributeDefinition that has the extra responsibility of tracking the settings for each of the annotated configuration methods.
 * It is the applications one-stop-shop for all information about the parsed Methods, containing both what is sent as MetaDataProvider and what is registered
 * by the ManagedProperties itself.
 * This class is what binds a method to a configuration item.
 * @author mnn
 */
public class AD implements AttributeDefinition {

    private static final Logger logger = LoggerFactory.getLogger(AD.class);

    private String id;
    private int type;
    private int cardinality;
    private String name;
    private String description;
    private String[] defValue;
    private String[] optionalLabels;
    private String[] optionalValues;
    private Class inputType;
    private Class<? extends TypeFilter> filter;
    Property.Cardinality cardinalityDef;

    /**
     * Default Constructor. This is the only non-deprecated constructor. It will create an AD from a method.
     * The constructor is an all-in-one operation; the setter methods available should not need to be used. During the construction of the AD
     * a fair amount of validation is going on. The constructor will fail if called on a method which is not annotated with @Property.
     * In general, if there are any restrictions or missing data in the @Property annotation, or the method itself, it will be caught when
     * creating the AD.
     * @param method The @Property annotated method to create an AD for.
     * @throws TypeFilterException Thrown if there is a problem with the filter defined for this method
     * @throws InvalidTypeException Thrown if there is a problem with the type of the input or output of the method, combined with the @Property
     * @throws InvalidMethodException Thrown if there is a problem with anything but the type, or filter. This could be missing information, invalid combinations,
     * or a missing @Property annotation.
     */
    protected AD(Method method) throws TypeFilterException, InvalidTypeException, InvalidMethodException {
	Property methodProperty = ManagedPropertiesFactory.getMethodAnnotation(method);;
	name = getAttributeName(method);
	id = name;

	cardinalityDef = methodProperty.cardinality();
	inputType = getMethodReturnType(method);
	type = getAttributeType(inputType);
	if (logger.isTraceEnabled()) {
	    logger.trace("Found @Property on " + method.getName() + "[" + inputType + "]");
	}

	if (!methodProperty.id().isEmpty()) {
	    id = methodProperty.id();
	}

	cardinality = getCardinality(cardinalityDef);
	
	Class<? extends TypeFilter> filterFromAnnotation = methodProperty.typeMapper();

	if(cardinalityDef.equals(cardinalityDef.List)){
	    if(!List.class.isAssignableFrom(method.getReturnType())){
		throw new InvalidMethodException("Could not create handler for method "+method.getName()+". Methods with list cardinality must return a list");
	    }
	    if(filterFromAnnotation != TypeFilter.class){
		throw new InvalidMethodException("Could not create handler for method "+method+". Cannot use filters with lists");
	    }
	}
	
	
	if (filterFromAnnotation == TypeFilter.class) {
	    filter = null;
	    if (!List.class.isAssignableFrom(method.getReturnType()) && !method.getReturnType().isAssignableFrom(inputType)) {
		throw new InvalidTypeException("Could not create method definition. The returntype of the method '" + method.getReturnType() + "' is not compatible with the returntype of the Property '" + inputType + "'.");
	    }
	} else {
	    filter = filterFromAnnotation;
	    Method parseMethod;
	    try {
		parseMethod = filter.getDeclaredMethod("parse", new Class[]{inputType});
	    } catch (NoSuchMethodException | SecurityException ex) {
		throw new TypeFilterException("Could not add filter. An exception occured while trying to examine the parse method", ex);
	    }
	    if (!method.getReturnType().isAssignableFrom(parseMethod.getReturnType())) {
		throw new TypeFilterException("Could not use Filter type " + filter.getName() + " with the method " + method.getName()
			+ " The returntype of the method " + method.getReturnType() + "must be assignable to the return of the filter parse method" + parseMethod.getReturnType());
	    }
	    if (method.getReturnType().isInterface() || Modifier.isAbstract(method.getReturnType().getModifiers())) {
		throw new TypeFilterException("Could not instantiate filter for " + method + ". Filter does not return an instantiable value.");
	    }
	}
	if (logger.isTraceEnabled()) {
	    logger.trace("Building AttributeDefinition with attributeID '" + id + "' attributeType '" + type + "' cardinality '" + cardinality + "'");
	}
	defValue = methodProperty.defaultValue();
	description = methodProperty.description();
	optionalLabels = methodProperty.optionLabels();
	optionalValues = methodProperty.optionValues();
    }

    @Deprecated
    protected AD(String id, int type, int cardinality) {
	this.id = id;
	this.type = type;
	this.cardinality = cardinality;
    }

    private static String getAttributeName(Method classMethod) {
	String attributeName = classMethod.getName().replaceAll("^get", "");
	if (classMethod.isAnnotationPresent(Property.class)) {
	    Property methodProperty = classMethod.getAnnotation(Property.class);
	    if (!methodProperty.name().isEmpty()) {
		attributeName = methodProperty.name();
	    }
	}
	return attributeName;
    }

    private static Class getMethodReturnType(Method classMethod) throws InvalidMethodException {
	Class methodReturnType = classMethod.getReturnType();
	if (classMethod.isAnnotationPresent(Property.class)) {
	    Property methodProperty = classMethod.getAnnotation(Property.class);
	    if (methodProperty.type() != void.class) {
		methodReturnType = methodProperty.type();
	    }else if(List.class.isAssignableFrom(methodReturnType)){
		throw new InvalidMethodException("Could not create handler for method "+classMethod.getName()+". Lists must be accompanied by a returnType");
	    }
	}
	return methodReturnType;
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

    /**
     * The cardinality of this Method
     * @return The cardinality of the configuration item defined by this method, returned as MetaType information
     */
    @Override
    public int getCardinality() {
	return cardinality;
    }

    /**
     * The default value of this method
     * @return The possible default values for this configuration item defined by this method, returned as MetaType information
     */
    @Override
    public String[] getDefaultValue() {
	return defValue;
    }

    /**
     * The description of this method
     * @return The description for the configuration item defined by this method
     */
    @Override
    public String getDescription() {
	return description;
    }

    /**
     * The configurationID for this method
     * @return The ID for the configuration item.
     */
    @Override
    public String getID() {
	return id;
    }

    /**
     * The display name for this method
     * @return The name for the configuration item
     */
    @Override
    public String getName() {
	return name;
    }

    /**
     * The option labels for this method
     * @return The Option labels for the configuration item
     */
    @Override
    public String[] getOptionLabels() {
	return optionalLabels;
    }

    /**
     * The option values for this method
     * @return The option values for this configuration item
     */
    @Override
    public String[] getOptionValues() {
	return optionalValues;
    }

    /**
     * The type for this method
     * @return The type for this configuration item, defined as MetaType provider data.
     */
    @Override
    public int getType() {
	return type;
    }

    @Override
    public String validate(String arg0) {
	return null;
    }

    protected void setOptionLabels(String[] labels) {
	optionalLabels = labels;
    }

    protected void setOptionValues(String[] values) {
	optionalValues = values;
    }

    protected void setDescription(String description) {
	this.description = description;
    }

    protected void setName(String name) {
	this.name = name;
    }

    protected void setDefaultValue(String[] defValue) {
	this.defValue = defValue;
    }

    /**
     * Return the inputType resulting from parsing the @Property and method return types.
     * @return The return type of the method, or the override in the @Property annotation
     */
    public Class getInputType() {
	return inputType;
    }

    public void setInputType(Class inputType) {
	this.inputType = inputType;
    }

    /**
     * The filter defined for this method, if any
     * @return Returns the filter defined for this method, or null if no filter is defined.
     */
    public Class<? extends TypeFilter> getFilter() {
	return filter;
    }

    public void setFilter(Class<? extends TypeFilter> filter) {
	this.filter = filter;
    }

    /**
     * Returns the @Property defined cardinality. This is parsed into the cardinality required by the MetaTypeProvider
     * @return The cardinality of the method
     */
    public Property.Cardinality getCardinalityDef() {
	return cardinalityDef;
    }

    public void setCardinalityDef(Property.Cardinality cardinalityDef) {
	this.cardinalityDef = cardinalityDef;
    }

    @Override
    public String toString() {
	ToStringBuilder builder = new ToStringBuilder(this);
	builder.append("id", id).append("name", name).append("type", type).append("cardinality", cardinality).append("description", description)
		.append("defValue", defValue).append("optionalLabels", optionalLabels).append("optionalValues", optionalValues);
	return builder.toString();
    }

}
