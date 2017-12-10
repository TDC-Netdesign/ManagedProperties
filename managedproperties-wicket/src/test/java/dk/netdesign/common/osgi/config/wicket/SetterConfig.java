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
package dk.netdesign.common.osgi.config.wicket;

import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.exception.UnknownValueException;
import dk.netdesign.common.osgi.config.filters.ExistingFileFilter;
import dk.netdesign.common.osgi.config.filters.FileFilter;
import dk.netdesign.common.osgi.config.filters.URLFilter;
import java.io.File;
import java.net.URL;

/**
 *
 * @author mnn
 */
@PropertyDefinition(id="SetterConfig", name="SetterConfig")
public interface SetterConfig {
    
    @Property(cardinality = Property.Cardinality.Required)
    public String getString() throws UnknownValueException;
    
    public void setString(String string);
    
    @Property(type = String.class, typeMapper = FileFilter.class, defaultValue = "C:/Program Files/")
    public File getFile() throws UnknownValueException;
    
    public void setFile(String fileAsString);
    public void setFile(File file);
    
    @Property(type = String.class, typeMapper = ExistingFileFilter.class, description = "This file must exist")
    public File getExistingFile() throws UnknownValueException;
    
    public void setExistingFile(String fileAsString);
    public void setExistingFile(File file);
    
    @Property(type = String.class, typeMapper = URLFilter.class, description = "This must be an actual url")
    public URL getURL() throws UnknownValueException;
    
    public void setURL(String fileAsString);
    public void setURL(File file);
    
}
