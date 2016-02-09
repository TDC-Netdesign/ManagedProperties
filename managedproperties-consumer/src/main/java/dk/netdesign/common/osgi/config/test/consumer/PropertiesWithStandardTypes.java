/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config.test.consumer;

import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.filters.URLFilter;
import java.net.URL;
import java.util.List;

/**
 *
 * @author mnn
 */
@PropertyDefinition(name = "TestConsumer one")
public interface PropertiesWithStandardTypes {
    
    @Property
    public String getStringProperty() throws InvalidTypeException, TypeFilterException;
    
    @Property
    public Integer getStringInteger() throws InvalidTypeException, TypeFilterException;
    
    @Property
    public Double getDoubleProperty() throws InvalidTypeException, TypeFilterException;
    
    @Property
    public Character getCharacterProperty() throws InvalidTypeException, TypeFilterException;
    
    @Property(type = String.class, cardinality = Property.Cardinality.List)
    public List<String> getStringListProperty() throws InvalidTypeException, TypeFilterException;
    
    @Property(type = String.class, typeMapper = URLFilter.class)
    public URL getURLProperty() throws InvalidTypeException, TypeFilterException;
    
    
    
}
