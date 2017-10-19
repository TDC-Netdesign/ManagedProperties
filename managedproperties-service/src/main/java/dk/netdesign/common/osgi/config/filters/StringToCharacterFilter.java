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

package dk.netdesign.common.osgi.config.filters;

import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import dk.netdesign.common.osgi.config.service.TypeFilter;

/**
 *
 * @author mnn
 */
public class StringToCharacterFilter extends TypeFilter<String, Character>{

    @Override
    public Character parse(String input) throws TypeFilterException {
	if(input.length() != 1){
	    throw new TypeFilterException("Could not parse String to Character. Input '"+input+"' did not fit into a single character");
	}
	return input.toCharArray()[0];
    }

    @Override
    public String revert(Character input) throws TypeFilterException {
        return input.toString();
    }
    
    
    
}
