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
package dk.netdesign.common.osgi.config.test.properties;

import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import java.io.File;

/**
 *
 * @author mnn
 */
@PropertyDefinition(id="SetterConfig", name="SetterConfig")
public interface SetterConfig {
    
    @Property
    public String getString();
    
    public void setString(String string);
    
    @Property
    public File getFile();
    
    public void setFile(String fileAsString);
    public void setFile(File file);
    
}
