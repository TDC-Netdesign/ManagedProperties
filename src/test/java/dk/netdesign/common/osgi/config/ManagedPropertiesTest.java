/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 *
 * @author mnn
 */
public class ManagedPropertiesTest {
    public TestManagedProperties props;
    
    public ManagedPropertiesTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
	props = new TestManagedProperties("TestManagedProps", "TestManagedPropsID", "TestManagedPropsDesc");
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getDefaults method, of class ManagedProperties.
     */
    @Test
    public void testOCD() {
	ObjectClassDefinition def = props.getObjectClassDefinition("TestManagedPropsID", null);
	assertEquals("TestManagedPropsID", def.getID());
	assertEquals("TestManagedProps", def.getName());
	assertEquals("TestManagedPropsDesc", def.getDescription());
    }
    
    @Test
    public void testUpdate() throws ConfigurationException{
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("String", "Stringval");
	newConfig.put("Integer", 1);
	newConfig.put("Double", 2.2d);
	newConfig.put("Password", new Character[]{'t','e','s','t'});
	newConfig.put("otherString", "otherStringVal");
	newConfig.put("otherInteger", 14);
	newConfig.put("otherDouble", 2.6d);
	newConfig.put("IntegerToString", 16);
	
	props.updated(newConfig);
	
	assertTrue("Did not match", props.getString().equals("Stringval"));
	assertTrue("Did not match", props.getInteger().equals(1));
	assertTrue("Did not match", props.getDouble().equals(2.2d));
	assertTrue("Did not match", Arrays.deepEquals(props.getPassword(), new Character[]{'t','e','s','t'}));
	assertTrue("Did not match", props.getRenamedString().equals("otherStringVal"));
	assertTrue("Did not match", props.getRenamedInteger().equals(14));
	assertTrue("Did not match", props.getRenamedDouble().equals(2.6d));
	assertTrue("Did not match", props.getIntegerToString().equals("16"));
	
	
    }

    class TestManagedProperties extends ManagedProperties{

	public TestManagedProperties(String name, String id, String description) {
	    super(name, id, description);
	}
	
	@Property
	public String getString(){
	    return get("String", String.class);
	}
	
	@Property
	public Integer getInteger(){
	    return get("Integer", Integer.class);
	}
	
	@Property
	public Double getDouble(){
	    return get("Double", Double.class);
	}
	
	@Property
	public Character[] getPassword(){
	    return get("Password", Character[].class);
	}
	
	@Property(name = "otherString")
	public String getRenamedString(){
	    return get("otherString", String.class);
	}
	
	@Property(name = "otherInteger")
	public Integer getRenamedInteger(){
	    return get("otherInteger", Integer.class);
	}
	
	@Property(name = "otherDouble")
	public Double getRenamedDouble(){
	    return get("otherDouble", Double.class);
	}
	
	@Property(type = Integer.class)
	public String getIntegerToString(){
	    return get("IntegerToString", Integer.class).toString();
	}
	
	
    }
    
}
