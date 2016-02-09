/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.enhancement.ConfigurationCallback;
import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.enhancement.ConfigurationCallbackHandler;
import dk.netdesign.common.osgi.config.enhancement.EnhancedProperty;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.filters.FileFilter;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.service.TypeFilter;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.locks.Lock;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;

/**
 *
 * @author mnn
 */
public class ManagedPropertiesServiceTest {

    MockContext stub;
    TestInterface testi;
    ManagedProperties props;
    File testfile;

    public ManagedPropertiesServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
	stub = new MockContext();
	testi = ManagedPropertiesFactory.register(TestInterface.class, new TestInterfaceDefaults(), stub);
	props = (ManagedProperties) stub.lastRegistered;
	testfile = new File("testFile.test");
	testfile.createNewFile();
    }

    @After
    public void tearDown() {
	testfile.delete();
    }

    @Test
    public void testValidTypes() throws Exception {
	Dictionary<String, Object> newConfig = new Hashtable<>();
	String testString = "test";
	Integer testInteger = 123;
	Long testLong = 112l;
	Short testShort = 15;
	Character testCharacter = 'b';
	Byte testByte = 0x34;
	Double testDouble = 123d;
	Float testFloat = 12f;
	BigInteger testBigInteger = BigInteger.valueOf(222);
	BigDecimal testBigDecimal = BigDecimal.valueOf(22.2);
	Boolean testBoolean = true;
	Character[] testPassword = new Character[]{'t', 'e', 's', 't'};
	newConfig.put("String", Collections.singletonList(testString));
	newConfig.put("Integer", Collections.singletonList(testInteger));
	newConfig.put("Long", Collections.singletonList(testLong));
	newConfig.put("Short", Collections.singletonList(testShort));
	newConfig.put("Character", Collections.singletonList(testCharacter));
	newConfig.put("Byte", Collections.singletonList(testByte));
	newConfig.put("Double", Collections.singletonList(testDouble));
	newConfig.put("Float", Collections.singletonList(testFloat));
	newConfig.put("BigInteger", Collections.singletonList(testBigInteger));
	newConfig.put("BigDecimal", Collections.singletonList(testBigDecimal));
	newConfig.put("Boolean", Collections.singletonList(testBoolean));
	newConfig.put("Password", Collections.singletonList(testPassword));
	props.updated(newConfig);

	assertEquals(testString, testi.getString());
	assertEquals(testInteger, testi.getInteger());
	assertEquals(testLong, testi.getLong());
	assertEquals(testShort, testi.getShort());
	assertEquals(testCharacter, testi.getCharacter());
	assertEquals(testByte, testi.getByte());
	assertEquals(testDouble, testi.getDouble());
	assertEquals(testFloat, testi.getFloat());
	assertEquals(testBigInteger, testi.getBigInteger());
	assertEquals(testBigDecimal, testi.getBigDecimal());
	assertEquals(testBoolean, testi.getBoolean());
	assertArrayEquals(testPassword, testi.getPassword());
    }
    
    @Test
    public void testOffline() throws Exception{
	TestInterfaceDefaults expected = new TestInterfaceDefaults();
	assertEquals(expected.getBigDecimal(), testi.getBigDecimal());
	assertEquals(expected.getBigInteger(), testi.getBigInteger());
	assertEquals(expected.getBoolean(), testi.getBoolean());
	assertEquals(expected.getByte(), testi.getByte());
	assertEquals(expected.getCharacter(), testi.getCharacter());
	assertEquals(expected.getDouble(), testi.getDouble());
	assertEquals(expected.getFile(), testi.getFile());
	assertEquals(expected.getFloat(), testi.getFloat());
	assertEquals(expected.getInteger(), testi.getInteger());
	assertEquals(expected.getLong(), testi.getLong());
	assertEquals(expected.getStringInteger(), testi.getStringInteger());
	assertArrayEquals(expected.getPassword(), testi.getPassword());
	
    }
    
    @Test
    public void testDefaultsInheritance() throws Exception{
	TestInterface properties = ManagedPropertiesFactory.register(TestInterface.class, new TestInterfaceDefaultsOverride(), stub);
	props = (ManagedProperties) stub.lastRegistered;
	
	TestInterfaceDefaults expected = new TestInterfaceDefaultsOverride();
	assertEquals(expected.getBigDecimal(), properties.getBigDecimal());
	assertEquals(expected.getBigInteger(), properties.getBigInteger());
	assertEquals(expected.getBoolean(), properties.getBoolean());
	assertEquals(expected.getByte(), properties.getByte());
	assertEquals(expected.getCharacter(), properties.getCharacter());
	assertEquals(expected.getDouble(), properties.getDouble());
	assertEquals(expected.getFile(), properties.getFile());
	assertEquals(expected.getFloat(), properties.getFloat());
	assertEquals(expected.getInteger(), properties.getInteger());
	assertEquals(expected.getLong(), properties.getLong());
	assertEquals(expected.getStringInteger(), properties.getStringInteger());
	assertArrayEquals(expected.getPassword(), properties.getPassword());
	
    }
    
    @Test(expected = UnknownValueException.class)
    public void testOfflineMissing() throws Exception{
	testi.getString();
	
    }
    
    @Test
    public void testToString() throws Exception {
	assertEquals(props.toString(), testi.toString());
    }

    @Test
    public void testTestDirectType() throws Exception {
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("String", Collections.singletonList("Stringval"));

	props.updated(newConfig);
	assertEquals("Stringval", testi.getString());
    }

    @Test
    public void testTestFilteredType() throws Exception {
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("StringInteger", Collections.singletonList(12));

	props.updated(newConfig);
	assertEquals("12", testi.getStringInteger());
    }

    @Test(expected = UnknownValueException.class)
    public void testNonExistant() throws Exception {
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("StringInteger", Collections.singletonList(12));

	props.updated(newConfig);
	testi.getString();
    }

    @Test(expected = TypeFilterException.class)
    public void testBadFilter() throws Exception {
	ManagedPropertiesFactory.register(TestBadFilter.class, stub);
    }

    @Test(expected = InvalidTypeException.class)
    public void testBadType() throws Exception {
	ManagedPropertiesFactory.register(TestBadType.class, stub);
    }

    @Test(expected = InvalidTypeException.class)
    public void testNotInterface() throws Exception {
	ManagedPropertiesFactory.register(Integer.class, stub);
    }

    @Test
    public void testFileFilter() throws Exception {
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("File", Collections.singletonList(testfile.getName()));

	props.updated(newConfig);
	assertEquals(testfile, testi.getFile());
    }

    @Test
    public void testTestNarrowing() throws Exception {
	TestNarrowing narrowing = ManagedPropertiesFactory.register(TestNarrowing.class, stub);
	ManagedProperties mprops = (ManagedProperties) stub.lastRegistered;

	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("Number", Collections.singletonList(10));
	newConfig.put("NumberDouble", Collections.singletonList(11d));
	newConfig.put("NumberFloat", Collections.singletonList(12f));
	mprops.updated(newConfig);

	assertEquals(10, narrowing.getNumber());
	assertEquals(11d, narrowing.getNumberDouble());
	assertEquals(12f, narrowing.getNumberFloat());
    }
    
    @Test(expected = ConfigurationException.class)
    public void testTestNarrowingBadInput() throws Exception {
	TestNarrowing narrowing = ManagedPropertiesFactory.register(TestNarrowing.class, stub);
	ManagedProperties mprops = (ManagedProperties) stub.lastRegistered;
	
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("Number", Collections.singletonList(10d));
	mprops.updated(newConfig);

    }
    
    @Test
    public void testCardinalityOnUpdate() throws Exception{
	TestCardinality cardinality = ManagedPropertiesFactory.register(TestCardinality.class, stub);
	ManagedProperties mprops = (ManagedProperties) stub.lastRegistered;
	
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("Optional", Collections.singletonList(10));
	newConfig.put("List", Arrays.asList(new String[]{"Test", "Fest"}));
	newConfig.put("Required", 500l);
	mprops.updated(newConfig);
	
	assertEquals(new Integer(10), cardinality.getOptional());
	assertEquals(Arrays.asList(new String[]{"Test", "Fest"}), cardinality.getList());
	assertEquals(new Long(500), cardinality.getRequired());
    }
    
    @Test(expected = ConfigurationException.class)
    public void testMissingRequredValueOnUpdate() throws Exception{
	TestCardinality cardinality = ManagedPropertiesFactory.register(TestCardinality.class, stub);
	ManagedProperties mprops = (ManagedProperties) stub.lastRegistered;
	
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("Optional", Collections.singletonList(10));
	newConfig.put("List", Arrays.asList(new String[]{"Test", "Fest"}));
	mprops.updated(newConfig);

    }
    
    @Test(expected = InvalidMethodException.class)
    public void testBadListCardinality() throws Exception{
	TestBadListCardinality cardinality = ManagedPropertiesFactory.register(TestBadListCardinality.class, stub);
    }
    
    @Test(expected = InvalidMethodException.class)
    public void testBadListType() throws Exception{
	TestBadListType cardinality = ManagedPropertiesFactory.register(TestBadListType.class, stub);
    }
    
    
    
    @Test
    public void testNarrowingDefaultsNoUpdate() throws Exception{
	TestNarrowing narrowing = ManagedPropertiesFactory.register(TestNarrowing.class, new NarrowingDefaults(), stub);
	ManagedProperties mprops = (ManagedProperties) stub.lastRegistered;

	assertEquals(new NarrowingDefaults().getNumber(), narrowing.getNumber());
	assertEquals(new NarrowingDefaults().getNumberDouble(), narrowing.getNumberDouble());
    }
    
    @Test
    public void testNarrowingDefaultsPartialUpdate() throws Exception{
	TestNarrowing narrowing = ManagedPropertiesFactory.register(TestNarrowing.class, new NarrowingDefaults(), stub);
	ManagedProperties mprops = (ManagedProperties) stub.lastRegistered;

	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("Number", Collections.singletonList(10));
	mprops.updated(newConfig);
	assertEquals(10, narrowing.getNumber());
	assertEquals(new NarrowingDefaults().getNumberDouble(), narrowing.getNumberDouble());
    }
    
    @Test(expected = UnknownValueException.class)
    public void testNarrowingMissingDefaults() throws Exception{
	TestNarrowing narrowing = ManagedPropertiesFactory.register(TestNarrowing.class, new NarrowingDefaults(), stub);
	ManagedProperties mprops = (ManagedProperties) stub.lastRegistered;
	narrowing.getNumberFloat(); //Returns null
    }

    @Test
    public void testLock() throws Exception {
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("String", Collections.singletonList("Stringval"));
	props.updated(newConfig);

	Lock lock = ((EnhancedProperty)testi).lockPropertiesUpdate();
	String toTest;
	try {
	    toTest = testi.getString();
	} finally {
	    lock.unlock();
	}
	assertEquals("Stringval", toTest);
    }

    @Test @Ignore
    public void testCallback() throws Exception {
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("String", Collections.singletonList("Stringval"));
	props.updated(newConfig);

	((ConfigurationCallbackHandler)testi).addConfigurationCallback(null);
	String toTest;
	
	//assertEquals("Stringval", toTest);
    }

    @Test(expected = InvalidTypeException.class)
    public void testUnknownInputType() throws Exception {
	ManagedPropertiesFactory.register(TestUnknownInputType.class, stub);
    }

    @Test(expected = TypeFilterException.class)
    public void testAbstract() throws Exception {
	ManagedPropertiesFactory.register(TestAbstractReturn.class, stub);
    }

    @Test(expected = DoubleIDException.class)
    public void testDoubleAttributeID() throws Exception {
	ManagedPropertiesFactory.register(TestDoubleAttributeID.class, stub);
    }

    @Test(expected = DoubleIDException.class) @Ignore
    public void testDoubleInterfaceID() throws Exception {
	ManagedPropertiesFactory.register(TestDoubleInterfaceID1.class, stub);
	ManagedPropertiesFactory.register(TestDoubleInterfaceID2.class, stub);
    }
    
    @Test(expected = InvalidMethodException.class)
    public void testMethodWithParams() throws Exception {
	ManagedPropertiesFactory.register(IllegalMethod.class, stub);
    }
    
    @Test(expected = InvalidTypeException.class)
    public void testIllegalInputType() throws Exception{
	ManagedPropertiesFactory.register(IllegalInputType.class, stub);
    }
    
    @Test @Ignore
    public void testLegalInputType() throws Exception{
	ManagedPropertiesFactory.register(LegalInputTypes.class, stub);
	String testString = "test";
	Integer testInteger = 123;
	Long testLong = 112l;
	Short testShort = 15;
	Character testCharacter = 'b';
	Byte testByte = 0x34;
	Double testDouble = 123d;
	Float testFloat = 12f;
	BigInteger testBigInteger = BigInteger.valueOf(222);
	BigDecimal testBigDecimal = BigDecimal.valueOf(22.2);
	Boolean testBoolean = true;
	Character[] testPassword = new Character[]{'t', 'e', 's', 't'};
	
	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("String", Collections.singletonList(testString));
	newConfig.put("Integer", Collections.singletonList(testInteger));
	newConfig.put("Long", Collections.singletonList(testLong));
	newConfig.put("Short", Collections.singletonList(testShort));
	newConfig.put("Character", Collections.singletonList(testCharacter));
	newConfig.put("Byte", Collections.singletonList(testByte));
	newConfig.put("Double", Collections.singletonList(testDouble));
	newConfig.put("Float", Collections.singletonList(testFloat));
	newConfig.put("BigInteger", Collections.singletonList(testBigInteger));
	newConfig.put("BigDecimal", Collections.singletonList(testBigDecimal));
	newConfig.put("Boolean", Collections.singletonList(testBoolean));
	newConfig.put("Password", Collections.singletonList(testPassword));
	
    }

    @Test @Ignore
    public void testRepeatedInterface() throws Exception {
	TestDoubleInterfaceID1 i1 = ManagedPropertiesFactory.register(TestDoubleInterfaceID1.class, stub);
	ManagedProperties i1i2Props = (ManagedProperties) stub.lastRegistered;
	TestDoubleInterfaceID1 i2 = ManagedPropertiesFactory.register(TestDoubleInterfaceID1.class, stub);

	Dictionary<String, Object> newConfig = new Hashtable<>();
	newConfig.put("String", Collections.singletonList("Stringval"));
	i1i2Props.updated(newConfig);

	assertEquals("Stringval", i1.getString());
	assertEquals(i1.getString(), i2.getString());

    }

    @PropertyDefinition(id = "TestInterface", name = "TestInterfaceName")
    private static interface TestInterface{

	@Property
	public String getString() throws InvalidTypeException, TypeFilterException;

	@Property
	public Integer getInteger() throws InvalidTypeException, TypeFilterException;

	@Property
	public Long getLong() throws InvalidTypeException, TypeFilterException;

	@Property
	public Short getShort() throws InvalidTypeException, TypeFilterException;

	@Property
	public Character getCharacter() throws InvalidTypeException, TypeFilterException;

	@Property
	public Byte getByte() throws InvalidTypeException, TypeFilterException;

	@Property
	public Double getDouble() throws InvalidTypeException, TypeFilterException;

	@Property
	public Float getFloat() throws InvalidTypeException, TypeFilterException;

	@Property
	public BigInteger getBigInteger() throws InvalidTypeException, TypeFilterException;

	@Property
	public BigDecimal getBigDecimal() throws InvalidTypeException, TypeFilterException;

	@Property
	public Boolean getBoolean() throws InvalidTypeException, TypeFilterException;

	@Property
	public Character[] getPassword() throws InvalidTypeException, TypeFilterException;

	@Property(type = Integer.class, typeMapper = StringFilter.class)
	public String getStringInteger() throws InvalidTypeException, TypeFilterException;

	@Property(type = String.class, typeMapper = FileFilter.class)
	public File getFile() throws InvalidTypeException, TypeFilterException;
    }
    
    private static class TestInterfaceDefaults implements TestInterface{

	@Override
	public String getString() throws InvalidTypeException, TypeFilterException {
	    return null;
	}

	@Override
	public Integer getInteger() throws InvalidTypeException, TypeFilterException {
	    return 21;
	}

	@Override
	public Long getLong() throws InvalidTypeException, TypeFilterException {
	    return 1l;
	}

	@Override
	public Short getShort() throws InvalidTypeException, TypeFilterException {
	    return 23;
	}

	@Override
	public Character getCharacter() throws InvalidTypeException, TypeFilterException {
	    return 'p';
	}

	@Override
	public Byte getByte() throws InvalidTypeException, TypeFilterException {
	    return 0x23;
	}

	@Override
	public Double getDouble() throws InvalidTypeException, TypeFilterException {
	    return 3434d;
	}

	@Override
	public Float getFloat() throws InvalidTypeException, TypeFilterException {
	    return 2f;
	}

	@Override
	public BigInteger getBigInteger() throws InvalidTypeException, TypeFilterException {
	    return BigInteger.valueOf(234);
	}

	@Override
	public BigDecimal getBigDecimal() throws InvalidTypeException, TypeFilterException {
	    return BigDecimal.valueOf(23.2d);
	}

	@Override
	public Boolean getBoolean() throws InvalidTypeException, TypeFilterException {
	    return true;
	}

	@Override
	public Character[] getPassword() throws InvalidTypeException, TypeFilterException {
	    return new Character[]{'f','i','s','k'};
	}

	@Override
	public String getStringInteger() throws InvalidTypeException, TypeFilterException {
	    return "34";
	}

	@Override
	public File getFile() throws InvalidTypeException, TypeFilterException {
	    return new File("test");
	}
	
    }
    
    private static class TestInterfaceDefaultsOverride extends TestInterfaceDefaults{

	@Override
	public String getStringInteger() throws InvalidTypeException, TypeFilterException {
	    return "53";
	}

	@Override
	public Integer getInteger() throws InvalidTypeException, TypeFilterException {
	    return 53;
	}
	
	
	
    }
    
    @PropertyDefinition(id = "CardinalityTest", name = "CardinalityTestName")
    private static interface TestCardinality extends EnhancedProperty, ConfigurationCallback {

	@Property(cardinality = Property.Cardinality.List, type = String.class)
	public List<String> getList() throws InvalidTypeException, TypeFilterException;

	@Property(cardinality = Property.Cardinality.Optional)
	public Integer getOptional() throws InvalidTypeException, TypeFilterException;

	@Property(cardinality = Property.Cardinality.Required)
	public Long getRequired() throws InvalidTypeException, TypeFilterException;
    }
    
    @PropertyDefinition(id = "BadCardinalityTest", name = "BadCardinalityTestName")
    private static interface TestBadListCardinality extends EnhancedProperty, ConfigurationCallback {

	@Property(cardinality = Property.Cardinality.List)
	public String getList() throws InvalidTypeException, TypeFilterException;

    }
    
    @PropertyDefinition(id = "TestBadListType", name = "TestBadListTypeName")
    private static interface TestBadListType extends EnhancedProperty, ConfigurationCallback {

	@Property(cardinality = Property.Cardinality.List)
	public List<String> getList() throws InvalidTypeException, TypeFilterException;

    }

    @PropertyDefinition(id = "TestBadFilter", name = "BadFilterName")
    private static interface TestBadFilter {

	@Property(type = Integer.class, typeMapper = StringFilter.class)
	public Number getStringInteger() throws InvalidTypeException, TypeFilterException;

    }

    @PropertyDefinition(id = "TestBadReturnType", name = "BadReturnName")
    private static interface TestBadType {

	@Property(type = Integer.class)
	public String getStringInteger() throws InvalidTypeException, TypeFilterException;

    }

    @PropertyDefinition(id = "TestNarrowing", name = "TestNarrowingName")
    private static interface TestNarrowing {

	@Property(type = Integer.class)
	public Number getNumber() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Double.class)
	public Number getNumberDouble() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Float.class)
	public Number getNumberFloat() throws InvalidTypeException, TypeFilterException;

    }
    
    private static class NarrowingDefaults implements TestNarrowing{

	@Override
	public Number getNumber() throws InvalidTypeException, TypeFilterException {
	    return 23;
	}

	@Override
	public Number getNumberDouble() throws InvalidTypeException, TypeFilterException {
	    return 14d;
	}

	@Override
	public Number getNumberFloat() throws InvalidTypeException, TypeFilterException {
	    return null;
	}
    }

    @PropertyDefinition(id = "TestBadInputType", name = "TestBadInputType")
    private static interface TestUnknownInputType {

	@Property
	public Number getNumber() throws InvalidTypeException, TypeFilterException;
    }

    @PropertyDefinition(id = "Uninitializable", name = "UninitializableName")
    private static interface TestAbstractReturn {

	@Property(type = String.class, typeMapper = AbstractFilter.class)
	public UninitializableClass getAbstract() throws InvalidTypeException, TypeFilterException;
    }

    @PropertyDefinition(id = "DoubleAttributeID", name = "DoubleAttributeName")
    private static interface TestDoubleAttributeID {

	@Property(id = "identicalID")
	public Integer getInteger() throws InvalidTypeException, TypeFilterException;

	@Property(id = "identicalID")
	public Integer getOtherInteger() throws InvalidTypeException, TypeFilterException;
    }

    @PropertyDefinition(id = "DoubleID", name = "DoubleIDName1")
    private static interface TestDoubleInterfaceID1 {

	@Property
	public String getString() throws InvalidTypeException, TypeFilterException;
    }

    @PropertyDefinition(id = "DoubleID", name = "DoubleIDName2")
    private static interface TestDoubleInterfaceID2 {

	@Property
	public String getString() throws InvalidTypeException, TypeFilterException;
    }
    
    @PropertyDefinition(id = "IllegalMethod", name = "IllegalMethodName")
    private static interface IllegalMethod {

	@Property
	public Number getNumber(Integer paramter) throws InvalidTypeException, TypeFilterException;
    }
    
    @PropertyDefinition(id = "IllegalInputType", name = "IllegalInputTypeName")
    private static interface IllegalInputType {

	@Property(type = URL.class)
	public String getURL() throws InvalidTypeException, TypeFilterException;
    }

    @PropertyDefinition(id = "LegalInputTypes", name = "LegalInputTypesName")
    private static interface LegalInputTypes {

	@Property(type = String.class)
	public String getString() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Long.class)
	public String getLong() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Integer.class)
	public String getInteger() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Short.class)
	public String getShort() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Character.class)
	public String getCharacter() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Byte.class)
	public String getByte() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Double.class)
	public String getDouble() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Float.class)
	public String getFloat() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = BigInteger.class)
	public String getBigInteger() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = BigDecimal.class)
	public String getDecimal() throws InvalidTypeException, TypeFilterException;

	@Property(type = Boolean.class)
	public String getBoolean() throws InvalidTypeException, TypeFilterException;

	@Property(type = Character[].class)
	public String getPassword() throws InvalidTypeException, TypeFilterException;
    }
    
    private abstract class UninitializableClass {

	public UninitializableClass() {
	}

	public abstract void NotImplemented();
    }

    private static class AbstractFilter extends TypeFilter<String, UninitializableClass> {

	@Override
	public UninitializableClass parse(String input) throws TypeFilterException {
	    return null;
	}
    }

    private static class StringFilter extends TypeFilter<Integer, String> {

	public StringFilter() {
	}

	@Override
	public String parse(Integer input) throws TypeFilterException {
	    return input.toString();
	}

    }

    

}
