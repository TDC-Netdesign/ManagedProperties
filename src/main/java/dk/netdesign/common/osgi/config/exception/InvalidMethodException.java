/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package dk.netdesign.common.osgi.config.exception;

/**
 *
 * @author mnn
 */
public class InvalidMethodException extends Exception{

    public InvalidMethodException(String message) {
	super(message);
    }

    public InvalidMethodException(String message, Throwable cause) {
	super(message, cause);
    }

    public InvalidMethodException(Throwable cause) {
	super(cause);
    }

    public InvalidMethodException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }
    
    
    
}
