/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.annotation;

import dk.netdesign.common.osgi.config.service.TypeFilter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this to define a get Method that should be reflected in the MetaDataProvider and ConfigurationAdmin.
 *
 * @author mnn
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface Property {

    /**
     * The name which will represent to configuration property. If not defined, the name of the method will be used (removing the "get").
     *
     * @return The name to use for the property
     */
    public String name() default "";

    /**
     * The ID to represent the configuration property. If not defined, the parsed property name will be used.
     *
     * @return The ID that uniquely identifies the property
     */
    public String id() default "";

    /**
     * The description to represent the configuration property
     *
     * @return The description of the property
     */
    public String description() default "";

    /**
     * The cardinality to represent to property. Defaults to optional.
     *
     * @return The cardinality fo the property
     */
    public Cardinality cardinality() default Cardinality.Optional;

    /**
     * The type of the configuration property when saved by the configuration manager. If not defined, will default to the returntype of the method. 
     * Should only be set to primitive wrappers(Integer, Long...) or String, as these are the only supported types for the ConfigurationAdmin.
     *
     * @return The type of object which should be returned from the ConfigAdmin
     */
    public Class type() default void.class;

    
    /**
     * Defines the TypeFilter to use for this method. If a TypeFilter is defined for this method, it will be run when a new configuration is recieved.
     * The mapper must have the same input type as the {@link #type() type} of the method, and the same output as the method return type. Defaults to none.
     *
     * @return The TypeFilter to use to parse this property
     */
    public Class<? extends TypeFilter> typeMapper() default TypeFilter.class;

    /**
     * The optional values for the property. Labels and values must match.
     *
     * @return The optional values to use in the Configuration Admin
     */
    public String[] optionValues() default {};

    /**
     * The optional labels for the property. Labels and values must match.
     *
     * @return The optional labels to use in the Configuration Admin
     */
    public String[] optionLabels() default {};

    /**
     * The default value for the property.
     *
     * @return The default value to use in the Configuration Admin
     */
    public String[] defaultValue() default {};

    public enum Cardinality {

	Optional, Required, List;
    }
}
