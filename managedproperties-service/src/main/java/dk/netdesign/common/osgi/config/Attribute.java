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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implemnetation of AttributeDefinition that has the extra responsibility of tracking the settings for each of the annotated configuration methods.
 * It is the applications one-stop-shop for all information about the parsed Methods, containing both what is sent as MetaDataProvider and what is registered
 * by the ManagedProperties itself.
 * This class is what binds a method to a configuration item.
 * @author mnn
 */
public class Attribute {

    private static final Logger logger = LoggerFactory.getLogger(Attribute.class);

    private String id;
    private Class inputType;
    private String name;
    private String description;
    private String[] defValue;
    private String[] optionalLabels;
    private String[] optionalValues;
    //private Class outputType;
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
    protected Attribute(Method method, Map<FilterReference, Class<? extends TypeFilter>> defaultFilters) throws TypeFilterException, InvalidTypeException, InvalidMethodException {
	Property methodProperty = ManagedPropertiesController.getMethodAnnotation(method);
	name = getAttributeName(method);
	id = name;

	Class methodReturnType = getMethodReturnType(method);
	
	cardinalityDef = methodProperty.cardinality();
	
	if(List.class.isAssignableFrom(method.getReturnType())){
	    cardinalityDef = Property.Cardinality.List;
	}
	
	inputType = getMethodReturnType(method);
	if(methodProperty.type() != void.class){
	    inputType = methodProperty.type();
	}
	
	if (logger.isTraceEnabled()) {
	    logger.trace("Found @Property on " + method.getName() + "[" + methodReturnType + "]");
	}

	if (!methodProperty.id().isEmpty()) {
	    id = methodProperty.id();
	}

	
	Class<? extends TypeFilter> filterFromAnnotation = methodProperty.typeMapper();

	checkCardinality(method);
	
	
	filter = getFilters(filterFromAnnotation, defaultFilters, method);
	
	if (filter != null) {
	    Method parseMethod;
	    try {
		parseMethod = filter.getDeclaredMethod("parse", new Class[]{inputType});
	    } catch (NoSuchMethodException | SecurityException ex) {
		throw new TypeFilterException("Could not add filter. An exception occured while examining the parse method", ex);
	    }

	    if (!cardinalityDef.equals(cardinalityDef.List) && !methodReturnType.isAssignableFrom(parseMethod.getReturnType())) {
		throw new TypeFilterException("Could not use Filter type " + filter.getName() + " with the method " + parseMethod.getName()
			+ " The output of the configuration method '" + methodReturnType + "' must be assignable to the return of the filter parse method '" + parseMethod.getReturnType() + "'");
	    }

	}
	    
	
	if (logger.isTraceEnabled()) {
	    logger.trace("Building AttributeDefinition with attributeID '" + id + "' inputType '" + inputType + "' cardinality '" + cardinalityDef + "'");
	}
	defValue = methodProperty.defaultValue();
	description = methodProperty.description();
	optionalLabels = methodProperty.optionLabels();
	optionalValues = methodProperty.optionValues();

    }
    
    private void checkCardinality(Method method) throws InvalidMethodException{
	if(cardinalityDef.equals(cardinalityDef.List)){
	    if(!List.class.isAssignableFrom(method.getReturnType())){
		throw new InvalidMethodException("Could not create handler for method "+method.getName()+". Methods with list cardinality must return a list");
	    }
	    if(Collection.class.isAssignableFrom(inputType)){
		throw new InvalidMethodException("Could not create handler for method "+method.getName()+". Methods with list must define a property type");
	    }
	}
    }
    
    private Class<? extends TypeFilter> getFilters(Class<? extends TypeFilter> filterFromAnnotation, Map<FilterReference, Class<? extends TypeFilter>> defaultFilters, Method method) throws InvalidMethodException{
	Class methodReturnType = getMethodReturnType(method);
	if (filterFromAnnotation == TypeFilter.class) {
	    if (!cardinalityDef.equals(cardinalityDef.List)) {
		FilterReference ref = new FilterReference(inputType, methodReturnType);
		return defaultFilters.get(ref);
	    }else{
		return null;
	    }

	}else{
	    return filterFromAnnotation;
	}
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
	    if(List.class.isAssignableFrom(methodReturnType) && methodProperty.type() == void.class){
		throw new InvalidMethodException("Could not create handler for method "+classMethod.getName()+". Lists must be accompanied by a returnType");
	    }
	}
	return methodReturnType;
    }

        /**
     * The default value of this method
     * @return The possible default values for this configuration item defined by this method, returned as MetaType information
     */
    public String[] getDefaultValue() {
	return defValue;
    }

    /**
     * The description of this method
     * @return The description for the configuration item defined by this method
     */
    public String getDescription() {
	return description;
    }

    /**
     * The configurationID for this method
     * @return The ID for the configuration item.
     */
    public String getID() {
	return id;
    }

    /**
     * The display name for this method
     * @return The name for the configuration item
     */
    public String getName() {
	return name;
    }

    /**
     * The option labels for this method
     * @return The Option labels for the configuration item
     */
    public String[] getOptionLabels() {
	return optionalLabels;
    }

    /**
     * The option values for this method
     * @return The option values for this configuration item
     */
    public String[] getOptionValues() {
	return optionalValues;
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

    public Class getInputType() {
	return inputType;
    }

    protected void setInputType(Class inputType) {
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
	builder.append("id", id).append("name", name).append("description", description)
		.append("defValue", defValue).append("optionalLabels", optionalLabels).append("optionalValues", optionalValues).append("filter", filter);
	return builder.toString();
    }

}