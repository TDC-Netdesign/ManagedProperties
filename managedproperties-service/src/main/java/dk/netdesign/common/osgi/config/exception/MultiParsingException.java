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
package dk.netdesign.common.osgi.config.exception;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author mnn
 */
public class MultiParsingException extends ManagedPropertiesException{
    private final List<ParsingException> exceptions;

    public MultiParsingException(List<ParsingException> exceptions, String message) {
        super(message);
        this.exceptions = exceptions;
    }

    public List<ParsingException> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
    
    

    @Override
    public String toString() {
        StringBuilder errorBuilder = new StringBuilder();
        errorBuilder.append(super.toString());
        for(ParsingException ex : exceptions){
            errorBuilder.append("\n").append(ex.toString());
        }
        return errorBuilder.toString();
    }

    
    
    
}
