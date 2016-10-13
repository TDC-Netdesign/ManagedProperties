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

package dk.netdesign.common.osgi.config.service;

import dk.netdesign.common.osgi.config.ManagedPropertiesController;
import dk.netdesign.common.osgi.config.exception.ControllerPersistenceException;
import dk.netdesign.common.osgi.config.exception.InvalidTypeException;

/**
 *
 * @author mnn
 */
public interface ControllerPersistenceProvider {
    
    public ManagedPropertiesController getController(Class typeToRetrieve) throws ControllerPersistenceException, InvalidTypeException;
    
    public void persistController(Class configurationType, ManagedPropertiesController controller) throws ControllerPersistenceException, InvalidTypeException;
    
}
