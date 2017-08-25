package com.aug3.storage.redisclient.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * @author Roger.xia
 * 
 */
public class EntityResolver implements org.xml.sax.EntityResolver {

	private static final Logger LOG = Logger.getLogger(EntityResolver.class);

	@Override
	public InputSource resolveEntity(String publicId, String systemId)
			throws SAXException, IOException {
		File file = new File(systemId);

		// xsd file path can be add here
		String fileName = file.getName();

		File DTDFile = new File(fileName);
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(DTDFile);
		} catch (FileNotFoundException ex) {
			LOG.warn("Could not find the XML file " + fileName);
			return null;
		}
		return new InputSource(fileReader);
	}

}
