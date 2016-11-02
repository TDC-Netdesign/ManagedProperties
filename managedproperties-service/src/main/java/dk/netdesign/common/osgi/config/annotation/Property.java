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
 * Use this to define a get Method that should be treated as a configuration item.
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
     * The type of the configuration property which should be input into this configuration. This is used by the backing provider to ensure a
     * propper resolution of the configuration type. If not defined, will default to the returntype of the method. 
     * If the type differs from the providers output, ManagedProperties will fail to register, unless a default filter exists to bridge the gap.
     *
     * @return The type of object which should be returned from the Property provider
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
     * The optional values for the property. Labels and values must match. (Optional value)
     *
     * @return The option values to use in the Configuration provider
     */
    public String[] optionValues() default {};

    /**
     * The optional labels for the property. Labels and values must match. (Optional value)
     *
     * @return The optional labels to use in the Configuration provider
     */
    public String[] optionLabels() default {};

    /**
     * The default value for the property. (Optional value)
     *
     * @return The default value to use in the Configuration Provider
     */
    public String[] defaultValue() default {};

    /**
     * The default value for the property. (Optional value)
     *
     * @return The default value to use in the Configuration Provider
     */
    public boolean hidden() default false;

    public enum Cardinality {

	Optional, Required, List;
    }
}
