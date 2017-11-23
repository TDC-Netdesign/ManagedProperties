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
package dk.netdesign.common.osgi.config.filters;

import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.service.TypeFilter;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author mnn
 */
public class ExistingFileFilter extends TypeFilter<String, File> {

    @Override
    public File parse(String input) throws TypeFilterException {
	File f = new File(input);
	try {
	    f.getCanonicalPath();
            if(!f.exists()){
                throw new TypeFilterException("Could not parse file. The file "+f.getCanonicalPath()+" did not exist");
            }
	} catch (IOException ex) {
	    throw new TypeFilterException("Could not read the file '" + input + "'. Could not resolve path: " + ex.getMessage(), ex);
	}
	return f;
    }

    @Override
    public String revert(File input) throws TypeFilterException {
        try {
            return input.getCanonicalPath();
        } catch (IOException ex) {
            throw new TypeFilterException("Could not parse file", ex);
        }
    }
    
}
