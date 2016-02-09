/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config.test.consumer;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author mnn
 */
public class DefaultProperties implements InheritedProperties{

    @Override
    public String getStringProperty() {
	return "default";
    }

    @Override
    public Integer getStringInteger() {
	return 22;
    }

    @Override
    public Double getDoubleProperty() {
	return 72d;
    }

    @Override
    public Character getCharacterProperty() {
	return 'g';
    }

    @Override
    public List<String> getStringListProperty() {
	return Arrays.asList(new String[]{"test", "testtest", "testtesttest"});
    }

    @Override
    public URL getURLProperty() {
	return null;
    }

    @Override
    public String getSubString() {
	return "subdubdee";
    }
    
    

    
    
}
