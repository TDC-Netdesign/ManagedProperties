/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
 * @author azem
 */
public class ManagedPropertiesTest {

    final org.slf4j.Logger logger = LoggerFactory.getLogger(ManagedPropertiesTest.class);
    public TestManagedProperties props;
    Lock r = null;

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
     *
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
    public void testUpdate() throws ConfigurationException, Exception {
        Dictionary<String, Object> newConfig = new Hashtable<>();
        newConfig.put("String", Collections.singletonList("Stringval"));
        newConfig.put("Integer", Collections.singletonList(1));
        newConfig.put("Double", Collections.singletonList(2.2d));
        newConfig.put("Password", Collections.singletonList(new Character[]{'t', 'e', 's', 't'}));
        newConfig.put("otherString", Collections.singletonList("otherStringVal"));
        newConfig.put("otherInteger", Collections.singletonList(14));
        newConfig.put("otherDouble", Collections.singletonList(2.6d));
        newConfig.put("IntegerToString", Collections.singletonList(16));
        props.updated(newConfig);
        assertTrue("Did not match", props.getString() == null || props.getString().equals("Stringval"));
        Thread.sleep(20);
        assertTrue("Did not match", props.getInteger().equals(1));
        assertTrue("Did not match", props.getDouble().equals(2.2d));
        assertTrue("Did not match", Arrays.deepEquals(props.getPassword(), new Character[]{'t', 'e', 's', 't'}));
        assertTrue("Did not match", props.getRenamedString().equals("otherStringVal"));
        assertTrue("Did not match", props.getRenamedInteger().equals(14));
        assertTrue("Did not match", props.getRenamedDouble().equals(2.6d));
        assertTrue("Did not match", props.getIntegerToString().equals("16"));

    }

    @Test
    public void testOffline() throws ConfigurationException {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("String", "testString");
        defaults.put("Integer", 1);
        defaults.put("Double", 1d);
        defaults.put("Password", new Character[]{'t', 'e', 's', 't'});
        defaults.put("otherString", "othertestString");
        defaults.put("otherInteger", 11);
        defaults.put("otherDouble", 1.2d);
        defaults.put("IntegerToString", 22);
        props = new TestManagedProperties(defaults, "TestManagedProps", "TestManagedPropsID", "TestManagedPropsDesc", null);
        assertEquals(props.getString(), "testString");
        assertEquals(props.getInteger(), new Integer(1));
        assertEquals(props.getDouble(), new Double(1d));
        assertArrayEquals(props.getPassword(), new Character[]{'t', 'e', 's', 't'});
        assertEquals(props.getRenamedString(), "othertestString");
        assertEquals(props.getRenamedInteger(), new Integer(11));
        assertEquals(props.getRenamedDouble(), new Double(1.2d));
        assertEquals(props.getIntegerToString(), "22");

    }

    @Test
    public void testLock() throws InterruptedException, ConfigurationException {

        String pass;
        final Dictionary<String, Object> newProps = new Hashtable<>();
        final Dictionary<String, Object> mainProps = new Hashtable<>();
        mainProps.put("name", Collections.singletonList("nameMain"));
        mainProps.put("pass", Collections.singletonList("passMain"));
        newProps.put("name", Collections.singletonList("nameSecondThread"));
        newProps.put("pass", Collections.singletonList("passSecondTread"));

        props.updated(mainProps);
        Runnable thread = new Runnable() {
            public void run() {
                try {
                    props.updated(newProps);
                } catch (ConfigurationException ex) {
                    logger.error("ConfigurationException in " + ManagedPropertiesTest.class.getName(), ex);
                }
            }
        };
        try {
            r = props.getReadLock();
            new Thread(thread).start();
            Thread.sleep(20);
            pass = props.getpass();
        } finally {
            r.unlock();
        }
        assertEquals("passMain", pass);
        assertEquals("passSecondTread", props.getpass());

    }
    
    @Test
    public void testCallback() throws Exception {
	ScheduledExecutorService service = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	TestCallback callback1 = new TestCallback();
	TestCallback callback2 = new TestCallback();
	TestCallback callback3 = new TestCallback();
	TestReadThread reader1 = new TestReadThread();
	TestReadThread reader2 = new TestReadThread();
	TestReadThread reader3 = new TestReadThread();
	
	callback1.monitor(callback2, callback3).monitor(reader1, reader2 ,reader3);
	callback2.monitor(callback1, callback3).monitor(reader1, reader2 ,reader3);
	callback3.monitor(callback1, callback2).monitor(reader1, reader2 ,reader3);
	
	reader1.monitor(callback1, callback2, callback3);
	reader2.monitor(callback1, callback2, callback3);
	reader3.monitor(callback1, callback2, callback3);
	
	props.addConfigurationCallback(callback1);
	props.addConfigurationCallback(callback2);
	props.addConfigurationCallback(callback3);
	
	service.scheduleWithFixedDelay(reader1, 0, 2, TimeUnit.MILLISECONDS);
	service.scheduleWithFixedDelay(reader2, 0, 2, TimeUnit.MILLISECONDS);
	service.scheduleWithFixedDelay(reader3, 0, 2, TimeUnit.MILLISECONDS);
	
	long startTime = System.currentTimeMillis();
	while(System.currentTimeMillis() < startTime + 10000){
	    Dictionary dict = new Hashtable<>();
	    dict.put("Integer", Collections.singletonList(2));
	    props.updated(dict);
	}
	service.shutdown();
	service.awaitTermination(10, TimeUnit.SECONDS);
	callback1.check();
	callback2.check();
	callback3.check();
	
	reader1.check();
	reader2.check();
	reader3.check();
	
	assertNotEquals(Collections.emptyList(), callback1.updates);
	assertNotEquals(Collections.emptyList(), callback2.updates);
	assertNotEquals(Collections.emptyList(), callback3.updates);
    }

    @Test
    public void threadsSynchronization() throws InterruptedException, ConfigurationException {

        final Dictionary<String, Object> mainProps = new Hashtable<>();
        mainProps.put("name", Collections.singletonList("nameMain"));
        mainProps.put("pass", Collections.singletonList("passMain"));

        props.updated(mainProps);
        Runnable t1 = new Runnable() {
            public void run() {
                Boolean conf = true;
                try {

                    long startTime = System.currentTimeMillis();
                    List<Dictionary<String, Object>> propsList = new ArrayList<Dictionary<String, Object>>();
                    Dictionary<String, Object> tempProps = new Hashtable<>();
                    Random random = new Random();
                    char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
                    StringBuilder sb = new StringBuilder();

                    while ((conf) && (!Thread.currentThread().isInterrupted()) && (System.currentTimeMillis() < startTime + 100)) {

                        for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 6; j++) {
                                char c = chars[random.nextInt(chars.length)];
                                sb.append(c);
                            }

                            tempProps.put("name", Collections.singletonList("name" + sb));
                            tempProps.put("pass", Collections.singletonList("pass" + sb));

                            propsList.add(tempProps);
                            props.updated(tempProps);
                            sb.delete(0, 6);
                            Thread.sleep(100);
                        }
                    }
                    synchronized (this) {
                        notifyAll();
                    }
                } catch (ConfigurationException ex) {
                    conf = false;
                    logger.error("ConfigurationException in " + ManagedPropertiesTest.class.getName(), ex);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.error("failed during sleep in " + Thread.currentThread().getName(), ex);
                }
            }
        };

        Thread thread1 = new Thread(t1);
        thread1.start();
        CheckReadingCorrectValue thread2 = new CheckReadingCorrectValue();
        thread2.start();
        CheckReadingFromSameSet thread3 = new CheckReadingFromSameSet();
        thread3.start();
        synchronized (this) {
            thread1.join();
            thread2.join();
            thread3.join();
        }
        assertEquals(true, thread2.resultOfReading);
        assertEquals(true, thread3.resultReadingFromSameSet);
    }

    class TestManagedProperties extends ManagedProperties {

        public TestManagedProperties(String name, String id, String description) {
            super(name, id, description);
        }

        public TestManagedProperties(Map<String, Object> defaults, String name, String id, String description, File iconFile) {
            super(defaults, name, id, description, iconFile);
        }

        @Property
        public String getString() {
            return get("String", String.class, "false");
        }

        @Property
        public Integer getInteger() {
            return get("Integer", Integer.class, "false");
        }

        @Property
        public Double getDouble() {
            return get("Double", Double.class, "false");
        }

        @Property
        public Character[] getPassword() {
            return get("Password", Character[].class, "false");
        }

        @Property(name = "otherString")
        public String getRenamedString() {
            return get("otherString", String.class, "false");
        }

        @Property(name = "otherInteger")
        public Integer getRenamedInteger() {
            return get("otherInteger", Integer.class, "false");
        }

        @Property(name = "otherDouble")
        public Double getRenamedDouble() {
            return get("otherDouble", Double.class, "false");
        }

        @Property(type = Integer.class)
        public String getIntegerToString() {
            return get("IntegerToString", Integer.class, "false").toString();
        }

        @Property(name = "name")
        public String getname() {
            return get("name", String.class, "false");
        }

        @Property(name = "pass")
        public String getpass() {
            return get("pass", String.class, "false");
        }

    }

    /**
     * The CheckReadingFromSameSet class is used to check if Threads parameters
     * are coming the same Configuration set or not, using a flag to show result
     *
     */
    class CheckReadingFromSameSet extends Thread {

        boolean resultReadingFromSameSet = true;

        public void run() {
            long startTime = System.currentTimeMillis();
            while ((!Thread.currentThread().isInterrupted()) && System.currentTimeMillis() < startTime + 1000) {
                try {
                    r = props.getReadLock();
                    String name = props.getname();
                    Thread.sleep(100);
                    String pass = props.getpass();
                    if (!name.substring(4, 9).equals(pass.substring(4, 9))) {
                        resultReadingFromSameSet = false;
                        logger.error(Thread.currentThread().getName() + "  name and pass come from the different props set");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.error("Error in sleeping time " + Thread.currentThread().getName(), ex);
                } finally {
                    r.unlock();
                }
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /**
     * The checkReadingCorrectValue class is used to check the parameter value
     * (getName) for Thread not be wrong or null
     *
     */
    class CheckReadingCorrectValue extends Thread {

        boolean resultOfReading = true;

        public void run() {
            long startTime = System.currentTimeMillis();
            while ((!Thread.currentThread().isInterrupted()) && System.currentTimeMillis() < startTime + 1000) {
                if (props.getname().isEmpty()) {
                    resultOfReading = false;
                } else {
                    if (!props.getname().substring(0, 4).equals("name")) {
                        resultOfReading = false;
                    }
                }
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }
    
    class TestCallback implements ConfigurationCallback{
	final List<Map<String, ?>> updates = new ArrayList<>();
	boolean working = false;
	final List<TestCallback> monitoredCallbacks = new ArrayList<>();
	final List<TestReadThread> monitoredReaders = new ArrayList<>();
	final List<Exception> exceptions = new ArrayList<>();

	@Override
	public void configurationUpdated(Map<String, ?> newProperties) {
	    try{
	    working = true;
	    updates.add(newProperties);
	    try {
		for(TestCallback callback : monitoredCallbacks){
		    if(callback.working){
			exceptions.add(new Exception("Callback "+callback+" was working while "+this+" was running"));
		    }
		}
		for(TestReadThread reader : monitoredReaders){
		    if(reader.working){
			exceptions.add(new Exception("Reader "+reader+" was working while "+this+" was running"));
		    }
		}
		Thread.sleep(40);
	    } catch (InterruptedException ex) {
		updates.clear();
		exceptions.add(ex);
	    }

	    }catch(Exception ex){
		exceptions.add(ex);
	    }finally{
		working = false;
	    }
	}
	
	public TestCallback monitor(TestCallback... monitoredObjects){
	    monitoredCallbacks.clear();
	    monitoredCallbacks.addAll(Arrays.asList(monitoredObjects));
	    return this;
	}
	
	public TestCallback monitor(TestReadThread... monitoredObjects){
	    monitoredCallbacks.clear();
	    monitoredReaders.addAll(Arrays.asList(monitoredObjects));
	    return this;
	}
	
	public void check() throws Exception{
	    if(!exceptions.isEmpty()){
		throw exceptions.get(0);
	    }
	}
    }
    
    class TestReadThread extends Thread{
	boolean working = false;
	final List<TestCallback> monitoredCallbacks = new ArrayList<>();
	final List<Exception> exceptions = new ArrayList<>();
	
	
	public TestReadThread() {
	}

	@Override
	public void run() {
	    Lock lock = props.getReadLock();
	    try{
		working = true;
		for(TestCallback callback : monitoredCallbacks){
		    if(callback.working){
			exceptions.add(new Exception("Callback "+callback+" was working while "+this+" was running"));
		    }
		}
		props.getInteger();
		Thread.sleep(15);
		working = false;
	    }catch(Exception ex){
		exceptions.add(ex);
	    }finally{
		working = false;
		lock.unlock();
	    }
	}
	
	public TestReadThread monitor(TestCallback... monitoredObjects){
	    monitoredCallbacks.clear();
	    monitoredCallbacks.addAll(Arrays.asList(monitoredObjects));
	    return this;
	}
	
	public void check() throws Exception{
	    if(!exceptions.isEmpty()){
		throw exceptions.get(0);
	    }
	}
    }
    
}
