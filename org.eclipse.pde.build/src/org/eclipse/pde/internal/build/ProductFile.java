/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @since 3.1
 */
public class ProductFile extends DefaultHandler {
	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	private static final String SOLARIS_LARGE = "solarisLarge"; //$NON-NLS-1$
	private static final String SOLARIS_MEDIUM = "solarisMedium"; //$NON-NLS-1$
	private static final String SOLARIS_SMALL = "solarisSmall"; //$NON-NLS-1$
	private static final String SOLARIS_TINY = "solarisTiny"; //$NON-NLS-1$

	private static final String WIN32_16_LOW = "winSmallLow"; //$NON-NLS-1$
	private static final String WIN32_16_HIGH = "winSmallHigh"; //$NON-NLS-1$
	private static final String WIN32_32_LOW = "winMediumLow"; //$NON-NLS-1$
	private static final String WIN32_32_HIGH = "winMediumHigh"; //$NON-NLS-1$
	private static final String WIN32_48_LOW = "winLargeLow"; //$NON-NLS-1$
	private static final String WIN32_48_HIGH = "winLargeHigh"; //$NON-NLS-1$

	private static final String P_USE_ICO = "useIco"; //$NON-NLS-1$

	private SAXParser parser;
	private File location;
	private String currentOS = null;
	private boolean useIco = false;
	private ArrayList result = new ArrayList(6);
	private String launcherName = null;
	private String icons[] = null;
	private boolean parsed = false;

	/**
	 * Constructs a feature parser.
	 */
	public ProductFile(String location, String os) {
		super();
		this.location = new File(location);
		this.currentOS = os;
		try {
			parserFactory.setNamespaceAware(true);
			parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			if (BundleHelper.getDefault().isDebugging())
				System.out.println(e);	
		} catch (SAXException e) {
			if (BundleHelper.getDefault().isDebugging())
				System.out.println(e);
		}
	}

	public String getLauncherName() {
		if (parsed)
			return launcherName;
		parse();
		return launcherName;
	}
	
	/**
	 * Parses the specified url and constructs a feature
	 */
	public String[] getIcons() {
		if (icons != null)
			return icons;
		if (!parsed)
			parse();
		String[] temp = new String[result.size()];
		int i = 0;
		for (Iterator iter = result.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (element != null)
				temp[i++] = element;
		}
		icons = new String[i];
		System.arraycopy(temp, 0, icons, 0, i);
		return icons;
	}

	private void parse() {
		// mark us as parsed.  If we fail this time it is likely we will fail next time
		// so don't even bother.
		parsed = true;
		InputStream in = null;
		try {
			in = new FileInputStream(location);
			try {
				parser.parse(new InputSource(in), this);
			} catch (SAXException e) {
				//Ignore
			} finally {
				in.close();
			}
		} catch (IOException e) {
			//Ignore
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		if ("launcher".equals(localName)) { //$NON-NLS-1$
			processLauncher(attributes);
		} else if ("solaris".equals(localName)) { //$NON-NLS-1$
			processSolaris(attributes);
		} else if ("win".equals(localName)) { //$NON-NLS-1$
			processWin(attributes);
		} else if ("linux".equals(localName)) { //$NON-NLS-1$
			processLinux(attributes);
		} else if ("macosx".equals(localName)) { //$NON-NLS-1$
			processMac(attributes);
		} else if ("ico".equals(localName)) { //$NON-NLS-1$
			processIco(attributes);
		} else if ("bmp".equals(localName)) { //$NON-NLS-1$
			processBmp(attributes);
		}
	}

	private void processLauncher(Attributes attributes) {
		launcherName = attributes.getValue("name"); //$NON-NLS-1$
	}

	private boolean osMatch(String os) {
		if (os == currentOS)
			return true;
		if (os == null)
			return false;
		return os.equals(currentOS);
	}

	private void processSolaris(Attributes attributes) {
		if (!osMatch("solaris")) //$NON-NLS-1$
			return;
		result.add(attributes.getValue(SOLARIS_LARGE));
		result.add(attributes.getValue(SOLARIS_MEDIUM));
		result.add(attributes.getValue(SOLARIS_SMALL));
		result.add(attributes.getValue(SOLARIS_TINY));
	}

	private void processWin(Attributes attributes) {
		if (!osMatch("win32")) //$NON-NLS-1$
			return;
		useIco = "true".equals(attributes.getValue(P_USE_ICO)); //$NON-NLS-1$
	}

	private void processIco(Attributes attributes) {
		if (!osMatch("win32") || !useIco) //$NON-NLS-1$
			return;
		result.add(attributes.getValue("path")); //$NON-NLS-1$
	}

	private void processBmp(Attributes attributes) {
		if (!osMatch("win32") || useIco) //$NON-NLS-1$
			return;
		result.add(attributes.getValue(WIN32_16_HIGH));
		result.add(attributes.getValue(WIN32_16_LOW));
		result.add(attributes.getValue(WIN32_32_HIGH));
		result.add(attributes.getValue(WIN32_32_LOW));
		result.add(attributes.getValue(WIN32_48_HIGH));
		result.add(attributes.getValue(WIN32_48_LOW));
	}

	private void processLinux(Attributes attributes) {
		if (!osMatch("linux")) //$NON-NLS-1$
			return;
		result.add(attributes.getValue("icon")); //$NON-NLS-1$
	}

	private void processMac(Attributes attributes) {
		if (!osMatch("macosx")) //$NON-NLS-1$
			return;
		result.add(attributes.getValue("icon")); //$NON-NLS-1$
	}
}
