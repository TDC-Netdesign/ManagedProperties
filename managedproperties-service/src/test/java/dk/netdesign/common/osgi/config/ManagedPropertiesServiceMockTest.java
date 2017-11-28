/*
 * Copyright 2017 mnn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.ParsingException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.service.HandlerFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesFactory;
import dk.netdesign.common.osgi.config.service.ManagedPropertiesProvider;
import dk.netdesign.common.osgi.config.service.PropertyAccess;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.*;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author mnn
 */
@RunWith(EasyMockRunner.class)
public class ManagedPropertiesServiceMockTest {
    @Mock
    ManagedPropertiesProvider provider;
    
    HandlerFactory handlerfactory;
    ManagedPropertiesFactory factory;
    File testFile1;
    File testFile2;
    
    @Before
    public void setUp() throws Exception {
	handlerfactory = new HandlerFactory() {

	    @Override
	    public <E> ManagedPropertiesProvider getProvider(Class<? super E> configurationType, ManagedPropertiesController controller, E defaults) throws InvocationException, InvalidTypeException, InvalidMethodException, DoubleIDException {
		return provider;
	    }
	};
        
        factory = new ManagedPropertiesFactory(handlerfactory, null, null);
        
        testFile1 = new File("testFile.test");
	testFile1.createNewFile();
        
        testFile2 = new File("testFile.test");
	testFile2.createNewFile();
    }
    
    @After
    public void tearDown() throws Exception {
        testFile1.delete();
        testFile2.delete();
    }
    
    @Test
    public void TestUpdateFilteredProperties() throws Exception{
        String beginningString = "testmigenfest";
        String setString = "newString";
        
        
        Map<String, Object> expectedSetConfig = new HashMap<>();
        expectedSetConfig.put("String", setString);
        expectedSetConfig.put("File", testFile2.getCanonicalPath());
        
        /*Expect*/provider.start();
        expect(provider.getReturnType("String")).andReturn(String.class);
        expect(provider.getReturnType("File")).andReturn(String.class);
        expect(provider.getReturnType("ExistingFile")).andReturn(String.class);
        
        
        /*Expect*/provider.persistConfiguration(expectedSetConfig);
        /*Expect*/provider.stop();
        replay(provider);
        
        SetterConfig config = factory.register(SetterConfig.class);
        
        
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("String", beginningString);
        newConfig.put("File", testFile1.getCanonicalPath());
        
        PropertyAccess.configuration(config).updateConfig(newConfig);
        
        assertEquals(beginningString, config.getString());
        assertEquals(testFile1.getCanonicalFile(), config.getFile());
        
        config.setString(setString);
        config.setFile(testFile2);
        PropertyAccess.actions(config).commitProperties();
        
        assertEquals(setString, config.getString());
        assertEquals(testFile2, config.getFile());
        
        PropertyAccess.actions(config).unregisterProperties();
        
    }
    
    @Test
    public void TestUpdateProperties() throws Exception{
        String beginningString = "testmigenfest";
        String setString = "newString";
        
        
        Map<String, Object> expectedSetConfig = new HashMap<>();
        expectedSetConfig.put("String", setString);
        expectedSetConfig.put("File", testFile2.getCanonicalPath());
        
        expect(provider.getReturnType("String")).andReturn(String.class);
        expect(provider.getReturnType("File")).andReturn(String.class);
        expect(provider.getReturnType("ExistingFile")).andReturn(String.class);
        
        /*Expect*/provider.start();
        /*Expect*/provider.persistConfiguration(expectedSetConfig);
        /*Expect*/provider.stop();
        replay(provider);
        
        SetterConfig config = factory.register(SetterConfig.class);
        
        
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("String", beginningString);
        newConfig.put("File", testFile1.getCanonicalPath());
        
        PropertyAccess.configuration(config).updateConfig(newConfig);
        
        assertEquals(beginningString, config.getString());
        assertEquals(testFile1.getCanonicalFile(), config.getFile());
        
        config.setString(setString);
        config.setFile(testFile2.getCanonicalPath());
        PropertyAccess.actions(config).commitProperties();
        
        assertEquals(setString, config.getString());
        assertEquals(testFile2.getCanonicalFile(), config.getFile());
        
        PropertyAccess.actions(config).unregisterProperties();
        
        
    }
    
    @Test
    public void testInvalidFileAndRecovery() throws Exception{
        File nonExistingFile = new File(UUID.randomUUID().toString());
        File existingFile = File.createTempFile("Test", "file");
        if(nonExistingFile.exists()){
            fail("By some act of god, this file existed prior to starting the test. File must not exist in beginning of test.\n"+nonExistingFile.getCanonicalPath());
        }
        if(!existingFile.exists()){
            fail("Could not create test file. Failing test.\n"+existingFile.getCanonicalPath());
        }
        
        
        
        Map<String, Object> expectedSetConfig = new HashMap<>();
        expectedSetConfig.put("ExistingFile", existingFile.getCanonicalPath());
        
        expect(provider.getReturnType("String")).andReturn(String.class);
        expect(provider.getReturnType("File")).andReturn(String.class);
        expect(provider.getReturnType("ExistingFile")).andReturn(String.class);
        
        /*Expect*/provider.start();
        /*Expect*/provider.persistConfiguration(expectedSetConfig);
        /*Expect*/provider.stop();
        replay(provider);
        
        SetterConfig config = factory.register(SetterConfig.class);
  
        
        
        try{
            config.setExistingFile(nonExistingFile.getCanonicalPath());
            fail("Expected exception when attempting to add a nonexisting file");
        }catch(ParsingException ex){
            assertTrue(ex.getCause() instanceof TypeFilterException);
            //Expected exception all is well
        }
        
        try{
            config.setExistingFile(nonExistingFile);
            fail("Expected exception when attempting to add a nonexisting file");
        }catch(ParsingException ex){
            assertTrue(ex.getCause() instanceof TypeFilterException);
            //Expected exception all is well
        }
        
        config.setExistingFile(existingFile);
        config.setExistingFile(existingFile.getCanonicalPath());
        PropertyAccess.actions(config).commitProperties();
        
        PropertyAccess.actions(config).unregisterProperties();
        
    }
    
    @Test
    public void testInvalidFile() throws Exception{
        File nonExistingFile = new File(UUID.randomUUID().toString());
        File existingFile = File.createTempFile("Test", "file");
        if(nonExistingFile.exists()){
            fail("By some act of god, this file existed prior to starting the test. File must not exist in beginning of test.\n"+nonExistingFile.getCanonicalPath());
        }
        if(!existingFile.exists()){
            fail("Could not create test file. Failing test.\n"+existingFile.getCanonicalPath());
        }
        
        
        
        Map<String, Object> expectedSetConfig = new HashMap<>();
        expectedSetConfig.put("ExistingFile", existingFile.getCanonicalPath());
        
        expect(provider.getReturnType("String")).andReturn(String.class);
        expect(provider.getReturnType("File")).andReturn(String.class);
        expect(provider.getReturnType("ExistingFile")).andReturn(String.class);
        
        /*Expect*/provider.start();
        /*Expect*/provider.stop();
        replay(provider);
        
        SetterConfig config = factory.register(SetterConfig.class);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("ExistingFile", existingFile.getCanonicalPath());
        
        PropertyAccess.configuration(config).updateConfig(newConfig);
  

        try{
            config.setExistingFile(nonExistingFile.getCanonicalPath());
            fail("Expected exception when attempting to add a nonexisting file");
        }catch(ParsingException ex){
            assertTrue(ex.getCause() instanceof TypeFilterException);
            //Expected exception all is well
        }
        
        assertEquals(existingFile.getCanonicalFile(), config.getExistingFile());
        
        PropertyAccess.actions(config).unregisterProperties();
        
    }
    
        @Test
        public void testRevertOnError() throws Exception { 
        Map<String, Object> expectedSetConfig = new HashMap<>();
        expectedSetConfig.put("ExistingFile", testFile1.getCanonicalPath());
        
        expect(provider.getReturnType("String")).andReturn(String.class);
        expect(provider.getReturnType("File")).andReturn(String.class);
        expect(provider.getReturnType("ExistingFile")).andReturn(String.class);
        
        /*Expect*/provider.start();
        /*Expect*/provider.persistConfiguration(expectedSetConfig);
        expectLastCall().andThrow(new InvocationException("I decided to fail this time."));
        /*Expect*/provider.stop();
        replay(provider);
        
        SetterConfig config = factory.register(SetterConfig.class);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("ExistingFile", testFile2.getCanonicalPath());
        PropertyAccess.configuration(config).updateConfig(newConfig);
  
        config.setExistingFile(testFile1.getCanonicalPath());
        
        
        try{
            PropertyAccess.actions(config).commitProperties();
            fail("Expected exception when committing");
        }catch(InvocationException ex){
            //Expected exception all is well
        }
        
           
        
        assertEquals(testFile2.getCanonicalFile(), config.getExistingFile());
        
        PropertyAccess.actions(config).unregisterProperties();
        
    }
        
        
        @Test
        public void testRevert() throws Exception { 
        
        expect(provider.getReturnType("String")).andReturn(String.class);
        expect(provider.getReturnType("File")).andReturn(String.class);
        expect(provider.getReturnType("ExistingFile")).andReturn(String.class);
        
        /*Expect*/provider.start();
        /*Expect*/provider.stop();
        replay(provider);
        
        SetterConfig config = factory.register(SetterConfig.class);
        
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put("ExistingFile", testFile2.getCanonicalPath());
        PropertyAccess.configuration(config).updateConfig(newConfig);
  
        config.setExistingFile(testFile1.getCanonicalPath());
        
        PropertyAccess.actions(config).abortCommitProperties();

        assertEquals(testFile2.getCanonicalFile(), config.getExistingFile());
        
        PropertyAccess.actions(config).unregisterProperties();
        
    }
    
    
    
    
}
