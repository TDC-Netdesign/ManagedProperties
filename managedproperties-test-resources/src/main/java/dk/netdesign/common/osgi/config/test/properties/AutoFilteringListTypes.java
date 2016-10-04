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

package dk.netdesign.common.osgi.config.test.properties;

import dk.netdesign.common.osgi.config.annotation.Property;
import dk.netdesign.common.osgi.config.annotation.PropertyDefinition;
import dk.netdesign.common.osgi.config.filters.FileFilter;
import dk.netdesign.common.osgi.config.filters.URLFilter;
import java.io.File;
import java.net.URL;
import java.util.List;

/**
 *
 * @author mnn
 */
@PropertyDefinition(name = "AutoFilteringListTypes", id = "AutoFilteringListTypes")
public interface AutoFilteringListTypes {
    
    @Property(type = String.class, typeMapper = FileFilter.class)
    public List<File> getFiles();
    @Property(type = String.class, typeMapper = URLFilter.class)
    public List<URL> getURLs();
    
}
