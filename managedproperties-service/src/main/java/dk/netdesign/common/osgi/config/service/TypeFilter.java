/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.service;

import dk.netdesign.common.osgi.config.exception.TypeFilterException;

/**
 * This is the supertype for all TypeFilters that can be used for the ManagedProperties service.
 * A TypeFilter is used to do post-update validations and build advanced objects from the Configuration Admin basic types. The TypeFilter class is a fairly
 * powerful function that is simple to use. TypeFilters can use used for many things, but here are a few examples:
 * Creating advanced types, such as File or URL objects, from the basic types you can use in ConfigurationAdmin.
 * Post-validating configurations, such as checking that a File actually exists, before applying the configuration, or making sure that a URL is well-formatted.
 * @author mnn
 * @param <I> The type of input to filter. This is the input value that will be filtered in order to build the output value.
 * @param <O> The type of output to filter. This is the type of output to produce from the filter.
 */
public abstract class TypeFilter<I extends Object, O extends Object> {

    public TypeFilter() {
    }

    public abstract O parse(I input) throws TypeFilterException;

}
