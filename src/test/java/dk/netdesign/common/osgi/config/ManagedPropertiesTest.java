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
    public CreateThread testThread;
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
    @Ignore
    @Test
    public void testOCD() {
        ObjectClassDefinition def = props.getObjectClassDefinition("TestManagedPropsID", null);
        assertEquals("TestManagedPropsID", def.getID());
        assertEquals("TestManagedProps", def.getName());
        assertEquals("TestManagedPropsDesc", def.getDescription());
    }

    @Ignore
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

        assertTrue("Did not match", props.getString().equals("Stringval"));
        assertTrue("Did not match", props.getInteger().equals(1));
        assertTrue("Did not match", props.getDouble().equals(2.2d));
        assertTrue("Did not match", Arrays.deepEquals(props.getPassword(), new Character[]{'t', 'e', 's', 't'}));
        assertTrue("Did not match", props.getRenamedString().equals("otherStringVal"));
        assertTrue("Did not match", props.getRenamedInteger().equals(14));
        assertTrue("Did not match", props.getRenamedDouble().equals(2.6d));
        assertTrue("Did not match", props.getIntegerToString().equals("16"));

    }

    @Ignore
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

    @Ignore
    @Test
    public void testforSynUpdate() throws ConfigurationException {
        props.get("PathToRedGateSQLDataCompare", String.class, "true");
        props.get("PathToRedGateSQLDataCompare", String.class, "true");
        props.get("PathToBCPCMD", String.class, "true");
        props.updated(null);
        props.get("HostForOriginalData", String.class, "false");
        props.updated(null);

    }

  
    
    @Test
    public void testforLock() throws InterruptedException, ConfigurationException {

        final Dictionary<String, Object> propsT1 = new Hashtable<>();
        final Dictionary<String, Object> propsT2 = new Hashtable<>();
        propsT1.put("name", Collections.singletonList("Azin"));
        propsT1.put("pass", Collections.singletonList("Emami"));
        propsT1.put("url", Collections.singletonList("T1Url"));
        propsT2.put("name", Collections.singletonList("Azade"));
        propsT2.put("pass", Collections.singletonList("Jafari"));
        propsT2.put("url", Collections.singletonList("T2Url"));

        props.updated(propsT1);

        Runnable t2 = new Runnable() {
            Boolean conf = true;

            public void run() {
                try {
                    if (conf) {
                        props.updated(propsT2);
                    }

                } catch (ConfigurationException ex) {
                    conf = false;
//                    logger.error("ConfigurationException in " + ManagedPropertiesTest.class.getName(), ex);
                    Logger.getLogger(ManagedPropertiesTest.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        
        String url;
        try {
            r = props.getReadLock();
            String name = props.getname();
            String pass = props.getpass();
            new Thread(t2).start();
//            Thread.sleep(5000);
            url = props.geturl();
            System.out.println(" ur1 1  : " + url);
        } finally {
            r.unlock();
        }
        assertEquals("T1Url", url);
        Thread.sleep(5000);
        url = props.geturl();
        System.out.println(" ur1 2  : " + url);
        assertEquals("T2Url", props.geturl());

    }
    
      @Test
    public void testforThreads() throws InterruptedException, ConfigurationException {

        Boolean flgAccuracyThread3 = true;
        String url;
        final Dictionary<String, Object> propsMain = new Hashtable<>();
        propsMain.put("name", Collections.singletonList("main"));
        propsMain.put("pass", Collections.singletonList("mainPass"));
        propsMain.put("url", Collections.singletonList("mainUrl"));
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
                            System.out.println("T1 add new props  " + temp.get("name") + " - - - " + temp.get("pass") + " - - - " + temp.get("url"));
                            Thread.sleep(5000);
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
        Runnable t2 = new Runnable() {
            public void run() {
                long startTime = System.currentTimeMillis();
                while ((!Thread.currentThread().isInterrupted()) && System.currentTimeMillis() < startTime + 100) {
                    props.getname();
                    System.out.println("T2 name for t2 : " + props.getname());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        logger.error("failed during sleep in " + Thread.currentThread().getName(), ex);
                    }
                }
                synchronized (this) {
                    notifyAll();
                }
            }
        };
        
        try {
            r = props.getReadLock();
            String name = props.getname(); /// main name
            String pass = props.getpass();// main pass
            Thread thread1 = new Thread(t1);
            thread1.start();
            Thread thread2 = new Thread(t2);
            thread2.start();
            CreateThread thread3 = new CreateThread();
            thread3.start();
            thread3.run();
            flgAccuracyThread3 = thread3.flgAccuracy;
            synchronized (this) {
                thread1.join();
                thread2.join();
                thread3.join();
            }
            url = props.geturl(); /// main url
            Thread.sleep(5000);
        } finally {
            r.unlock();
        }
        // check for thread 3 
        assertEquals(flgAccuracyThread3, true);
        assertEquals(url, "mainUrl");
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
// for chech T3

    class CreateThread extends Thread {

        public CreateThread() {
        }

        boolean flgAccuracy = true;

        public boolean isFlgAccuracy() {
            return flgAccuracy;
        }

        public void setFlgAccuracy(boolean flgAccuracy) {
            this.flgAccuracy = flgAccuracy;
        }

        public void run() {

            long startTime = System.currentTimeMillis();
            while ((!Thread.currentThread().isInterrupted()) && System.currentTimeMillis() < startTime + 100) {
                
                try {
                    r = props.getReadLock();
                    String name = props.getname();
                    String pass = props.getpass();
                    System.out.println("T3 name and pass for thread3 : " + props.getname() + " - - - " + props.getpass());
                    if (name.equals("main")) {
                        if (!pass.equals("mainPass")) {
                            flgAccuracy = false;
                            System.out.println("T3 - name and pass for main is diffrenet!!!");
                        }
                    } else {
                        if (!name.substring(4, 9).equals(pass.substring(4, 9))) {
                            flgAccuracy = false;
                            System.out.println("T3 - name and pass for ELSE  main is diffrenet!!!");
                        }
                    }
                } finally {
                    r.unlock();
                }
                try {
                    Thread.sleep(5000);

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    logger.error("failed during sleep in  " + Thread.currentThread().getName(), ex);
                }
            }
            synchronized (this) {
                notifyAll();
            }

        }

    }
}
