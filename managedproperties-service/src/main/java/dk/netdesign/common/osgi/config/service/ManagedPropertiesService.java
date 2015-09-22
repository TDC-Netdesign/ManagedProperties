/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config.service;

import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;

/**
 * This is the main entry to the ManagedProperties service. It exposes two methods which are both used to bind an Interface to a ConfigAdmin configuration.
 * When any of the Register methods are called, the ManagedProperties service will run through the provided interface, parse the annotated methods,
 * and build up the required data to map the Interface to a Configuration Admin service. It will create a MetaType 
 * {@link org.osgi.service.metatype.ObjectClassDefinition ObjectClassDefinition} to reflect the configuration, check that all values are valid, and register
 * the Interface as a ManagedService and MetaTypeProvider
 * @see org.osgi.service.cm.ManagedService
 * @see org.osgi.service.metatype.MetaTypeProvider 
 * 
 * @author mnn
 */
public interface ManagedPropertiesService {

    /**
     * This class will register a new configuration of the @see type defined by the interface supplied to the method.
     * The returned object will be a proxy, that will allow the user to call any methods on the interface annotated with @Property and get the corresponding
     * configuration value.
     * Example:
     * SomeInterface properties = managedPropertiesService.register(SomeInterface.class);
     * 
     * SomeInterface must be annotated with @see dk.netdesign.common.osgi.config.annotation.PropertyDefinition
     * @param <T> The type to return.
     * @param type The type of configuration to create. This MUST be an interface and the interface must be annotated with 
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition PropertyDefinition}
     * @return An implementation of the interface defined by @see type, backed by a @see java.util.Proxy. The proxy implements the interfaces @see type,
     * {@link  dk.netdesign.common.osgi.config.enhancement.ConfigurationCallbackHandler ConfigurationCallbackHandler} and 
     * {@link dk.netdesign.common.osgi.config.enhancement.EnhancedProperty EnhancedProperty}
     * @throws InvalidTypeException If a method/configuration item mapping uses an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any restriction not defined in the other exceptions.
     */
    <T extends Object> T register(Class<T> type) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException;

    /**
     * This class will register a new configuration of the @see type defined by the interface supplied to the method.
     * The returned object will be a proxy, that will allow the user to call any methods on the interface annotated with @Property and get the corresponding
     * configuration value. This method also allows the user to specify a defaults object. This object is basically just an implementation of the interface
     * that defines the type of proxy. The defaults will be called whenever the proxy cannot find a value for the Configuration item mapped to the method.
     * 
     * Example:
     * SomeInterface properties = managedPropertiesService.register(SomeInterface.class, new SomeInterfaceImpl());
     * 
     * SomeInterface must be annotated with @see dk.netdesign.common.osgi.config.annotation.PropertyDefinition
     * @param <I> The returned type.
     * @param <T> The type of object to return.
     * @param type The type of configuration to create. This MUST be an interface and the interface must be annotated with 
     * {@link dk.netdesign.common.osgi.config.annotation.PropertyDefinition PropertyDefinition}.
     * @param defaults The default values to use if a configuration item is not currently in the configuration set. The defaults must be an object
     * of a type that implements the type denoted by @see I.
     * @return An implementation of the interface defined by @see type, backed by a @see java.util.Proxy. The proxy implements the interfaces @see type,
     * {@link  dk.netdesign.common.osgi.config.enhancement.ConfigurationCallbackHandler ConfigurationCallbackHandler} and 
     * {@link dk.netdesign.common.osgi.config.enhancement.EnhancedProperty EnhancedProperty}
     * @throws InvalidTypeException If a method/configuration item mapping uses an invalid type.
     * @throws TypeFilterException If a method/configuration item mapping uses an invalid TypeMapper.
     * @throws DoubleIDException If a method/configuration item mapping uses an ID that is already defined.
     * @throws InvalidMethodException If a method/configuration violates any restriction not defined in the other exceptions.
     */
    <I, T extends I> I register(Class<I> type, T defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException;
    
}
