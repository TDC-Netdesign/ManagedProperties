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
import java.math.BigDecimal;

/**
 *
 * @author mnn
 */
public class StringToBigDecimalFilter extends TypeFilter<String, BigDecimal> {

    @Override
    public BigDecimal parse(String input) throws TypeFilterException {
	try{
	    return new BigDecimal(input);
	}catch(NumberFormatException ex){
	    throw new TypeFilterException("Could parse the string "+input+" to a BigDecimal", ex);
	}
    }

    @Override
    public String revert(BigDecimal input) throws TypeFilterException {
        return input.toString();
    }
    
    
    
    
    
}
