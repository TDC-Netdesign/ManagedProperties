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

package dk.netdesign.common.osgi.config;

import java.util.Objects;

/**
 *
 * @author mnn
 */
public class FilterReference implements Comparable<FilterReference>{
    private final Class inputType;
    private final Class outputType;

    public FilterReference(Class inputType, Class outputType) {
	this.inputType = inputType;
	this.outputType = outputType;
    }

    public Class getInputType() {
	return inputType;
    }

    public Class getOutputType() {
	return outputType;
    }

    @Override
    public int hashCode() {
	int hash = 3;
	hash = 79 * hash + Objects.hashCode(this.inputType);
	hash = 79 * hash + Objects.hashCode(this.outputType);
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final FilterReference other = (FilterReference) obj;
	if (!Objects.equals(this.inputType, other.inputType)) {
	    return false;
	}
	if (!Objects.equals(this.outputType, other.outputType)) {
	    return false;
	}
	return true;
    }

    @Override
    public int compareTo(FilterReference o) {
	Integer comp;
	comp = inputType.getCanonicalName().compareTo(o.inputType.getCanonicalName());
	if(comp != 0){
	    return comp;
	}
	comp = outputType.getCanonicalName().compareTo(o.outputType.getCanonicalName());
	return comp;
	
    }

    @Override
    public String toString() {
	return "Filter: "+inputType+"->"+outputType;
    }
    
    
    
    
    
    
    
    
}
