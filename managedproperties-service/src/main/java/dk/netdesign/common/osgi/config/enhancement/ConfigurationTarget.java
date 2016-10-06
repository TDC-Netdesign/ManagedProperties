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

package dk.netdesign.common.osgi.config.enhancement;

import dk.netdesign.common.osgi.config.Attribute;
import dk.netdesign.common.osgi.config.exception.ParsingException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mnn
 */
public interface ConfigurationTarget {
    
    public Map<String, Object> updateConfig(Map<String, Object> properties) throws ParsingException;
    
    public List<Attribute> getAttributes();
    
    public Class getConfigurationType();
    
    public String getID();
    public String getName();
    public String getDescription();
    public String getIconFile();
    
}
