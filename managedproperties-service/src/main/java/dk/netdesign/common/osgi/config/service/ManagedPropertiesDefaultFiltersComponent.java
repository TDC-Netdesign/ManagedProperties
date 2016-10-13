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

package dk.netdesign.common.osgi.config.service;

import dk.netdesign.common.osgi.config.filters.FileFilter;
import dk.netdesign.common.osgi.config.filters.StringToBase10BigIntegerFilter;
import dk.netdesign.common.osgi.config.filters.StringToBigDecimalFilter;
import dk.netdesign.common.osgi.config.filters.StringToBooleanFilter;
import dk.netdesign.common.osgi.config.filters.StringToByteFilter;
import dk.netdesign.common.osgi.config.filters.StringToCharArrayFilter;
import dk.netdesign.common.osgi.config.filters.StringToCharacterFilter;
import dk.netdesign.common.osgi.config.filters.StringToDoubleFilter;
import dk.netdesign.common.osgi.config.filters.StringToFloatFilter;
import dk.netdesign.common.osgi.config.filters.StringToIntegerFilter;
import dk.netdesign.common.osgi.config.filters.StringToLongFilter;
import dk.netdesign.common.osgi.config.filters.StringToShortFilter;
import dk.netdesign.common.osgi.config.filters.URLFilter;
import java.util.ArrayList;
import java.util.List;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author mnn
 */
@Component(service = DefaultFilterProvider.class, immediate = true)
public class ManagedPropertiesDefaultFiltersComponent implements DefaultFilterProvider{

    @Override
    public List<Class<? extends TypeFilter>> getFilters() {
	List<Class<? extends TypeFilter>> toReturn = new ArrayList<>();
	toReturn.add(StringToBooleanFilter.class);
	toReturn.add(StringToByteFilter.class);
	toReturn.add(StringToDoubleFilter.class);
	toReturn.add(StringToFloatFilter.class);
	toReturn.add(StringToIntegerFilter.class);
	toReturn.add(StringToLongFilter.class);
	toReturn.add(StringToShortFilter.class);
	toReturn.add(URLFilter.class);
	toReturn.add(FileFilter.class);
	toReturn.add(StringToBase10BigIntegerFilter.class);
	toReturn.add(StringToBigDecimalFilter.class);
	toReturn.add(StringToCharArrayFilter.class);
	toReturn.add(StringToCharacterFilter.class);
	return toReturn;
    }
    
    
    
}
