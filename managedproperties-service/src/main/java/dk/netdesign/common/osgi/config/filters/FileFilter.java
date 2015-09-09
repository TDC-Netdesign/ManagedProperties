/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config.filters;

import dk.netdesign.common.osgi.config.service.TypeFilter;
import dk.netdesign.common.osgi.config.exception.TypeFilterException;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author mnn
 */
public class FileFilter extends TypeFilter<String> {

    @Override
    public File parse(String input) throws TypeFilterException {
	File f = new File(input);
	try {
	    f.getCanonicalPath();
	} catch (IOException ex) {
	    throw new TypeFilterException("Could not read the file '" + input + "'. Could not resolve path: " + ex.getMessage(), ex);
	}
	return f;
    }

}
