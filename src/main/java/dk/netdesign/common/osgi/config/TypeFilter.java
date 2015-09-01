/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import dk.netdesign.common.osgi.config.exception.TypeFilterException;

/**
 *
 * @author mnn
 * @param <I>
 */
public abstract class TypeFilter<I extends Object> {

    public TypeFilter() {
    }

    public abstract Object parse(I input) throws TypeFilterException;

}
