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
public class UnknownValueException extends RuntimeException{

    public UnknownValueException(String message) {
	super(message);
    }

    public UnknownValueException(String message, Throwable cause) {
	super(message, cause);
    }

    public UnknownValueException(Throwable cause) {
	super(cause);
    }

    public UnknownValueException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }
    
    
    
}
