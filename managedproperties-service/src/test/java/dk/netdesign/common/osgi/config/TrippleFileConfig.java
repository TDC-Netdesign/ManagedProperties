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

import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.exception.ParsingException;
import dk.netdesign.common.osgi.config.filters.ExistingFileFilter;
import dk.netdesign.common.osgi.config.filters.FileFilter;
import java.io.File;

/**
 *
 * @author mnn
 */
@PropertyDefinition(id="FailingConfig", name="FailingConfig")
public interface TrippleFileConfig {
    
    @Property(type = String.class, typeMapper = ExistingFileFilter.class)
    public File getFile1();
    
    public void setFile1(String fileAsString) throws ParsingException;
    public void setFile1(File file) throws ParsingException;
    
    
     @Property(type = String.class, typeMapper = ExistingFileFilter.class)
    public File getFile2();
    
    public void setFile2(String fileAsString) throws ParsingException;
    public void setFile2(File file) throws ParsingException;
    
    @Property(type = String.class, typeMapper = FileFilter.class)
    public File getFile3();
    
    public void setFile3(String fileAsString) throws ParsingException;
    public void setFile3(File file) throws ParsingException;
    
}
