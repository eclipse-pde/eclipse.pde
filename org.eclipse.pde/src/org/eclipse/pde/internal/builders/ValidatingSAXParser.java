package org.eclipse.pde.internal.builders;

import java.io.IOException;
import java.net.URL;

import org.apache.xerces.parsers.SAXParser;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.PDECore;
import org.xml.sax.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ValidatingSAXParser {
	private SAXParser parser;
	public ValidatingSAXParser() {
		parser = new SAXParser();
		try {
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.setFeature(
				"http://apache.org/xml/features/validation/dynamic",
				true);
		} catch (SAXException e) {
			PDE.log(e);
		}
	}
	
	public SAXParser getParser() {
		return parser;
	}
	
	public void setErrorHandler(ErrorHandler handler) {
		parser.setErrorHandler(handler);
	}

	public void parse(InputSource inputSource) throws SAXException, IOException {
		URL dtdLocation = PDECore.getDefault().getDescriptor().getInstallURL();
		inputSource.setSystemId(dtdLocation.toString());
		parser.parse(inputSource);
	}
}
