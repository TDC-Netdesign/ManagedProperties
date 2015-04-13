/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author mnn
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.METHOD})
public @interface Property {
    
    public String name() default "";

    public String id() default "";

    public String description() default "";

    public int cardinality() default -1;

    public Class type() default void.class;

    public String[] optionValues() default {};

    public String[] optionLabels() default {};

    public String[] defaultValue() default {};
    
}
