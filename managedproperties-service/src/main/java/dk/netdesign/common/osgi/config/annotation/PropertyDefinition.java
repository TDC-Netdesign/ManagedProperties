/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to define the overall attributes needed to map an interface to a configuration. In order to map an interface to a configuration
 * with this service, the interface MUST be annotated with this annotation.
 * @author mnn
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface PropertyDefinition {

    /**
     * The name of the configuration in the Configuration Admin. This is a required value.
     * @return The name of the configuration
     */
    public String name();

    /**
     * The ID of the configuration in the Configuration Admin. This is a required value.
     * @return The ID of the configuration
     */
    public String id();

    /**
     * The description of the configuration in the Configuration Admin.
     * @return The description of the configuration
     */
    public String description() default "";

    /**
     * The icon for the configuration in the Configuration Admin.
     * @return The Icon File for the configuration
     */
    public String iconFile() default "";
}
