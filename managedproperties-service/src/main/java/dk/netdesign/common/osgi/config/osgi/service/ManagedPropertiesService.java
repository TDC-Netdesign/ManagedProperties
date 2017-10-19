/*
 * Copyright 2016 mnn.
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

package dk.netdesign.common.osgi.config.osgi.service;

import dk.netdesign.common.osgi.config.exception.ControllerPersistenceException;
import dk.netdesign.common.osgi.config.exception.DoubleIDException;
import dk.netdesign.common.osgi.config.exception.InvalidMethodException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;
import dk.netdesign.common.osgi.config.exception.InvocationException;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import org.osgi.framework.BundleContext;

/**
 *
 * @author mnn
 */
public interface ManagedPropertiesService {
    
    public <T extends Object> T register(Class<T> type, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException;
    
    public <I, T extends I> I register(Class<I> type, T defaults, BundleContext context) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException;
    
    public <T extends Object> T register(Class<T> type) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException;
    
    public <I, T extends I> I register(Class<I> type, T defaults) throws InvalidTypeException, TypeFilterException, DoubleIDException, InvalidMethodException, InvocationException, ControllerPersistenceException;
    
}
