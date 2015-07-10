/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
	newConfig.put("String", Collections.singletonList("Stringval"));
	newConfig.put("Integer", Collections.singletonList(1));
	newConfig.put("Double", Collections.singletonList(2.2d));
	newConfig.put("Password", Collections.singletonList(new Character[]{'t','e','s','t'}));
	newConfig.put("otherString", Collections.singletonList("otherStringVal"));
	newConfig.put("otherInteger", Collections.singletonList(14));
	newConfig.put("otherDouble", Collections.singletonList(2.6d));
	newConfig.put("IntegerToString", Collections.singletonList(16));
	
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
    
    @Test
    public void testOffline() throws ConfigurationException{
	Map<String, Object> defaults = new HashMap<>();
	defaults.put("String", "testString");
	defaults.put("Integer", 1);
	defaults.put("Double", 1d);
	defaults.put("Password", new Character[]{'t','e','s','t'});
	defaults.put("otherString", "othertestString");
	defaults.put("otherInteger", 11);
	defaults.put("otherDouble", 1.2d);
	defaults.put("IntegerToString", 22);
	props = new TestManagedProperties(defaults, "TestManagedProps", "TestManagedPropsID", "TestManagedPropsDesc", null);
	
	assertEquals(props.getString(), "testString");
	assertEquals(props.getInteger(), new Integer(1));
	assertEquals(props.getDouble(), new Double(1d));
	assertArrayEquals(props.getPassword(), new Character[]{'t','e','s','t'});
	assertEquals(props.getRenamedString(), "othertestString");
	assertEquals(props.getRenamedInteger(), new Integer(11));
	assertEquals(props.getRenamedDouble(), new Double(1.2d));
	assertEquals(props.getIntegerToString(), "22");

    }

    class TestManagedProperties extends ManagedProperties{

	public TestManagedProperties(String name, String id, String description) {
	    super(name, id, description);
	}

	public TestManagedProperties(Map<String, Object> defaults, String name, String id, String description, File iconFile) {
	    super(defaults, name, id, description, iconFile);
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
