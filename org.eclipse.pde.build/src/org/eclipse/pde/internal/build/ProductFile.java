/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
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
	private static final String ATTRIBUTE_AUTO_START = "autoStart"; //$NON-NLS-1$
	private static final String ATTRIBUTE_START_LEVEL = "startLevel"; //$NON-NLS-1$

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
	private static final String CONFIGURATIONS = "configurations"; //$NON-NLS-1$
	private static final String PROPERTY = "property"; //$NON-NLS-1$
	private static final String P_USE_ICO = "useIco"; //$NON-NLS-1$
	private static final String UID = "uid"; //$NON-NLS-1$

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
	private static final int STATE_CONFIG_INI = 16;
	private static final int STATE_CONFIGURATIONS = 17;

	private int state = STATE_START;

	private SAXParser parser;
	private String currentOS = null;
	private boolean useIco = false;
	private final Map iconsMap = new HashMap(6);
	private String launcherName = null;
	private String configPath = null;
	private final Map platformSpecificConfigPaths = new HashMap();
	private String configPlatform = null;
	private String platformConfigPath = null;
	private String id = null;
	private String uid = null;
	private boolean useFeatures = false;
	private Properties properties = null;
	private List entries = null;
	private Map bundleInfos = null;
	private String splashLocation = null;
	private String productName = null;
	private String application = null;
	private String location = null;
	private String version = null;

	private Properties launcherArgs = new Properties();

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
		this.location = location;
		try {
			parserFactory.setNamespaceAware(true);
			parser = parserFactory.newSAXParser();
			InputStream in = new BufferedInputStream(new FileInputStream(location));
			try {
				parser.parse(new InputSource(in), this);
			} finally {
				Utils.close(in);
			}
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

	public String getLocation() {
		return location;
	}

	public List getPlugins() {
		return getPlugins(true);
	}

	public List getPlugins(boolean includeFragments) {
		if (entries == null)
			return Collections.EMPTY_LIST;

		List plugins = new ArrayList();
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			FeatureEntry entry = (FeatureEntry) iterator.next();
			if (entry.isPlugin() && (!entry.isFragment() || includeFragments))
				plugins.add(entry.getId());
		}
		return plugins;
	}

	public List getFragments() {
		if (entries == null)
			return Collections.EMPTY_LIST;

		List fragments = new ArrayList();
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			FeatureEntry entry = (FeatureEntry) iterator.next();
			if (entry.isPlugin() && entry.isFragment())
				fragments.add(entry.getId());
		}
		return fragments;
	}

	public List getFeatures() {
		if (entries == null)
			return Collections.EMPTY_LIST;

		List features = new ArrayList();
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			FeatureEntry entry = (FeatureEntry) iterator.next();
			if (!entry.isPlugin())
				features.add(entry.getId());
		}
		return features;
	}

	public List getProductEntries() {
		if (entries == null)
			return Collections.EMPTY_LIST;
		List results = new ArrayList();
		for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
			FeatureEntry entry = (FeatureEntry) iterator.next();
			if (useFeatures() == !entry.isPlugin())
				results.add(entry);
		}
		return results;
	}

	public boolean containsPlugin(String plugin) {
		List plugins = getPlugins();
		return (plugins != null && plugins.contains(plugin));
	}

	/**
	 * Parses the specified url and constructs a feature
	 */
	public String[] getIcons() {
		return getIcons(currentOS);
	}

	public String[] getIcons(String os) {
		if (iconsMap.containsKey(os))
			return (String[]) iconsMap.get(os);
		return new String[0];
	}

	private String[] toArrayRemoveNulls(List list) {
		String[] temp = new String[list.size()];
		int i = 0;
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			String element = (String) iter.next();
			if (element != null)
				temp[i++] = element;
		}
		String[] result = new String[i];
		System.arraycopy(temp, 0, result, 0, i);
		return result;
	}

	public String getConfigIniPath() {
		return configPath;
	}

	public String getConfigIniPath(String os) {
		String specific = (String) platformSpecificConfigPaths.get(os);
		return specific == null ? configPath : specific;
	}

	public boolean haveCustomConfig() {
		return configPath != null || platformSpecificConfigPaths.size() > 0;
	}

	public String getId() {
		if (uid != null)
			return uid;
		return id;
	}

	public String getProductId() {
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

	public String getVersion() {
		return (version == null) ? "0.0.0" : version; //$NON-NLS-1$
	}

	public Map getConfigurationInfo() {
		if (bundleInfos == null)
			return Collections.EMPTY_MAP;
		return bundleInfos;
	}

	public Properties getConfigProperties() {
		if (properties == null)
			return new Properties();
		return properties;
	}

	public String getVMArguments(String os) {
		String key = null;
		if (os.equals(Platform.OS_WIN32)) {
			key = VM_ARGS_WIN;
		} else if (os.equals(Platform.OS_LINUX)) {
			key = VM_ARGS_LINUX;
		} else if (os.equals(Platform.OS_MACOSX)) {
			key = VM_ARGS_MAC;
		} else if (os.equals(Platform.OS_SOLARIS)) {
			key = VM_ARGS_SOLARIS;
		}

		String prefix = launcherArgs.getProperty(VM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args = platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
		else
			args = platform != null ? platform : ""; //$NON-NLS-1$
		return normalize(args);
	}

	public String getProgramArguments(String os) {
		String key = null;
		if (os.equals(Platform.OS_WIN32)) {
			key = PROGRAM_ARGS_WIN;
		} else if (os.equals(Platform.OS_LINUX)) {
			key = PROGRAM_ARGS_LINUX;
		} else if (os.equals(Platform.OS_MACOSX)) {
			key = PROGRAM_ARGS_MAC;
		} else if (os.equals(Platform.OS_SOLARIS)) {
			key = PROGRAM_ARGS_SOLARIS;
		}

		String prefix = launcherArgs.getProperty(PROGRAM_ARGS);
		String platform = null, args = null;
		if (key != null)
			platform = launcherArgs.getProperty(key);
		if (prefix != null)
			args = platform != null ? prefix + " " + platform : prefix; //$NON-NLS-1$
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
					state = STATE_CONFIG_INI;
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
				} else if (CONFIGURATIONS.equals(localName)) {
					state = STATE_CONFIGURATIONS;
				}
				break;

			case STATE_CONFIG_INI :
				processConfigIniPlatform(localName, true);
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
				if (PLUGIN.equals(localName)) {
					processPlugin(attributes);
				}
				break;

			case STATE_FEATURES :
				if (FEATURE.equals(localName)) {
					processFeature(attributes);
				}
				break;
			case STATE_CONFIGURATIONS :
				if (PLUGIN.equals(localName)) {
					processPluginConfiguration(attributes);
				} else if (PROPERTY.equals(localName)) {
					processPropertyConfiguration(attributes);
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

			case STATE_CONFIG_INI :
				if (CONFIG_INI.equals(localName))
					state = STATE_PRODUCT;
				else
					processConfigIniPlatform(localName, false);
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
			case STATE_CONFIG_INI :
				if (platformConfigPath != null)
					platformConfigPath += String.valueOf(ch, start, length);
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
		if (entries == null)
			entries = new ArrayList();

		String fragment = attributes.getValue(FRAGMENT);
		String pluginId = attributes.getValue(ID);
		String pluginVersion = attributes.getValue(VERSION);

		FeatureEntry entry = new FeatureEntry(pluginId, pluginVersion != null ? pluginVersion : GENERIC_VERSION_NUMBER, true);
		entry.setFragment(Boolean.valueOf(fragment).booleanValue());
		entries.add(entry);
	}

	private void processFeature(Attributes attributes) {
		if (entries == null)
			entries = new ArrayList();
		String featureId = attributes.getValue(ID);
		String featureVersion = attributes.getValue(VERSION);
		entries.add(new FeatureEntry(featureId, featureVersion != null ? featureVersion : GENERIC_VERSION_NUMBER, false));
	}

	private void processProduct(Attributes attributes) {
		id = attributes.getValue(ID);
		uid = attributes.getValue(UID);
		productName = attributes.getValue("name"); //$NON-NLS-1$
		application = attributes.getValue("application"); //$NON-NLS-1$
		String use = attributes.getValue("useFeatures"); //$NON-NLS-1$
		if (use != null)
			useFeatures = IBuildPropertiesConstants.TRUE.equalsIgnoreCase(use);
		version = attributes.getValue(VERSION);
	}

	private void processConfigIni(Attributes attributes) {
		String path = null;
		if ("custom".equals(attributes.getValue("use"))) { //$NON-NLS-1$//$NON-NLS-2$
			path = attributes.getValue("path"); //$NON-NLS-1$
		}
		String os = attributes.getValue("os"); //$NON-NLS-1$
		if (os != null && os.length() > 0) {
			// TODO should we allow a platform-specific default to over-ride a custom generic path?
			if (path != null)
				platformSpecificConfigPaths.put(os, path);
		} else if (path != null) {
			configPath = path;
		}
	}

	private void processConfigIniPlatform(String key, boolean begin) {
		if (begin) {
			configPlatform = key;
			platformConfigPath = ""; //$NON-NLS-1$
		} else if (configPlatform.equals(key) && platformConfigPath.length() > 0) {
			platformSpecificConfigPaths.put(key, platformConfigPath);
			platformConfigPath = null;
		}
	}

	private void processLauncher(Attributes attributes) {
		launcherName = attributes.getValue("name"); //$NON-NLS-1$
	}

	private void processSolaris(Attributes attributes) {
		List result = new ArrayList(4);
		result.add(attributes.getValue(SOLARIS_LARGE));
		result.add(attributes.getValue(SOLARIS_MEDIUM));
		result.add(attributes.getValue(SOLARIS_SMALL));
		result.add(attributes.getValue(SOLARIS_TINY));
		iconsMap.put(Platform.OS_SOLARIS, toArrayRemoveNulls(result));
	}

	private void processWin(Attributes attributes) {
		useIco = IBuildPropertiesConstants.TRUE.equalsIgnoreCase(attributes.getValue(P_USE_ICO));
	}

	private void processIco(Attributes attributes) {
		if (!useIco)
			return;
		String value = attributes.getValue("path"); //$NON-NLS-1$
		if (value != null)
			iconsMap.put(Platform.OS_WIN32, new String[] {value});
	}

	private void processBmp(Attributes attributes) {
		if (useIco)
			return;
		List result = new ArrayList(7);
		result.add(attributes.getValue(WIN32_16_HIGH));
		result.add(attributes.getValue(WIN32_16_LOW));
		result.add(attributes.getValue(WIN32_24_LOW));
		result.add(attributes.getValue(WIN32_32_HIGH));
		result.add(attributes.getValue(WIN32_32_LOW));
		result.add(attributes.getValue(WIN32_48_HIGH));
		result.add(attributes.getValue(WIN32_48_LOW));
		iconsMap.put(Platform.OS_WIN32, toArrayRemoveNulls(result));
	}

	private void processLinux(Attributes attributes) {
		String value = attributes.getValue("icon"); //$NON-NLS-1$
		if (value != null)
			iconsMap.put(Platform.OS_LINUX, new String[] {value});
	}

	private void processMac(Attributes attributes) {
		String value = attributes.getValue("icon"); //$NON-NLS-1$
		if (value != null)
			iconsMap.put(Platform.OS_MACOSX, new String[] {value});
	}

	private void processPluginConfiguration(Attributes attributes) {
		String bundleId = attributes.getValue(ID);
		if (bundleId != null) {
			BundleInfo info = new BundleInfo();
			info.setSymbolicName(bundleId);
			info.setVersion(attributes.getValue(VERSION));
			String value = attributes.getValue(ATTRIBUTE_START_LEVEL);
			if (value != null)
				info.setStartLevel(Integer.parseInt(value));
			value = attributes.getValue(ATTRIBUTE_AUTO_START);
			if (value != null)
				info.setMarkedAsStarted(Boolean.valueOf(value).booleanValue());
			if (bundleInfos == null)
				bundleInfos = new HashMap();
			bundleInfos.put(bundleId, info);
		}
	}

	private void processPropertyConfiguration(Attributes attributes) {
		String name = attributes.getValue("name"); //$NON-NLS-1$
		String value = attributes.getValue("value"); //$NON-NLS-1$
		if (name == null)
			return;
		if (value == null)
			value = ""; //$NON-NLS-1$
		if (properties == null)
			properties = new Properties();
		properties.put(name, value);
	}
}
