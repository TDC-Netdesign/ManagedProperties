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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The file filter parses a String into a File. The filter takes a String as input, and produces a File. The input is a String denoting a path to the
 * file in question. The Filter checks if the path has a valid format for the backing file system. It throws an exception if the path is not well formatted,
 * or if the path is not valid for the file system.
 * @author mnn
 */
public class FileFilter extends TypeFilter<String, File> {

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

    @Override
    public String revert(File input) throws TypeFilterException {
        try {
            return input.getCanonicalPath();
        } catch (IOException ex) {
            throw new TypeFilterException("Could not parse file", ex);
        }
    }
    
    

}
