/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.boot.*;
import org.eclipse.core.internal.boot.*;

public class PlatformConfiguration implements IPlatformConfiguration {

	private static PlatformConfiguration currentPlatformConfiguration = null;

	private URL configLocation;
	private HashMap sites;
	private HashMap externalLinkSites; // used to restore prior link site state
	private HashMap cfgdFeatures;
	private HashMap bootPlugins;
	private String defaultFeature;
	private long changeStamp;
	private boolean changeStampIsValid = false;
	private long lastFeaturesChangeStamp;
	private long featuresChangeStamp;
	private boolean featuresChangeStampIsValid = false;
	private long pluginsChangeStamp;
	private boolean pluginsChangeStampIsValid = false;
	private boolean transientConfig = false;
	private File cfgLockFile;
	private RandomAccessFile cfgLockFileRAF;
	private BootDescriptor runtimeDescriptor;

	private static String cmdFeature = null;
	private static String cmdApplication = null;
	private static boolean cmdInitialize = false;
	private static boolean cmdFirstUse = false;
	private static boolean cmdUpdate = false;
	private static boolean cmdNoUpdate = false;

	static boolean DEBUG = false;

	private static final String RUNTIME_PLUGIN_ID = "org.eclipse.core.runtime"; //$NON-NLS-1$

	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String CONFIG_FILE_TEMP_SUFFIX = ".tmp"; //$NON-NLS-1$
	private static final String CONFIG_FILE_BAK_SUFFIX = ".bak"; //$NON-NLS-1$
	private static final String PLUGIN_XML = "plugin.xml"; //$NON-NLS-1$
	private static final String FRAGMENT_XML = "fragment.xml"; //$NON-NLS-1$
	private static final String FEATURE_XML = "feature.xml"; //$NON-NLS-1$

	private static final String[] BOOTSTRAP_PLUGINS = { "org.eclipse.core.boot" }; //$NON-NLS-1$
	private static final String CFG_BOOT_PLUGIN = "bootstrap"; //$NON-NLS-1$
	private static final String CFG_SITE = "site"; //$NON-NLS-1$
	private static final String CFG_URL = "url"; //$NON-NLS-1$
	private static final String CFG_POLICY = "policy"; //$NON-NLS-1$
	private static final String[] CFG_POLICY_TYPE = { "USER-INCLUDE", "USER-EXCLUDE" }; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String CFG_POLICY_TYPE_UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
	private static final String CFG_LIST = "list"; //$NON-NLS-1$
	private static final String CFG_STAMP = "stamp"; //$NON-NLS-1$
	private static final String CFG_FEATURE_STAMP = "stamp.features"; //$NON-NLS-1$
	private static final String CFG_PLUGIN_STAMP = "stamp.plugins"; //$NON-NLS-1$
	private static final String CFG_UPDATEABLE = "updateable"; //$NON-NLS-1$
	private static final String CFG_LINK_FILE = "linkfile"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY = "feature"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_DEFAULT = "feature.default.id"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_ID = "id"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_PRIMARY = "primary"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_VERSION = "version"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER = "plugin-identifier"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_APPLICATION = "application"; //$NON-NLS-1$
	private static final String CFG_FEATURE_ENTRY_ROOT = "root"; //$NON-NLS-1$

	private static final String DEFAULT_FEATURE_ID = "org.eclipse.platform"; //$NON-NLS-1$
	private static final String DEFAULT_FEATURE_APPLICATION = "org.eclipse.ui.workbench"; //$NON-NLS-1$

	private static final String CFG_VERSION = "version"; //$NON-NLS-1$
	private static final String CFG_TRANSIENT = "transient"; //$NON-NLS-1$
	private static final String VERSION = "2.1"; //$NON-NLS-1$
	private static final String EOF = "eof"; //$NON-NLS-1$
	private static final int CFG_LIST_LENGTH = 10;

	private static final int DEFAULT_POLICY_TYPE = ISitePolicy.USER_EXCLUDE;
	private static final String[] DEFAULT_POLICY_LIST = new String[0];


	protected static final String RECONCILER_APP = "org.eclipse.update.core.reconciler"; //$NON-NLS-1$

	private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	private static URL installURL;

	public class SiteEntry implements IPlatformConfiguration.ISiteEntry {

		private URL url; // this is the external URL for the site
		private URL resolvedURL; // this is the resolved URL used internally
		private ISitePolicy policy;
		private boolean updateable = true;
		private ArrayList features;
		private ArrayList plugins;
		private PlatformConfiguration parent;
		private long changeStamp;
		private boolean changeStampIsValid = false;
		private long lastFeaturesChangeStamp;
		private long featuresChangeStamp;
		private boolean featuresChangeStampIsValid = false;
		private long lastPluginsChangeStamp;
		private long pluginsChangeStamp;
		private boolean pluginsChangeStampIsValid = false;
		private String linkFileName = null;

		private SiteEntry() {
		}
		private SiteEntry(URL url, ISitePolicy policy, PlatformConfiguration parent) {
			if (url == null)
				throw new IllegalArgumentException();

			if (policy == null)
				throw new IllegalArgumentException();

			if (parent == null)
				throw new IllegalArgumentException();

			this.url = url;
			this.policy = policy;
			this.parent = parent;
			this.features = null;
			this.plugins = null;
			this.resolvedURL = this.url;
			if (url.getProtocol().equals(PlatformURLHandler.PROTOCOL)) {
				try {
					resolvedURL = resolvePlatformURL(url); // 19536
				} catch (IOException e) {
					// will use the baseline URL ...
				}
			}
		}

		/*
		 * @see ISiteEntry#getURL()
		 */
		public URL getURL() {
			return url;
		}

		/*
		* @see ISiteEntry#getSitePolicy()
		*/
		public ISitePolicy getSitePolicy() {
			return policy;
		}

		/*
		 * @see ISiteEntry#setSitePolicy(ISitePolicy)
		 */
		public synchronized void setSitePolicy(ISitePolicy policy) {
			if (policy == null)
				throw new IllegalArgumentException();
			this.policy = policy;
		}

		/*
		 * @see ISiteEntry#getFeatures()
		 */
		public String[] getFeatures() {
			return getDetectedFeatures();
		}

		/*
		 * @see ISiteEntry#getPlugins()
		 */
		public String[] getPlugins() {

			ISitePolicy policy = getSitePolicy();

			if (policy.getType() == ISitePolicy.USER_INCLUDE)
				return policy.getList();

			if (policy.getType() == ISitePolicy.USER_EXCLUDE) {
				ArrayList detectedPlugins = new ArrayList(Arrays.asList(getDetectedPlugins()));
				String[] excludedPlugins = policy.getList();
				for (int i = 0; i < excludedPlugins.length; i++) {
					if (detectedPlugins.contains(excludedPlugins[i]))
						detectedPlugins.remove(excludedPlugins[i]);
				}
				return (String[]) detectedPlugins.toArray(new String[0]);
			}

			// bad policy type
			return new String[0];
		}

		/*
		 * @see ISiteEntry#getChangeStamp()
		 */
		public long getChangeStamp() {
			if (!changeStampIsValid)
				computeChangeStamp();
			return changeStamp;
		}

		/*
		 * @see ISiteEntry#getFeaturesChangeStamp()
		 */
		public long getFeaturesChangeStamp() {
			if (!featuresChangeStampIsValid)
				computeFeaturesChangeStamp();
			return featuresChangeStamp;
		}

		/*
		 * @see ISiteEntry#getPluginsChangeStamp()
		 */
		public long getPluginsChangeStamp() {
			if (!pluginsChangeStampIsValid)
				computePluginsChangeStamp();
			return pluginsChangeStamp;
		}

		/*
		 * @see ISiteEntry#isUpdateable()
		 */
		public boolean isUpdateable() {
			return updateable;
		}

		/*
		 * @see ISiteEntry#isNativelyLinked()
		 */
		public boolean isNativelyLinked() {
			return isExternallyLinkedSite();
		}

		private String[] detectFeatures() {

			// invalidate stamps ... we are doing discovery
			changeStampIsValid = false;
			featuresChangeStampIsValid = false;
			parent.changeStampIsValid = false;
			parent.featuresChangeStampIsValid = false;

			features = new ArrayList();

			if (!supportsDetection(resolvedURL))
				return new String[0];

			// locate feature entries on site
			File siteRoot = new File(resolvedURL.getFile().replace('/', File.separatorChar));
			File root = new File(siteRoot, FEATURES);

			String[] list = root.list();
			String path;
			File plugin;
			for (int i = 0; list != null && i < list.length; i++) {
				path = list[i] + File.separator + FEATURE_XML;
				plugin = new File(root, path);
				if (!plugin.exists()) {
					continue;
				}
				features.add(FEATURES + "/" + path.replace(File.separatorChar, '/')); //$NON-NLS-1$
			}
			if (DEBUG) {
				debug(resolvedURL.toString() + " located  " + features.size() + " feature(s)"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			return (String[]) features.toArray(new String[0]);
		}

		private String[] detectPlugins() {

			// invalidate stamps ... we are doing discovery
			changeStampIsValid = false;
			pluginsChangeStampIsValid = false;
			parent.changeStampIsValid = false;
			parent.pluginsChangeStampIsValid = false;

			plugins = new ArrayList();

			if (!supportsDetection(resolvedURL))
				return new String[0];

			// locate plugin entries on site
			File root = new File(resolvedURL.getFile().replace('/', File.separatorChar) + PLUGINS);
			String[] list = root.list();
			String path;
			File plugin;
			for (int i = 0; list != null && i < list.length; i++) {
				path = list[i] + File.separator + PLUGIN_XML;
				plugin = new File(root, path);
				if (!plugin.exists()) {
					path = list[i] + File.separator + FRAGMENT_XML;
					plugin = new File(root, path);
					if (!plugin.exists())
						continue;
				}
				plugins.add(PLUGINS + "/" + path.replace(File.separatorChar, '/')); //$NON-NLS-1$
			}
			if (DEBUG) {
				debug(resolvedURL.toString() + " located  " + plugins.size() + " plugin(s)"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			return (String[]) plugins.toArray(new String[0]);
		}

		private synchronized String[] getDetectedFeatures() {
			if (features == null)
				return detectFeatures();
			return (String[]) features.toArray(new String[0]);
		}

		private synchronized String[] getDetectedPlugins() {
			if (plugins == null)
				return detectPlugins();
			return (String[]) plugins.toArray(new String[0]);
		}

		private URL getResolvedURL() {
			return resolvedURL;
		}

		private void computeChangeStamp() {
			computeFeaturesChangeStamp();
			computePluginsChangeStamp();
			changeStamp = resolvedURL.hashCode() ^ featuresChangeStamp ^ pluginsChangeStamp;
			changeStampIsValid = true;
		}

		private synchronized void computeFeaturesChangeStamp() {
			if (featuresChangeStampIsValid)
				return;

			long start = 0;
			if (DEBUG)
				start = (new Date()).getTime();
			String[] features = getFeatures();
			featuresChangeStamp = computeStamp(features);
			featuresChangeStampIsValid = true;
			if (DEBUG) {
				long end = (new Date()).getTime();
				debug(resolvedURL.toString() + " feature stamp: " + featuresChangeStamp + ((featuresChangeStamp == lastFeaturesChangeStamp) ? " [no changes]" : " [was " + lastFeaturesChangeStamp + "]") + " in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			}
		}

		private synchronized void computePluginsChangeStamp() {
			if (pluginsChangeStampIsValid)
				return;

			long start = 0;
			if (DEBUG)
				start = (new Date()).getTime();
			String[] plugins = getPlugins();
			pluginsChangeStamp = computeStamp(plugins);
			pluginsChangeStampIsValid = true;
			if (DEBUG) {
				long end = (new Date()).getTime();
				debug(resolvedURL.toString() + " plugin stamp: " + pluginsChangeStamp + ((pluginsChangeStamp == lastPluginsChangeStamp) ? " [no changes]" : " [was " + lastPluginsChangeStamp + "]") + " in " + (end - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			}
		}

		private long computeStamp(String[] targets) {

			long result = 0;
			if (!supportsDetection(resolvedURL)) {
				// NOTE:  this path should not be executed until we support running
				//        from an arbitrary URL (in particular from http server). For
				//        now just compute stamp across the list of names. Eventually
				//        when general URLs are supported we need to do better (factor
				//        in at least the existence of the target). However, given this
				//        code executes early on the startup sequence we need to be
				//        extremely mindful of performance issues.
				for (int i = 0; i < targets.length; i++)
					result ^= targets[i].hashCode();
				if (DEBUG)
					debug("*WARNING* computing stamp using URL hashcodes only"); //$NON-NLS-1$
			} else {
				// compute stamp across local targets
				String rootPath = resolvedURL.getFile().replace('/', File.separatorChar);
				if (!rootPath.endsWith(File.separator))
					rootPath += File.separator;
				File rootFile = new File(rootPath);
				if (rootFile.exists()) {
					File f = null;
					for (int i = 0; i < targets.length; i++) {
						f = new File(rootFile, targets[i]);
						if (f.exists())
							result ^= f.getAbsolutePath().hashCode() ^ f.lastModified() ^ f.length();
					}
				}
			}

			return result;
		}

		private boolean isExternallyLinkedSite() {
			return (linkFileName != null && !linkFileName.trim().equals("")); //$NON-NLS-1$
		}

		private synchronized void refresh() {
			// reset computed values. Will be updated on next access.
			lastFeaturesChangeStamp = featuresChangeStamp;
			lastPluginsChangeStamp = pluginsChangeStamp;
			changeStampIsValid = false;
			featuresChangeStampIsValid = false;
			pluginsChangeStampIsValid = false;
			features = null;
			plugins = null;
		}

	}

	public class SitePolicy implements IPlatformConfiguration.ISitePolicy {

		private int type;
		private String[] list;

		private SitePolicy() {
		}
		private SitePolicy(int type, String[] list) {
			if (type != ISitePolicy.USER_INCLUDE && type != ISitePolicy.USER_EXCLUDE)
				throw new IllegalArgumentException();
			this.type = type;

			if (list == null)
				this.list = new String[0];
			else
				this.list = list;
		}

		/*
		 * @see ISitePolicy#getType()
		 */
		public int getType() {
			return type;
		}

		/*
		* @see ISitePolicy#getList()
		*/
		public String[] getList() {
			return list;
		}

		/*
		 * @see ISitePolicy#setList(String[])
		 */
		public synchronized void setList(String[] list) {
			if (list == null)
				this.list = new String[0];
			else
				this.list = list;
		}

	}

	public class FeatureEntry implements IPlatformConfiguration.IFeatureEntry {
		private String id;
		private String version;
		private String pluginVersion;
		private String application;
		private URL[] root;
		private boolean primary;
		private String pluginIdentifier;

		private FeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
			if (id == null)
				throw new IllegalArgumentException();
			this.id = id;
			this.version = version;
			this.pluginVersion = pluginVersion;
			this.pluginIdentifier = pluginIdentifier;
			this.primary = primary;
			this.application = application;
			this.root = (root == null ? new URL[0] : root);
		}

		private FeatureEntry(String id, String version, String pluginVersion, boolean primary, String application, URL[] root) {
			this(id, version, id, pluginVersion, primary, application, root);
		}

		/*
		 * @see IFeatureEntry#getFeatureIdentifier()
		 */
		public String getFeatureIdentifier() {
			return id;
		}

		/*
		 * @see IFeatureEntry#getFeatureVersion()
		 */
		public String getFeatureVersion() {
			return version;
		}

		/*
		 * @see IFeatureEntry#getFeaturePluginVersion()
		 */
		public String getFeaturePluginVersion() {
			return pluginVersion;
		}

		/*
		 * @see IFeatureEntry#getFeatureApplication()
		 */
		public String getFeatureApplication() {
			return application;
		}

		/*
		 * @see IFeatureEntry#getFeatureRootURLs()
		 */
		public URL[] getFeatureRootURLs() {
			return root;
		}

		/*
		 * @see IFeatureEntry#canBePrimary()
		 */
		public boolean canBePrimary() {
			return primary;
		}
		/*
		 * @see IFeatureEntry#getFeaturePluginIdentifier()
		 */
		public String getFeaturePluginIdentifier() {
			return pluginIdentifier;
		}

	}


	/*
	 * Element selector for use with "tiny" parser. Parser callers supply
	 * concrete selectors
	 */
	public interface Selector {

		/*
		 * Method is called to pre-select a specific xml type. Pre-selected
		 * elements are then fully parsed and result in calls to full
		 * select method.
		 * @return <code>true</code> is the element should be considered,
		 * <code>false</code> otherwise
		 */
		public boolean select(String entry);

		/*
		 * Method is called with a fully parsed element.
		 * @return <code>true</code> to select this element and terminate the parse,
		 * <code>false</code> otherwise
		 */
		public boolean select(String element, HashMap attributes);
	}

	/*
	 * "Tiny" xml parser. Performs a rudimentary parse of a well-formed xml file.
	 * Is specifically geared to parsing plugin.xml files of "bootstrap" plug-ins
	 * during the platform startup sequence before full xml plugin is available.
	 */
	public static class Parser {

		private ArrayList elements = new ArrayList();

		/*
		 * Construct parser for the specified file
		 */
		public Parser(File file) {
			try {
				load(new FileInputStream(file));
			} catch (Exception e) {
				// continue ... actual parsing will report errors
			}
		}

		/*
		 * Construct parser for the specified URL
		 */
		public Parser(URL url) {
			try {
				load(url.openStream());
			} catch (Exception e) {
				// continue ... actual parsing will report errors
			}
		}

		/*
		 * Return selected elements as an (attribute-name, attribute-value) map.
		 * The name of the selected element is returned as the value of entry with
		 * name "<element>".
		 * @return attribute map for selected element, or <code>null</code>
		 */
		public HashMap getElement(Selector selector) {
			if (selector == null)
				return null;

			String element;
			for (int i = 0; i < elements.size(); i++) {
				// make pre-parse selector call
				element = (String) elements.get(i);
				if (selector.select(element)) {
					// parse selected entry
					HashMap attributes = new HashMap();
					String elementName;
					int j;
					// parse out element name
					for (j = 0; j < element.length(); j++) {
						if (Character.isWhitespace(element.charAt(j)))
							break;
					}
					if (j >= element.length()) {
						elementName = element;
					} else {
						elementName = element.substring(0, j);
						element = element.substring(j);
						// parse out attributes
						StringTokenizer t = new StringTokenizer(element, "=\""); //$NON-NLS-1$
						boolean isKey = true;
						String key = ""; //$NON-NLS-1$
						while (t.hasMoreTokens()) {
							String token = t.nextToken().trim();
							if (!token.equals("")) { //$NON-NLS-1$
								// collect (key, value) pairs
								if (isKey) {
									key = token;
									isKey = false;
								} else {
									attributes.put(key, token);
									isKey = true;
								}
							}
						}
					}
					// make post-parse selector call
					if (selector.select(elementName, attributes)) {
						attributes.put("<element>", elementName); //$NON-NLS-1$
						return attributes;
					}
				}
			}
			return null;
		}

		private void load(InputStream is) {
			if (is == null)
				return;

			// read file
			StringBuffer xml = new StringBuffer(4096);
			char[] iobuf = new char[4096];
			InputStreamReader r = null;
			try {
				r = new InputStreamReader(is);
				int len = r.read(iobuf, 0, iobuf.length);
				while (len != -1) {
					xml.append(iobuf, 0, len);
					len = r.read(iobuf, 0, iobuf.length);
				}
			} catch (Exception e) {
				return;
			} finally {
				if (r != null)
					try {
						r.close();
					} catch (IOException e) {
						// ignore
					}
			}

			// parse out element tokens
			String xmlString = xml.toString();
			StringTokenizer t = new StringTokenizer(xmlString, "<>"); //$NON-NLS-1$
			while (t.hasMoreTokens()) {
				String token = t.nextToken().trim();
				if (!token.equals("")) //$NON-NLS-1$
					elements.add(token);
			}
		}
	}

	public static class BootDescriptor {
		private String id;
		private String version;
		private String[] libs;
		private URL dir;

		public BootDescriptor(String id, String version, String[] libs, URL dir) {
			this.id = id;
			this.version = version;
			this.libs = libs;
			this.dir = dir;
		}

		public String getId() {
			return id;
		}

		public String getVersion() {
			return version;
		}

		public String[] getLibraries() {
			return libs;
		}

		public URL getPluginDirectoryURL() {
			return dir;
		}
	}


	PlatformConfiguration(URL url) throws IOException {
		this.sites = new HashMap();
		this.externalLinkSites = new HashMap();
		this.cfgdFeatures = new HashMap();
		this.bootPlugins = new HashMap();
		initialize(url);
	}

	/*
	 * @see IPlatformConfiguration#createSiteEntry(URL, ISitePolicy)
	 */
	public ISiteEntry createSiteEntry(URL url, ISitePolicy policy) {
		return new PlatformConfiguration.SiteEntry(url, policy, this);
	}

	/*
	 * @see IPlatformConfiguration#createSitePolicy(int, String[])
	 */
	public ISitePolicy createSitePolicy(int type, String[] list) {
		return new PlatformConfiguration.SitePolicy(type, list);
	}

	/*
	 * @see IPlatformConfiguration#createFeatureEntry(String, String, String, boolean, String, URL)
	 */
	public IFeatureEntry createFeatureEntry(String id, String version, String pluginVersion, boolean primary, String application, URL[] root) {
		return new PlatformConfiguration.FeatureEntry(id, version, pluginVersion, primary, application, root);
	}

	/*
	 * @see IPlatformConfiguration#createFeatureEntry(String, String, String,
	 * String, boolean, String, URL)
	 */
	public IFeatureEntry createFeatureEntry(String id, String version, String pluginIdentifier, String pluginVersion, boolean primary, String application, URL[] root) {
		return new PlatformConfiguration.FeatureEntry(id, version, pluginIdentifier, pluginVersion, primary, application, root);
	}

	/*
	 * @see IPlatformConfiguration#configureSite(ISiteEntry)
	 */
	public void configureSite(ISiteEntry entry) {
		configureSite(entry, false);
	}

	/*
	 * @see IPlatformConfiguration#configureSite(ISiteEntry, boolean)
	 */
	public synchronized void configureSite(ISiteEntry entry, boolean replace) {

		if (entry == null)
			return;

		URL url = entry.getURL();
		if (url == null)
			return;
		String key = url.toExternalForm();

		if (sites.containsKey(key) && !replace)
			return;

		sites.put(key, entry);
	}

	/*
	 * @see IPlatformConfiguration#unconfigureSite(ISiteEntry)
	 */
	public synchronized void unconfigureSite(ISiteEntry entry) {
		if (entry == null)
			return;

		URL url = entry.getURL();
		if (url == null)
			return;
		String key = url.toExternalForm();

		sites.remove(key);
	}

	/*
	 * @see IPlatformConfiguration#getConfiguredSites()
	 */
	public ISiteEntry[] getConfiguredSites() {
		if (sites.size() == 0)
			return new ISiteEntry[0];

		return (ISiteEntry[]) sites.values().toArray(new ISiteEntry[0]);
	}

	/*
	 * @see IPlatformConfiguration#findConfiguredSite(URL)
	 */
	public ISiteEntry findConfiguredSite(URL url) {
		if (url == null)
			return null;
		String key = url.toExternalForm();

		ISiteEntry result = (ISiteEntry) sites.get(key);
		try {
			if (result == null) // retry with decoded URL string
				result = (ISiteEntry) sites.get(URLDecoder.decode(key, null));
		} catch (UnsupportedEncodingException e) {
		}
		return result;
	}

	/*
	 * @see IPlatformConfiguration#configureFeatureEntry(IFeatureEntry)
	 */
	public synchronized void configureFeatureEntry(IFeatureEntry entry) {
		if (entry == null)
			return;

		String key = entry.getFeatureIdentifier();
		if (key == null)
			return;

		cfgdFeatures.put(key, entry);
	}

	/*
	 * @see IPlatformConfiguration#unconfigureFeatureEntry(IFeatureEntry)
	 */
	public synchronized void unconfigureFeatureEntry(IFeatureEntry entry) {
		if (entry == null)
			return;

		String key = entry.getFeatureIdentifier();
		if (key == null)
			return;

		cfgdFeatures.remove(key);
	}

	/*
	 * @see IPlatformConfiguration#getConfiguredFeatureEntries()
	 */
	public IFeatureEntry[] getConfiguredFeatureEntries() {
		if (cfgdFeatures.size() == 0)
			return new IFeatureEntry[0];

		return (IFeatureEntry[]) cfgdFeatures.values().toArray(new IFeatureEntry[0]);
	}

	/*
	 * @see IPlatformConfiguration#findConfiguredFeatureEntry(String)
	 */
	public IFeatureEntry findConfiguredFeatureEntry(String id) {
		if (id == null)
			return null;

		return (IFeatureEntry) cfgdFeatures.get(id);
	}

	/*
	 * @see IPlatformConfiguration#getConfigurationLocation()
	 */
	public URL getConfigurationLocation() {
		return configLocation;
	}

	/*
	 * @see IPlatformConfiguration#getChangeStamp()
	 */
	public long getChangeStamp() {
		if (!changeStampIsValid)
			computeChangeStamp();
		return changeStamp;
	}

	/*
	 * @see IPlatformConfiguration#getFeaturesChangeStamp()
	 */
	public long getFeaturesChangeStamp() {
		if (!featuresChangeStampIsValid)
			computeFeaturesChangeStamp();
		return featuresChangeStamp;
	}

	/*
	 * @see IPlatformConfiguration#getPluginsChangeStamp()
	 */
	public long getPluginsChangeStamp() {
		if (!pluginsChangeStampIsValid)
			computePluginsChangeStamp();
		return pluginsChangeStamp;
	}

	/*
	 * @see IPlatformConfiguration#getApplicationIdentifier()
	 */
	public String getApplicationIdentifier() {

		if (cmdInitialize) {
			// we are running post-install initialization. Force
			// running of the reconciler
			return RECONCILER_APP;
		}

		if (featuresChangeStamp != lastFeaturesChangeStamp) {
			// we have detected feature changes ... see if we need to reconcile
			boolean update = !cmdNoUpdate || cmdUpdate;
			if (update)
				return RECONCILER_APP;
		}

		// "normal" startup ... run specified application
		return getApplicationIdentifierInternal();
	}

	private String getApplicationIdentifierInternal() {

		if (cmdApplication != null) // application was specified
			return cmdApplication;
		// if -feature was not specified use the default feature
		String feature = cmdFeature;
		if (feature == null)
			feature = defaultFeature;

		// lookup application for feature (specified or defaulted)
		if (feature != null) {
			IFeatureEntry fe = findConfiguredFeatureEntry(feature);
			if (fe != null) {
				if (fe.getFeatureApplication() != null)
					return fe.getFeatureApplication();
			}
		}

		// return hardcoded default if we failed
		return DEFAULT_FEATURE_APPLICATION;
	}

	/*
	 * @see IPlatformConfiguration#getPrimaryFeatureIdentifier()
	 */
	public String getPrimaryFeatureIdentifier() {

		if (cmdFeature != null) // -feature was specified on command line
			return cmdFeature;

		// feature was not specified on command line
		if (defaultFeature != null)
			return defaultFeature; // return customized default if set
		return DEFAULT_FEATURE_ID; // return hardcoded default
	}

	/*
	 * @see IPlatformConfiguration#getPluginPath()
	 */
	public URL[] getPluginPath() {
		ArrayList path = new ArrayList();
		if (DEBUG)
			debug("computed plug-in path:"); //$NON-NLS-1$

		ISiteEntry[] sites = getConfiguredSites();
		URL pathURL;
		for (int i = 0; i < sites.length; i++) {
			String[] plugins = sites[i].getPlugins();
			for (int j = 0; j < plugins.length; j++) {
				try {
					pathURL = new URL(((SiteEntry) sites[i]).getResolvedURL(), plugins[j]);
					path.add(pathURL);
					if (DEBUG)
						debug("   " + pathURL.toString()); //$NON-NLS-1$
				} catch (MalformedURLException e) {
					// skip entry ...
					if (DEBUG)
						debug("   bad URL: " + e); //$NON-NLS-1$
				}
			}
		}
		return (URL[]) path.toArray(new URL[0]);
	}

	/*
	 * @see IPlatformConfiguration#getBootstrapPluginIdentifiers()
	 */
	public String[] getBootstrapPluginIdentifiers() {
		return BOOTSTRAP_PLUGINS;
	}

	/*
	 * @see IPlatformConfiguration#setBootstrapPluginLocation(String, URL)
	 */
	public void setBootstrapPluginLocation(String id, URL location) {
		String[] ids = getBootstrapPluginIdentifiers();
		for (int i = 0; i < ids.length; i++) {
			if (ids[i].equals(id)) {
				bootPlugins.put(id, location.toExternalForm());
				break;
			}
		}
	}

	/*
	 * @see IPlatformConfiguration#isUpdateable()
	 */
	public boolean isUpdateable() {
		return true;
	}

	/*
	 * @see IPlatformConfiguration#isTransient()
	 */
	public boolean isTransient() {
		return transientConfig;
	}

	/*
	 * @see IPlatformConfiguration#isTransient(boolean)
	 */
	public void isTransient(boolean value) {
		//		if (this != BootLoader.getCurrentPlatformConfiguration())
		//			transientConfig = value;
	}

	/*
	 * @see IPlatformConfiguration#refresh()
	 */
	public synchronized void refresh() {
		// Reset computed values. Will be lazily refreshed
		// on next access
		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			// reset site entry
			 ((SiteEntry) sites[i]).refresh();
		}
		// reset configuration entry.
		lastFeaturesChangeStamp = featuresChangeStamp;
		changeStampIsValid = false;
		featuresChangeStampIsValid = false;
		pluginsChangeStampIsValid = false;
	}

	/*
	 * @see IPlatformConfiguration#save()
	 */
	public void save() throws IOException {
		if (isUpdateable())
			save(configLocation);
	}

	/*
	 * @see IPlatformConfiguration#save(URL)
	 */
	public synchronized void save(URL url) throws IOException {
		if (url == null)
			throw new IOException("Unable to save.  No URL is specified"); //$NON-NLS-1$

		PrintWriter w = null;
		OutputStream os = null;
		if (!url.getProtocol().equals("file")) { //$NON-NLS-1$
			// not a file protocol - attempt to save to the URL
			URLConnection uc = url.openConnection();
			uc.setDoOutput(true);
			os = uc.getOutputStream();
			w = new PrintWriter(os);
			try {
				write(w);
			} finally {
				w.close();
			}
		} else {
			// file protocol - do safe i/o
			File cfigFile = new File(url.getFile().replace('/', File.separatorChar));
			File cfigDir = cfigFile.getParentFile();
			if (cfigDir != null)
				cfigDir.mkdirs();

			// first save the file as temp
			File cfigTmp = new File(cfigFile.getAbsolutePath() + CONFIG_FILE_TEMP_SUFFIX);
			os = new FileOutputStream(cfigTmp);
			w = new PrintWriter(os);
			try {
				write(w);
			} finally {
				w.close();
			}

			// make sure we actually succeeded saving the whole configuration.
			InputStream is = new FileInputStream(cfigTmp);
			Properties tmpProps = new Properties();
			try {
				tmpProps.load(is);
				if (!EOF.equals(tmpProps.getProperty(EOF))) {
					throw new IOException("Unable to save " +  cfigTmp.getAbsolutePath()); //$NON-NLS-1$
				}
			} finally {
				is.close();
			}

			// make the saved config the "active" one
			File cfigBak = new File(cfigFile.getAbsolutePath() + CONFIG_FILE_BAK_SUFFIX);
			cfigBak.delete(); // may have old .bak due to prior failure

			if (cfigFile.exists())
				cfigFile.renameTo(cfigBak);

			// at this point we have old config (if existed) as "bak" and the
			// new config as "tmp".
			boolean ok = cfigTmp.renameTo(cfigFile);
			if (ok) {
				// at this point we have the new config "activated", and the old
				// config (if it existed) as "bak"
				cfigBak.delete(); // clean up
			} else {
				// this codepath represents a tiny failure window. The load processing
				// on startup will detect missing config and will attempt to start
				// with "tmp" (latest), then "bak" (the previous). We can also end up
				// here if we failed to rename the current config to "bak". In that
				// case we will restart with the previous state.
				throw new IOException("Unable to save " + cfigTmp.getAbsolutePath()); //$NON-NLS-1$
			}
		}
	}

	public BootDescriptor getPluginBootDescriptor(String id) {
		// return the plugin descriptor for the specified plugin. This method
		// is used during boot processing to obtain information about "kernel" plugins
		// whose class loaders must be created prior to the plugin registry being
		// available (ie. loaders needed to create the plugin registry).

		if (RUNTIME_PLUGIN_ID.equals(id))
			return runtimeDescriptor;
		return null;
	}

	static PlatformConfiguration getCurrent() {
		return currentPlatformConfiguration;
	}

	static synchronized void shutdown() throws IOException {

		// save platform configuration
		PlatformConfiguration config = getCurrent();
		if (config != null) {
			try {
				config.save();
			} catch (IOException e) {
				if (DEBUG)
					debug("Unable to save configuration " + e.toString()); //$NON-NLS-1$
				// will recover on next startup
			}
			config.clearConfigurationLock();
		}
	}

	private synchronized void initialize(URL url) throws IOException {
		if (url == null) {
			if (DEBUG)
				debug("Creating empty configuration object"); //$NON-NLS-1$
			return;
		}

		load(url);
		configLocation = url;
		if (DEBUG)
			debug("Using configuration " + configLocation.toString()); //$NON-NLS-1$
	}

	private ISiteEntry getRootSite() {
		// create default site entry for the root
		ISitePolicy defaultPolicy = createSitePolicy(DEFAULT_POLICY_TYPE, DEFAULT_POLICY_LIST);
		URL siteURL = null;
		try {
			siteURL = new URL(PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR + "/" + "base" + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ // try using platform-relative URL
		} catch (MalformedURLException e) {
			siteURL = getInstallURL(); // ensure we come up ... use absolute file URL
		}
		ISiteEntry defaultSite = createSiteEntry(siteURL, defaultPolicy);
		return defaultSite;
	}


	private void resetInitializationLocation(File dir) {
		// [20111]
		if (dir == null || !dir.exists() || !dir.isDirectory())
			return;
		File[] list = dir.listFiles();
		for (int i = 0; i < list.length; i++) {
			if (list[i].isDirectory())
				resetInitializationLocation(list[i]);
			list[i].delete();
		}
	}


	private void clearConfigurationLock() {
		try {
			if (cfgLockFileRAF != null) {
				cfgLockFileRAF.close();
				cfgLockFileRAF = null;
			}
		} catch (IOException e) {
			// ignore ...
		}
		if (cfgLockFile != null) {
			cfgLockFile.delete();
			cfgLockFile = null;
		}
	}

	private void computeChangeStamp() {
		computeFeaturesChangeStamp();
		computePluginsChangeStamp();
		changeStamp = featuresChangeStamp ^ pluginsChangeStamp;
		changeStampIsValid = true;
	}

	private void computeFeaturesChangeStamp() {
		if (featuresChangeStampIsValid)
			return;

		long result = 0;
		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			result ^= sites[i].getFeaturesChangeStamp();
		}
		featuresChangeStamp = result;
		featuresChangeStampIsValid = true;
	}

	private void computePluginsChangeStamp() {
		if (pluginsChangeStampIsValid)
			return;

		long result = 0;
		ISiteEntry[] sites = getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			result ^= sites[i].getPluginsChangeStamp();
		}
		pluginsChangeStamp = result;
		pluginsChangeStampIsValid = true;
	}
	
	private void load(URL url) throws IOException {

		if (url == null)
			throw new IOException("Unable to load URL"); //$NON-NLS-1$

		// try to load saved configuration file (watch for failed prior save())
		Properties props = null;
		IOException originalException = null;
		try {
			props = loadProperties(url, null); // try to load config file
		} catch (IOException e1) {
			originalException = e1;
			try {
				props = loadProperties(url, CONFIG_FILE_TEMP_SUFFIX); // check for failures on save
			} catch (IOException e2) {
				try {
					props = loadProperties(url, CONFIG_FILE_BAK_SUFFIX); // check for failures on save
				} catch (IOException e3) {
					throw originalException; // we tried, but no config here ...
				}
			}
		}

		// check version
		String v = props.getProperty(CFG_VERSION);
		if (!VERSION.equals(v)) {
			// the state is invalid, delete any files under the directory
			// bug 33493
			resetUpdateManagerState(url);
			throw new IOException("Bad version:" + v); //$NON-NLS-1$
		}

		// load simple properties
		defaultFeature = loadAttribute(props, CFG_FEATURE_ENTRY_DEFAULT, null);

		String flag = loadAttribute(props, CFG_TRANSIENT, null);
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				transientConfig = true;
			else
				transientConfig = false;
		}

		String stamp = loadAttribute(props, CFG_FEATURE_STAMP, null);
		if (stamp != null) {
			try {
				lastFeaturesChangeStamp = Long.parseLong(stamp);
			} catch (NumberFormatException e) {
				// ignore bad attribute ...
			}
		}

		// load bootstrap entries
		String[] ids = getBootstrapPluginIdentifiers();
		for (int i = 0; i < ids.length; i++) {
			bootPlugins.put(ids[i], loadAttribute(props, CFG_BOOT_PLUGIN + "." + ids[i], null)); //$NON-NLS-1$
		}

		// load feature entries
		IFeatureEntry fe = loadFeatureEntry(props, CFG_FEATURE_ENTRY + ".0", null); //$NON-NLS-1$
		for (int i = 1; fe != null; i++) {
			configureFeatureEntry(fe);
			fe = loadFeatureEntry(props, CFG_FEATURE_ENTRY + "." + i, null); //$NON-NLS-1$
		}

		// load site properties
		SiteEntry root = (SiteEntry) getRootSite();
		String rootUrlString = root.getURL().toExternalForm();
		SiteEntry se = (SiteEntry) loadSite(props, CFG_SITE + ".0", null); //$NON-NLS-1$
		for (int i = 1; se != null; i++) {

			// check if we are forcing "first use" processing with an existing
			// platform.cfg. In this case ignore site entry that represents
			// the platform install, and use a root site entry in its place.
			// This ensures we do not get messed up by an exclusion list that
			// is read from the prior state.
			if (cmdFirstUse && rootUrlString.equals(se.getURL().toExternalForm()))
				se = root;

			if (!se.isExternallyLinkedSite())
				configureSite(se);
			else
				// remember external link site state, but do not configure at this point
				externalLinkSites.put(se.getURL(), se);
			se = (SiteEntry) loadSite(props, CFG_SITE + "." + i, null); //$NON-NLS-1$
		}
	}

	private Properties loadProperties(URL url, String suffix) throws IOException {

		// figure out what we will be loading
		if (suffix != null && !suffix.equals("")) //$NON-NLS-1$
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + suffix);

		// try to load saved configuration file
		Properties props = new Properties();
		InputStream is = null;
		try {
			is = url.openStream();
			props.load(is);
			// check to see if we have complete config file
			if (!EOF.equals(props.getProperty(EOF))) {
				throw new IOException("Unable to load"); //$NON-NLS-1$
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// ignore ...
				}
			}
		}
		return props;
	}

	private ISiteEntry loadSite(Properties props, String name, ISiteEntry dflt) {

		String urlString = loadAttribute(props, name + "." + CFG_URL, null); //$NON-NLS-1$
		if (urlString == null)
			return dflt;

		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			return dflt;
		}

		int policyType;
		String[] policyList;
		String typeString = loadAttribute(props, name + "." + CFG_POLICY, null); //$NON-NLS-1$
		if (typeString == null) {
			policyType = DEFAULT_POLICY_TYPE;
			policyList = DEFAULT_POLICY_LIST;
		} else {
			int i;
			for (i = 0; i < CFG_POLICY_TYPE.length; i++) {
				if (typeString.equals(CFG_POLICY_TYPE[i])) {
					break;
				}
			}
			if (i >= CFG_POLICY_TYPE.length) {
				policyType = DEFAULT_POLICY_TYPE;
				policyList = DEFAULT_POLICY_LIST;
			} else {
				policyType = i;
				policyList = loadListAttribute(props, name + "." + CFG_LIST, new String[0]); //$NON-NLS-1$
			}
		}

		ISitePolicy sp = createSitePolicy(policyType, policyList);
		SiteEntry site = (SiteEntry) createSiteEntry(url, sp);

		String stamp = loadAttribute(props, name + "." + CFG_FEATURE_STAMP, null); //$NON-NLS-1$
		if (stamp != null) {
			try {
				site.lastFeaturesChangeStamp = Long.parseLong(stamp);
			} catch (NumberFormatException e) {
				// ignore bad attribute ...
			}
		}

		stamp = loadAttribute(props, name + "." + CFG_PLUGIN_STAMP, null); //$NON-NLS-1$
		if (stamp != null) {
			try {
				site.lastPluginsChangeStamp = Long.parseLong(stamp);
			} catch (NumberFormatException e) {
				// ignore bad attribute ...
			}
		}

		String flag = loadAttribute(props, name + "." + CFG_UPDATEABLE, null); //$NON-NLS-1$
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				site.updateable = true;
			else
				site.updateable = false;
		}

		String linkname = loadAttribute(props, name + "." + CFG_LINK_FILE, null); //$NON-NLS-1$
		if (linkname != null && !linkname.equals("")) { //$NON-NLS-1$
			site.linkFileName = linkname.replace('/', File.separatorChar);
		}

		return site;
	}

	private IFeatureEntry loadFeatureEntry(Properties props, String name, IFeatureEntry dflt) {
		String id = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_ID, null); //$NON-NLS-1$
		if (id == null)
			return dflt;
		String version = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_VERSION, null); //$NON-NLS-1$
		String pluginVersion = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_PLUGIN_VERSION, null); //$NON-NLS-1$
		if (pluginVersion == null)
			pluginVersion = version;
		String pluginIdentifier = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER, null); //$NON-NLS-1$
		if (pluginIdentifier == null)
			pluginIdentifier = id;
		String application = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_APPLICATION, null); //$NON-NLS-1$
		ArrayList rootList = new ArrayList();

		// get install locations
		String rootString = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_ROOT + ".0", null); //$NON-NLS-1$ //$NON-NLS-2$
		for (int i = 1; rootString != null; i++) {
			try {
				URL rootEntry = new URL(rootString);
				rootList.add(rootEntry);
			} catch (MalformedURLException e) {
				// skip bad entries ...
			}
			rootString = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_ROOT + "." + i, null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		URL[] roots = (URL[]) rootList.toArray(new URL[0]);

		// get primary flag
		boolean primary = false;
		String flag = loadAttribute(props, name + "." + CFG_FEATURE_ENTRY_PRIMARY, null); //$NON-NLS-1$
		if (flag != null) {
			if (flag.equals("true")) //$NON-NLS-1$
				primary = true;
		}
		return createFeatureEntry(id, version, pluginIdentifier, pluginVersion, primary, application, roots);
	}

	private String[] loadListAttribute(Properties props, String name, String[] dflt) {
		ArrayList list = new ArrayList();
		String value = loadAttribute(props, name + ".0", null); //$NON-NLS-1$
		if (value == null)
			return dflt;

		for (int i = 1; value != null; i++) {
			loadListAttributeSegment(list, value);
			value = loadAttribute(props, name + "." + i, null); //$NON-NLS-1$
		}
		return (String[]) list.toArray(new String[0]);
	}

	private void loadListAttributeSegment(ArrayList list, String value) {

		if (value == null)
			return;

		StringTokenizer tokens = new StringTokenizer(value, ","); //$NON-NLS-1$
		String token;
		while (tokens.hasMoreTokens()) {
			token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.add(token);
		}
		return;
	}

	private String loadAttribute(Properties props, String name, String dflt) {
		String prop = props.getProperty(name);
		if (prop == null)
			return dflt;
		return prop.trim();
	}


	private void write(PrintWriter w) {
		// write header
		w.println("# " + (new Date()).toString()); //$NON-NLS-1$
		writeAttribute(w, CFG_VERSION, VERSION);
		if (transientConfig)
			writeAttribute(w, CFG_TRANSIENT, "true"); //$NON-NLS-1$
		w.println(""); //$NON-NLS-1$

		// write global attributes
		writeAttribute(w, CFG_STAMP, Long.toString(getChangeStamp()));
		writeAttribute(w, CFG_FEATURE_STAMP, Long.toString(getFeaturesChangeStamp()));
		writeAttribute(w, CFG_PLUGIN_STAMP, Long.toString(getPluginsChangeStamp()));

		// write out bootstrap entries
		String[] ids = getBootstrapPluginIdentifiers();
		for (int i = 0; i < ids.length; i++) {
			String location = (String) bootPlugins.get(ids[i]);
			if (location != null)
				writeAttribute(w, CFG_BOOT_PLUGIN + "." + ids[i], location); //$NON-NLS-1$
		}

		// write out feature entries
		w.println(""); //$NON-NLS-1$
		writeAttribute(w, CFG_FEATURE_ENTRY_DEFAULT, defaultFeature);
		IFeatureEntry[] feats = getConfiguredFeatureEntries();
		for (int i = 0; i < feats.length; i++) {
			writeFeatureEntry(w, CFG_FEATURE_ENTRY + "." + Integer.toString(i), feats[i]); //$NON-NLS-1$
		}

		// write out site entries
		SiteEntry[] list = (SiteEntry[]) sites.values().toArray(new SiteEntry[0]);
		for (int i = 0; i < list.length; i++) {
			writeSite(w, CFG_SITE + "." + Integer.toString(i), list[i]); //$NON-NLS-1$
		}

		// write end-of-file marker
		writeAttribute(w, EOF, EOF);
	}

	private void writeSite(PrintWriter w, String id, SiteEntry entry) {

		// write site separator
		w.println(""); //$NON-NLS-1$

		// write out site settings
		writeAttribute(w, id + "." + CFG_URL, entry.getURL().toString()); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_STAMP, Long.toString(entry.getChangeStamp())); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_FEATURE_STAMP, Long.toString(entry.getFeaturesChangeStamp())); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_PLUGIN_STAMP, Long.toString(entry.getPluginsChangeStamp())); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_UPDATEABLE, entry.updateable ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (entry.linkFileName != null && !entry.linkFileName.trim().equals("")) //$NON-NLS-1$
			writeAttribute(w, id + "." + CFG_LINK_FILE, entry.linkFileName.trim().replace(File.separatorChar, '/')); //$NON-NLS-1$

		// write out site policy
		int type = entry.getSitePolicy().getType();
		String typeString = CFG_POLICY_TYPE_UNKNOWN;
		try {
			typeString = CFG_POLICY_TYPE[type];
		} catch (IndexOutOfBoundsException e) {
			// ignore bad attribute ...
		}
		writeAttribute(w, id + "." + CFG_POLICY, typeString); //$NON-NLS-1$
		writeListAttribute(w, id + "." + CFG_LIST, entry.getSitePolicy().getList()); //$NON-NLS-1$
	}

	private void writeFeatureEntry(PrintWriter w, String id, IFeatureEntry entry) {

		// write feature entry separator
		w.println(""); //$NON-NLS-1$

		// write out feature entry settings
		writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_ID, entry.getFeatureIdentifier()); //$NON-NLS-1$
		if (entry.canBePrimary())
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_PRIMARY, "true"); //$NON-NLS-1$ //$NON-NLS-2$
		writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_VERSION, entry.getFeatureVersion()); //$NON-NLS-1$
		if (entry.getFeatureVersion() != null && !entry.getFeatureVersion().equals(entry.getFeaturePluginVersion()))
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_PLUGIN_VERSION, entry.getFeaturePluginVersion()); //$NON-NLS-1$
		if (entry.getFeatureIdentifier() != null && !entry.getFeatureIdentifier().equals(entry.getFeaturePluginIdentifier()))
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER, entry.getFeaturePluginIdentifier()); //$NON-NLS-1$
		writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_APPLICATION, entry.getFeatureApplication()); //$NON-NLS-1$
		URL[] roots = entry.getFeatureRootURLs();
		for (int i = 0; i < roots.length; i++) {
			// write our as individual attributes (is easier for Main.java to read)
			writeAttribute(w, id + "." + CFG_FEATURE_ENTRY_ROOT + "." + i, roots[i].toExternalForm()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void writeListAttribute(PrintWriter w, String id, String[] list) {
		if (list == null || list.length == 0)
			return;

		String value = ""; //$NON-NLS-1$
		int listLen = 0;
		int listIndex = 0;
		for (int i = 0; i < list.length; i++) {
			if (listLen != 0)
				value += ","; //$NON-NLS-1$
			else
				value = ""; //$NON-NLS-1$
			value += list[i];

			if (++listLen >= CFG_LIST_LENGTH) {
				writeAttribute(w, id + "." + Integer.toString(listIndex++), value); //$NON-NLS-1$
				listLen = 0;
			}
		}
		if (listLen != 0)
			writeAttribute(w, id + "." + Integer.toString(listIndex), value); //$NON-NLS-1$
	}

	private void writeAttribute(PrintWriter w, String id, String value) {
		if (value == null || value.trim().equals("")) //$NON-NLS-1$
			return;
		w.println(id + "=" + escapedValue(value)); //$NON-NLS-1$
	}

	private String escapedValue(String value) {
		// if required, escape property values as \\uXXXX
		StringBuffer buf = new StringBuffer(value.length() * 2); // assume expansion by less than factor of 2
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (character == '\\' || character == '\t' || character == '\r' || character == '\n' || character == '\f') {
				// handle characters requiring leading \
				buf.append('\\');
				buf.append(character);
			} else if ((character < 0x0020) || (character > 0x007e)) {
				// handle characters outside base range (encoded)
				buf.append('\\');
				buf.append('u');
				buf.append(HEX[(character >> 12) & 0xF]); // first nibble
				buf.append(HEX[(character >> 8) & 0xF]); // second nibble
				buf.append(HEX[(character >> 4) & 0xF]); // third nibble
				buf.append(HEX[character & 0xF]); // fourth nibble
			} else {
				// handle base characters
				buf.append(character);
			}
		}
		return buf.toString();
	}




	private static boolean supportsDetection(URL url) {
		String protocol = url.getProtocol();
		if (protocol.equals("file")) //$NON-NLS-1$
			return true;
		else if (protocol.equals(PlatformURLHandler.PROTOCOL)) {
			URL resolved = null;
			try {
				resolved = resolvePlatformURL(url); // 19536
			} catch (IOException e) {
				return false; // we tried but failed to resolve the platform URL
			}
			return resolved.getProtocol().equals("file"); //$NON-NLS-1$
		} else
			return false;
	}

	private static URL resolvePlatformURL(URL url) throws IOException {
		// 19536
		if (url.getProtocol().equals(PlatformURLHandler.PROTOCOL)) {
			URLConnection connection = url.openConnection();
			if (connection instanceof PlatformURLConnection) {
				url = ((PlatformURLConnection) connection).getResolvedURL();
			} else {
				//				connection = new PlatformURLBaseConnection(url);
				//				url = ((PlatformURLConnection)connection).getResolvedURL();
				url = getInstallURL();
			}
		}
		return url;
	}

	private static void debug(String s) {
		System.out.println("PlatformConfig: " + s); //$NON-NLS-1$
	}

	private void resetUpdateManagerState(URL url) throws IOException {
		// [20111]
		if (!supportsDetection(url))
			return; // can't do ...

		// find directory where the platform configuration file is	
		URL resolved = resolvePlatformURL(url);
		File initCfg = new File(resolved.getFile().replace('/', File.separatorChar));
		File initDir = initCfg.getParentFile();

		// Find the Update Manager State directory
		if (initDir == null || !initDir.exists() || !initDir.isDirectory())
			return;
		String temp = initCfg.getName() + ".metadata"; //$NON-NLS-1$
		File UMDir = new File(initDir, temp + '/');

		// Attempt to rename it
		if (UMDir == null || !UMDir.exists() || !UMDir.isDirectory())
			return;
		Date now = new Date();
		boolean renamed = UMDir.renameTo(new File(initDir, temp + now.getTime() + '/'));

		if (!renamed)
			resetInitializationLocation(UMDir);
	}

	private static URL getInstallURL() {
		return installURL;
	}


}
