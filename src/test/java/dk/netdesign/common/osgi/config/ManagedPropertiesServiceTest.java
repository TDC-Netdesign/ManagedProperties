/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.enhancement.EnhancedProperty;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.filters.FileFilter;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.locks.Lock;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
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

/**
 *
 * @author mnn
 */
public class ManagedPropertiesServiceTest {
    ContextStub stub;
    ManagedPropertiesService service;
    TestInterface testi;
    ManagedPropertiesService.ManagedProperties props;
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
    public void setUp() throws Exception{
	stub = new ContextStub();
	service = new ManagedPropertiesService();
	service.start(stub);
	testi = service.register(TestInterface.class);
	props = (ManagedPropertiesService.ManagedProperties)stub.lastRegistered;
	testfile = new File("testFile.test");
	testfile.createNewFile();
	
    }
    
    @After
    public void tearDown() {
	testfile.delete();
    }

    @Test
    public void testTestDirectType() throws Exception{
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("String", Collections.singletonList("Stringval"));
        
	props.updated(newConfig);
	assertEquals("Stringval", testi.getString());
    }
    
    @Test
    public void testTestFilteredType() throws Exception{
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("StringInteger", Collections.singletonList(12));
        
	props.updated(newConfig);
	assertEquals("12", testi.getStringInteger());
    }
    
    @Test(expected = InvalidTypeException.class)
    public void testNonExistant() throws Exception{
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("StringInteger", Collections.singletonList(12));
        
	props.updated(newConfig);
	testi.getString();
    }
    
    @Test(expected = TypeFilterException.class)
    public void testBadFilter() throws Exception{
	service.register(TestBadFilter.class);
    }
    
    @Test(expected = InvalidTypeException.class)
    public void testBadType() throws Exception{
	service.register(TestBadType.class);
    }
    
    @Test(expected = InvalidTypeException.class)
    public void testNotInterface() throws Exception{
	service.register(Integer.class);
    }
    
    @Test
    public void testFileFilter() throws Exception{
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("File", Collections.singletonList(testfile.getName()));
        
	props.updated(newConfig);
	assertEquals(testfile, testi.getFile());
    }
    
    @Test
    public void testTestNarrowing() throws Exception{
	TestNarrowing narrowing = service.register(TestNarrowing.class);
	ManagedPropertiesService.ManagedProperties mprops = (ManagedPropertiesService.ManagedProperties)stub.lastRegistered;
	
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("Number", Collections.singletonList(12));
	mprops.updated(newConfig);
	
	assertEquals(12, narrowing.getNumber());
    }
    
    @Test
    public void testLock() throws Exception{
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("String", Collections.singletonList("Stringval"));
	props.updated(newConfig);
	
	Lock lock = testi.lockPropertiesUpdate();
	String toTest;
	try{
	     toTest = testi.getString();
	} finally {
	    lock.unlock();
	}
	assertEquals("Stringval", toTest);
    }
    
    @Test
    public void testCallback() throws Exception{
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("String", Collections.singletonList("Stringval"));
	props.updated(newConfig);
	
	Lock lock = testi.lockPropertiesUpdate();
	String toTest;
	try{
	     toTest = testi.getString();
	} finally {
	    lock.unlock();
	}
	assertEquals("Stringval", toTest);
    }
    
    @Test(expected = InvalidTypeException.class)
    public void testUnknownInputType() throws Exception{
	service.register(TestUnknownInputType.class);
    }
    
    @Test(expected = TypeFilterException.class)
    public void testAbstract() throws Exception{
	service.register(TestAbstractReturn.class);
    }
    
    @Test(expected = DoubleIDException.class)
    public void testDoubleAttributeID() throws Exception{
	service.register(TestDoubleAttributeID.class);
    }
    
    @Test(expected = DoubleIDException.class)
    public void testDoubleInterfaceID() throws Exception{
	service.register(TestDoubleInterfaceID1.class);
	service.register(TestDoubleInterfaceID2.class);
    }
    
    @Test
    public void testRepeatedInterface() throws Exception{
	TestDoubleInterfaceID1 i1 = service.register(TestDoubleInterfaceID1.class);
	ManagedPropertiesService.ManagedProperties i1i2Props = (ManagedPropertiesService.ManagedProperties)stub.lastRegistered;
	TestDoubleInterfaceID1 i2 = service.register(TestDoubleInterfaceID1.class);
	
	Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("String", Collections.singletonList("Stringval"));
	i1i2Props.updated(newConfig);
	
	assertEquals("Stringval", i1.getString());
	assertEquals(i1.getString(), i2.getString());
		
    }
    
    
    @PropertyDefinition(id = "TestInterface", name = "TestInterfaceName")
    private static interface TestInterface extends EnhancedProperty, ConfigurationCallback{
	@Property
	public String getString() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = Integer.class, typeMapper = StringFilter.class)
	public String getStringInteger() throws InvalidTypeException, TypeFilterException;
	
	@Property(type = String.class, typeMapper = FileFilter.class)
	public File getFile() throws InvalidTypeException, TypeFilterException;
	
    }
    
    @PropertyDefinition(id = "TestBadFilter", name = "BadFilterName")
    private static interface TestBadFilter{
	
	@Property(type = Integer.class, typeMapper = StringFilter.class)
	public Number getStringInteger() throws InvalidTypeException, TypeFilterException;
	
    }
    
    @PropertyDefinition(id = "TestBadReturnType", name = "BadReturnName")
    private static interface TestBadType{
	
	@Property(type = Integer.class)
	public String getStringInteger() throws InvalidTypeException, TypeFilterException;
	
    }
    
    @PropertyDefinition(id = "TestNarrowing", name = "TestNarrowingName")
    private static interface TestNarrowing{
	
	@Property(type = Integer.class)
	public Number getNumber() throws InvalidTypeException, TypeFilterException;
	
    }
    
    @PropertyDefinition(id = "TestBadInputType", name = "TestBadInputType")
    private static interface TestUnknownInputType{
	@Property
	public Number getNumber() throws InvalidTypeException, TypeFilterException;
    }
    
    @PropertyDefinition(id = "Uninitializable", name = "UninitializableName")
    private static interface TestAbstractReturn{
	@Property(type = String.class, typeMapper = AbstractFilter.class)
	public UninitializableClass getAbstract() throws InvalidTypeException, TypeFilterException;
    }
    
    @PropertyDefinition(id = "DoubleAttributeID", name = "DoubleAttributeName")
    private static interface TestDoubleAttributeID{
	@Property(id = "identicalID")
	public Integer getInteger() throws InvalidTypeException, TypeFilterException;
	
	@Property(id = "identicalID")
	public Integer getOtherInteger() throws InvalidTypeException, TypeFilterException;
    }
    
    @PropertyDefinition(id = "DoubleID", name = "DoubleIDName1")
    private static interface TestDoubleInterfaceID1{
	@Property
	public String getString() throws InvalidTypeException, TypeFilterException;
    }
    
    @PropertyDefinition(id = "DoubleID", name = "DoubleIDName2")
    private static interface TestDoubleInterfaceID2{
	@Property
	public String getString() throws InvalidTypeException, TypeFilterException;
    }
    
    private abstract class UninitializableClass{
	public UninitializableClass() {
	}
	
	public abstract void NotImplemented();
    }
    
    private static class AbstractFilter extends TypeFilter<String>{
	@Override
	public UninitializableClass parse(String input) throws TypeFilterException {
	    return null;
	}
    } 
    
    private static class StringFilter extends TypeFilter<Integer>{

	public StringFilter() {
	}

	
	@Override
	public String parse(Integer input) throws TypeFilterException {
	    return input.toString();
	}
	
    }
    
    private static class ContextStub implements BundleContext{
	Object lastRegistered;
	
	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> arg0, S arg1, Dictionary<String, ?> arg2) {
	    lastRegistered = arg1;
	    return null;
	}

	@Override
	public String getProperty(String key) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Bundle getBundle() {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Bundle installBundle(String arg0, InputStream arg1) throws BundleException {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Bundle installBundle(String arg0) throws BundleException {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Bundle getBundle(long id) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Bundle[] getBundles() {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeServiceListener(ServiceListener arg0) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addBundleListener(BundleListener listener) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeBundleListener(BundleListener arg0) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeFrameworkListener(FrameworkListener arg0) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ServiceRegistration<?> registerService(String[] arg0, Object arg1, Dictionary<String, ?> arg2) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ServiceRegistration<?> registerService(String arg0, Object arg1, Dictionary<String, ?> arg2) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ServiceReference<?>[] getServiceReferences(String arg0, String arg1) throws InvalidSyntaxException {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ServiceReference<?> getServiceReference(String arg0) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <S> ServiceReference<S> getServiceReference(Class<S> arg0) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> arg0, String arg1) throws InvalidSyntaxException {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <S> S getService(ServiceReference<S> arg0) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean ungetService(ServiceReference<?> arg0) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public File getDataFile(String filename) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Bundle getBundle(String location) {
	    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
    }
    
}
