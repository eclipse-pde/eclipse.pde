/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import java.util.*;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @since 3.1
 */
public class ProductFile extends DefaultHandler implements IPDEBuildConstants {
	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	private static final String PROGRAM_ARGS = "programArgs"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_LINUX = "programArgsLin"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_MAC = "programArgsMac"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_SOLARIS = "programArgsSol"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS_WIN = "programArgsWin"; //$NON-NLS-1$
	private static final String VM_ARGS = "vmArgs"; //$NON-NLS-1$
	private static final String VM_ARGS_LINUX = "vmArgsLin"; //$NON-NLS-1$
	private static final String VM_ARGS_MAC = "vmArgsMac"; //$NON-NLS-1$
	private static final String VM_ARGS_SOLARIS = "vmArgsSol"; //$NON-NLS-1$
	private static final String VM_ARGS_WIN = "vmArgsWin"; //$NON-NLS-1$
	
	private static final String SOLARIS_LARGE = "solarisLarge"; //$NON-NLS-1$
	private static final String SOLARIS_MEDIUM = "solarisMedium"; //$NON-NLS-1$
	private static final String SOLARIS_SMALL = "solarisSmall"; //$NON-NLS-1$
	private static final String SOLARIS_TINY = "solarisTiny"; //$NON-NLS-1$
	private static final String WIN32_16_LOW = "winSmallLow"; //$NON-NLS-1$
	private static final String WIN32_16_HIGH = "winSmallHigh"; //$NON-NLS-1$
	private static final String WIN32_24_LOW = "win24Low"; //$NON-NLS-1$
	private static final String WIN32_32_LOW = "winMediumLow"; //$NON-NLS-1$
	private static final String WIN32_32_HIGH = "winMediumHigh"; //$NON-NLS-1$
	private static final String WIN32_48_LOW = "winLargeLow"; //$NON-NLS-1$
	private static final String WIN32_48_HIGH = "winLargeHigh"; //$NON-NLS-1$
	
	private static final String PRODUCT = "product"; //$NON-NLS-1$
	private static final String CONFIG_INI = "configIni"; //$NON-NLS-1$
	private static final String LAUNCHER = "launcher"; //$NON-NLS-1$
	private static final String LAUNCHER_ARGS = "launcherArgs"; //$NON-NLS-1$
	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String SPLASH = "splash"; //$NON-NLS-1$
	private static final String P_USE_ICO = "useIco"; //$NON-NLS-1$
	
	//These constants form a small state machine to parse the .product file
	private static final int STATE_START = 0;
	private static final int STATE_PRODUCT = 1;
	private static final int STATE_LAUNCHER = 2;
	private static final int STATE_LAUNCHER_ARGS = 3;
	private static final int STATE_PLUGINS = 4;
	private static final int STATE_FEATURES = 5;
	private static final int STATE_PROGRAM_ARGS = 6;
	private static final int STATE_PROGRAM_ARGS_LINUX = 7;
	private static final int STATE_PROGRAM_ARGS_MAC = 8;
	private static final int STATE_PROGRAM_ARGS_SOLARIS = 9;
	private static final int STATE_PROGRAM_ARGS_WIN = 10;
	private static final int STATE_VM_ARGS = 11;
	private static final int STATE_VM_ARGS_LINUX = 12;
	private static final int STATE_VM_ARGS_MAC = 13;
	private static final int STATE_VM_ARGS_SOLARIS = 14;
	private static final int STATE_VM_ARGS_WIN = 15;
	
	private int state = STATE_START;

	private SAXParser parser;
	private String currentOS = null;
	private boolean useIco = false;
	private ArrayList result = new ArrayList(6);
	private String launcherName = null;
	private String icons[] = null;
	private String configPath = null;
	private String id = null;
	private boolean useFeatures = false;
	private List plugins = null;
	private List fragments = null;
	private List features = null;
	private String splashLocation = null;
	private String productName = null;
	private String application = null;

	private Properties  launcherArgs = new Properties();
	
	private static String normalize(String text) {
		if (text == null || text.trim().length() == 0)
			return ""; //$NON-NLS-1$

		text = text.replaceAll("\\r|\\n|\\f|\\t", " "); //$NON-NLS-1$ //$NON-NLS-2$
		return text.replaceAll("\\s+", " "); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Constructs a feature parser.
	 */
	public ProductFile(String location, String os) throws CoreException {
		super();
		this.currentOS = os;
		try {
			parserFactory.setNamespaceAware(true);
			parser = parserFactory.newSAXParser();
			InputStream in = new FileInputStream(location);
			parser.parse(new InputSource(in), this);
		} catch (ParserConfigurationException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.exception_productParse, location), e));
		} catch (SAXException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.exception_productParse, location), e));
		} catch (FileNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FILE, NLS.bind(Messages.exception_missingElement, location), null));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PRODUCT_FORMAT, NLS.bind(Messages.exception_productParse, location), e));
		}
	}

	public String getLauncherName() {
		return launcherName;
	}
	
	public List getPlugins() {
		return getPlugins(true);
	}
	
	public List getPlugins(boolean includeFragments) {
		List p = plugins != null ? plugins : Collections.EMPTY_LIST;
		if(!includeFragments)
			return p;
		
		List f = fragments != null ? fragments : Collections.EMPTY_LIST;
		int size = p.size() + f.size();
		if (size == 0)
			return Collections.EMPTY_LIST;

		List both = new ArrayList(size);
		both.addAll(p);
		both.addAll(f);
		return both;
	}
	
	public List getFragments() {
		if(fragments == null)
			return Collections.EMPTY_LIST;
		return fragments;
	}
	
	public List getFeatures() {
		if(features == null)
			return Collections.EMPTY_LIST;
		return features;
	}
	
	public boolean containsPlugin(String plugin) {
		return (plugins != null && plugins.contains(plugin)) || (fragments != null && fragments.contains(plugin));
	}

	/**
	 * Parses the specified url and constructs a feature
	 */
	public String[] getIcons() {
		if (icons != null)
			return icons;
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

	public String getConfigIniPath() {
		return configPath;
	}

	public String getId() {
		return id;
	}
	
	public String getSplashLocation() {
		return splashLocation;
	}

	public String getProductName() {
		return productName;
	}
	
	public String getApplication() {
		return application;
	}
	
	public boolean useFeatures() {
		return useFeatures;
	}

	public String getVMArguments(String os) {
		String key = null;
		if( os.equals(Platform.OS_WIN32)){
			key = VM_ARGS_WIN;
		} else if( os.equals(Platform.OS_LINUX)) {
			key = VM_ARGS_LINUX;
		} else if( os.equals(Platform.OS_MACOSX)) {
			key = VM_ARGS_MAC;
		} else if(os.equals(Platform.OS_SOLARIS)) {
			key = VM_ARGS_SOLARIS;
		}
		
		String prefix = launcherArgs.getProperty(VM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args =  platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
		else 
			args = platform != null ? platform : ""; //$NON-NLS-1$
		return normalize(args);
	}
	
	public String getProgramArguments(String os) {
		String key = null;
		if( os.equals(Platform.OS_WIN32)){
			key = PROGRAM_ARGS_WIN;
		} else if( os.equals(Platform.OS_LINUX)) {
			key = PROGRAM_ARGS_LINUX;
		} else if( os.equals(Platform.OS_MACOSX)) {
			key = PROGRAM_ARGS_MAC;
		} else if(os.equals(Platform.OS_SOLARIS)) {
			key = PROGRAM_ARGS_SOLARIS;
		}
		
		String prefix = launcherArgs.getProperty(PROGRAM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args =  platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
		else 
			args = platform != null ? platform : ""; //$NON-NLS-1$
		return normalize(args);
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		switch (state) {
			case STATE_START :
				if (PRODUCT.equals(localName)) {
					processProduct(attributes);
					state = STATE_PRODUCT;
				}
				break;

			case STATE_PRODUCT :
				if (CONFIG_INI.equals(localName)) {
					processConfigIni(attributes);
				} else if (LAUNCHER.equals(localName)) {
					processLauncher(attributes);
					state = STATE_LAUNCHER;
				} else if (PLUGINS.equals(localName)) {
					state = STATE_PLUGINS;
				} else if (FEATURES.equals(localName)) {
					state = STATE_FEATURES;
				} else if (LAUNCHER_ARGS.equals(localName)) {
					state = STATE_LAUNCHER_ARGS;
				} else if (SPLASH.equals(localName)) {
					splashLocation = attributes.getValue("location"); //$NON-NLS-1$
				}
				break;

			case STATE_LAUNCHER :
				if (Platform.OS_SOLARIS.equals(localName)) {
					processSolaris(attributes);
				} else if ("win".equals(localName)) { //$NON-NLS-1$
					processWin(attributes);
				} else if (Platform.OS_LINUX.equals(localName)) {
					processLinux(attributes);
				} else if (Platform.OS_MACOSX.equals(localName)) {
					processMac(attributes);
				}
				if ("ico".equals(localName)) { //$NON-NLS-1$
					processIco(attributes);
				} else if ("bmp".equals(localName)) { //$NON-NLS-1$
					processBmp(attributes);
				}
				break;

			case STATE_LAUNCHER_ARGS :
				if (PROGRAM_ARGS.equals(localName)) {
					state = STATE_PROGRAM_ARGS;
				} else if (PROGRAM_ARGS_LINUX.equals(localName)) {
					state = STATE_PROGRAM_ARGS_LINUX;
				} else if (PROGRAM_ARGS_MAC.equals(localName)) {
					state = STATE_PROGRAM_ARGS_MAC;
				} else if (PROGRAM_ARGS_SOLARIS.equals(localName)) {
					state = STATE_PROGRAM_ARGS_SOLARIS;
				} else if (PROGRAM_ARGS_WIN.equals(localName)) {
					state = STATE_PROGRAM_ARGS_WIN;
				} else if (VM_ARGS.equals(localName)) {
					state = STATE_VM_ARGS;
				} else if (VM_ARGS_LINUX.equals(localName)) {
					state = STATE_VM_ARGS_LINUX;
				} else if (VM_ARGS_MAC.equals(localName)) {
					state = STATE_VM_ARGS_MAC;
				} else if (VM_ARGS_SOLARIS.equals(localName)) {
					state = STATE_VM_ARGS_SOLARIS;
				} else if (VM_ARGS_WIN.equals(localName)) {
					state = STATE_VM_ARGS_WIN;
				}
				break;

			case STATE_PLUGINS :
				if ("plugin".equals(localName)) { //$NON-NLS-1$
					processPlugin(attributes);
				}
				break;

			case STATE_FEATURES :
				if ("feature".equals(localName)) { //$NON-NLS-1$
					processFeature(attributes);
				}
				break;
		}
	}

	public void endElement(String uri, String localName, String qName) {
		switch (state) {
			case STATE_PLUGINS :
				if (PLUGINS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_FEATURES :
				if (FEATURES.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER_ARGS :
				if (LAUNCHER_ARGS.equals(localName))
					state = STATE_PRODUCT;
				break;
			case STATE_LAUNCHER :
				if (LAUNCHER.equals(localName))
					state = STATE_PRODUCT;
				break;

			case STATE_PROGRAM_ARGS :
			case STATE_PROGRAM_ARGS_LINUX :
			case STATE_PROGRAM_ARGS_MAC :
			case STATE_PROGRAM_ARGS_SOLARIS :
			case STATE_PROGRAM_ARGS_WIN :
			case STATE_VM_ARGS :
			case STATE_VM_ARGS_LINUX :
			case STATE_VM_ARGS_MAC :
			case STATE_VM_ARGS_SOLARIS :
			case STATE_VM_ARGS_WIN :
				state = STATE_LAUNCHER_ARGS;
				break;
		}
	}
	
	public void characters(char[] ch, int start, int length) {
		switch (state) {
			case STATE_PROGRAM_ARGS :
				addLaunchArgumentToMap(PROGRAM_ARGS, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_LINUX :
				addLaunchArgumentToMap(PROGRAM_ARGS_LINUX, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_MAC :
				addLaunchArgumentToMap(PROGRAM_ARGS_MAC, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_SOLARIS :
				addLaunchArgumentToMap(PROGRAM_ARGS_SOLARIS, String.valueOf(ch, start, length));
				break;
			case STATE_PROGRAM_ARGS_WIN :
				addLaunchArgumentToMap(PROGRAM_ARGS_WIN, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS :
				addLaunchArgumentToMap(VM_ARGS, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_LINUX :
				addLaunchArgumentToMap(VM_ARGS_LINUX, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_MAC :
				addLaunchArgumentToMap(VM_ARGS_MAC, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_SOLARIS :
				addLaunchArgumentToMap(VM_ARGS_SOLARIS, String.valueOf(ch, start, length));
				break;
			case STATE_VM_ARGS_WIN :
				addLaunchArgumentToMap(VM_ARGS_WIN, String.valueOf(ch, start, length));
				break;
		}
	}
	
	private void addLaunchArgumentToMap(String key, String value) {
		if (launcherArgs == null)
			launcherArgs = new Properties();
		
		String oldValue = launcherArgs.getProperty(key);
		if (oldValue != null)
			launcherArgs.setProperty(key, oldValue + value);
		else
			launcherArgs.setProperty(key, value);
	}
	private void processPlugin(Attributes attributes) {
		String fragment = attributes.getValue("fragment"); //$NON-NLS-1$
		if (fragment != null && new Boolean(fragment).booleanValue()) {
			if (fragments == null)
				fragments = new ArrayList();
			fragments.add(attributes.getValue("id")); //$NON-NLS-1$
		} else {
			if (plugins == null)
				plugins = new ArrayList();
			plugins.add(attributes.getValue("id")); //$NON-NLS-1$
		}
	}
	
	private void processFeature(Attributes attributes) {
		if (features == null)
			features = new ArrayList();
		features.add(attributes.getValue("id")); //$NON-NLS-1$
	}

	private void processProduct(Attributes attributes) {
		id = attributes.getValue("id"); //$NON-NLS-1$
		productName = attributes.getValue("name"); //$NON-NLS-1$
		application = attributes.getValue("application"); //$NON-NLS-1$
		String use = attributes.getValue("useFeatures"); //$NON-NLS-1$
		if (use != null)
			useFeatures = IBuildPropertiesConstants.TRUE.equalsIgnoreCase(use);
	}

	private void processConfigIni(Attributes attributes) {
		if (attributes.getValue("use").equals("custom")) { //$NON-NLS-1$//$NON-NLS-2$
			configPath = attributes.getValue("path"); //$NON-NLS-1$
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
		if (!osMatch(Platform.OS_SOLARIS))
			return;
		result.add(attributes.getValue(SOLARIS_LARGE));
		result.add(attributes.getValue(SOLARIS_MEDIUM));
		result.add(attributes.getValue(SOLARIS_SMALL));
		result.add(attributes.getValue(SOLARIS_TINY));
	}

	private void processWin(Attributes attributes) {
		if (!osMatch(Platform.OS_WIN32))
			return;
		useIco = IBuildPropertiesConstants.TRUE.equalsIgnoreCase(attributes.getValue(P_USE_ICO));
	}

	private void processIco(Attributes attributes) {
		if (!osMatch(Platform.OS_WIN32) || !useIco) 
			return;
		result.add(attributes.getValue("path")); //$NON-NLS-1$
	}

	private void processBmp(Attributes attributes) {
		if (!osMatch(Platform.OS_WIN32) || useIco) 
			return;
		result.add(attributes.getValue(WIN32_16_HIGH));
		result.add(attributes.getValue(WIN32_16_LOW));
		result.add(attributes.getValue(WIN32_24_LOW));
		result.add(attributes.getValue(WIN32_32_HIGH));
		result.add(attributes.getValue(WIN32_32_LOW));
		result.add(attributes.getValue(WIN32_48_HIGH));
		result.add(attributes.getValue(WIN32_48_LOW));
	}

	private void processLinux(Attributes attributes) {
		if (!osMatch(Platform.OS_LINUX)) 
			return;
		result.add(attributes.getValue("icon")); //$NON-NLS-1$
	}

	private void processMac(Attributes attributes) {
		if (!osMatch(Platform.OS_MACOSX)) 
			return;
		result.add(attributes.getValue("icon")); //$NON-NLS-1$
	}
}
