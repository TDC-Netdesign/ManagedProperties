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
     * The name which will represent to configuration property. If not defined, the name of the method or property will be used.
     *
     * @return The name to use for the property
     */
    public String name() default "";

    /**
     * The ID to represent the configuration property. If not defined, the name of the property will be used.
     *
     * @return The ID that uniquely identifies the property
     */
    public String id() default "";

    /**
     * The description metadata to represent the configuration property
     *
     * @return
     */
    public String description() default "";

    /**
     * The cardinality to represent to property.
     *
     * @return
     */
    public Cardinality cardinality() default Cardinality.Optional;

    /**
     * The type of the configuration property when saved by the configuration manager. If not defined, will default to the type of the method. Should only be
     * set to primitive wrappers(Integer, Long...) or String.
     *
     * @return
     */
    public Class type() default void.class;

    public Class<? extends TypeFilter> typeMapper() default TypeFilter.class;

    /**
     * The optional values for the property. Labels and values must match.
     *
     * @return
     */
    public String[] optionValues() default {};

    /**
     * The optional labels for the property. Labels and values must match.
     *
     * @return
     */
    public String[] optionLabels() default {};

    /**
     * The default value for the property.
     *
     * @return
     */
    public String[] defaultValue() default {};

    public enum Cardinality {

	Optional, Required, List;
    }
}