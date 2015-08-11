/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import java.io.File;
import static java.lang.Math.random;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import static jdk.nashorn.internal.objects.NativeRegExp.test;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mnn
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
    public void testUpdate() throws ConfigurationException {
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
        // get configuration from Queue
        Runnable t1 = new Runnable() {
            @Override
            public void run() {
                try {
                    r = props.getReadLock();
                } finally {
                    r.unlock();
                }
            }
        };
        Thread thread1 = new Thread(t1);
        thread1.start();
        assertTrue("Did not match", props.getString().equals("Stringval"));
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

        String url;
        final Dictionary<String, Object> propsT2 = new Hashtable<>();
        final Dictionary<String, Object> propsMain = new Hashtable<>();
        propsMain.put("name", Collections.singletonList("nameMain"));
        propsMain.put("pass", Collections.singletonList("passMain"));
        propsMain.put("url", Collections.singletonList("urlMain"));
        propsT2.put("name", Collections.singletonList("nameSecondThread"));
        propsT2.put("pass", Collections.singletonList("passSecondTread"));
        propsT2.put("url", Collections.singletonList("urlSecondThread"));

        props.updated(propsMain);
        Runnable thread = new Runnable() {
            public void run() {
                try {
                    props.updated(propsT2);
                } catch (ConfigurationException ex) {
                    logger.error("ConfigurationException in " + ManagedPropertiesTest.class.getName(), ex);
                }
            }
        };
        try {
            new Thread(thread).start();
            r = props.getReadLock();
            Thread.sleep(5000);
            url = props.geturl();
        } finally {
            r.unlock();
        }
        assertEquals("urlMain", url);
        assertEquals("urlSecondThread", props.geturl());

    }

    @Test
    public void threadsSynchronization() throws InterruptedException, ConfigurationException {

        final Dictionary<String, Object> propsMain = new Hashtable<>();
        propsMain.put("name", Collections.singletonList("nameMain"));
        propsMain.put("pass", Collections.singletonList("passMain"));
        propsMain.put("url", Collections.singletonList("urlMain"));

        props.updated(propsMain);
        Runnable t1 = new Runnable() {
            public void run() {
                Boolean conf = true;
                try {
                    long startTime = System.currentTimeMillis();
                    while ((conf) && (!Thread.currentThread().isInterrupted()) && (System.currentTimeMillis() < startTime + 50)) {
                        List<Dictionary<String, Object>> docList = new ArrayList<Dictionary<String, Object>>();
                        Dictionary<String, Object> temp = new Hashtable<>();
                        Random random = new Random();
                        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < 10; i++) {
                            for (int j = 0; j < 6; j++) {
                                char c = chars[random.nextInt(chars.length)];
                                sb.append(c);
                            }
                            String output = sb.toString();
                            temp.put("name", Collections.singletonList("name" + sb));
                            temp.put("pass", Collections.singletonList("pass" + sb));
                            temp.put("url", Collections.singletonList("url" + sb));
                            sb.delete(0, 6);
                            docList.add(temp);
                            props.updated(temp);
                            System.out.println(Thread.currentThread().getName() + " add new props  " + temp.get("name") + " - - - " + temp.get("pass") + " - - - " + temp.get("url"));
                            Thread.sleep(1000);
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
        checkReadingCorrectValue thread2 = new checkReadingCorrectValue();
        thread2.start();
        thread2.run();
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

        @Property(name = "url")
        public String geturl() {
            return get("url", String.class, "false");
        }

    }

    /**
     * The CheckReadingFromSameSet class is used to check if Threads params are
     * coming the same Configuration set or not, it used of a flg to show result
     *
     * @author AZEM
     */
    class CheckReadingFromSameSet extends Thread {

        /**
         * @param resultReadingFromSameSet : flg to show if Thread name and Pass
         * are coming from the same Configuration set
         */
        boolean resultReadingFromSameSet = true;

        /**
         * sets resultReadingFromSameSet flag with checking if name and pass are
         * coming the same Configuration set
         */
        public void run() {
            long startTime = System.currentTimeMillis();
            while ((!Thread.currentThread().isInterrupted()) && System.currentTimeMillis() < startTime + 10000) {
                try {
                    r = props.getReadLock();
                    String name = props.getname();
                    Thread.sleep(2000);
                    String pass = props.getpass();
                    System.out.println(Thread.currentThread().getName() + " name and pass for thread3 : " + props.getname() + " - - - " + props.getpass());
                    if (!name.substring(4, 9).equals(pass.substring(4, 9))) {
                        resultReadingFromSameSet = false;
                        System.out.println("T3 - name and pass come from the different props set");
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
     * The checkReadingCorrectValue class is used to check the param value
     * (getName) for Thread not be wrong or null
     *
     * @author AZEM
     */
    class checkReadingCorrectValue extends Thread {
        /**
         * @param resultOfReading : flg to show getName is not null and is the
         * name param of configuration set
         */
        boolean resultOfReading = true;

        /**
         * sets resultOfReading flag with checking if name is not null and
         * getName is the name param of Configuration set
         */
        public void run() {

            long startTime = System.currentTimeMillis();
            while ((!Thread.currentThread().isInterrupted()) && System.currentTimeMillis() < startTime + 100) {

                if (props.getname().isEmpty()) {
                    resultOfReading = false;
                } else {
                    if (!props.getname().substring(0, 4).equals("name")) {
                        resultOfReading = false;
                    }
                }
                System.out.println(Thread.currentThread().getName() + "  name   : " + props.getname());
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
