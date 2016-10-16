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

package dk.netdesign.common.osgi.config.exception;

/**
 *
 * @author mnn
 */
public class ParsingException extends ManagedPropertiesException{
    private String key;

    public ParsingException(String key, String message) {
	super(message);
	this.key = key;
    }

    public ParsingException(String key, String message, Throwable cause) {
	super(message, cause);
	this.key = key;
    }

    public ParsingException(String key, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
	this.key = key;
    }

    public ParsingException(String message) {
	super(message);
    }

    public ParsingException(String message, Throwable cause) {
	super(message, cause);
    }

    public ParsingException(Throwable cause) {
	super(cause);
    }

    public ParsingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String getMessage() {
	StringBuilder builder = new StringBuilder();
	builder.append("Error parsing configuration");
	if(key != null){
	    builder.append(" on key '").append(key).append("'");
	}
	String message = super.getMessage();
	if(message != null){
	    builder.append(": ").append(message);
	}
	return builder.toString();
    }

    public String getKey() {
	return key;
    }
    
    

    
    
    
}
