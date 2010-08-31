/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.helpers.DefaultHandler;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Stores additional information from the manifest files of plugins and stores
 * this information in separate xml file.  Accessed through PDEState.
 */
public class PDEAuxiliaryState {

	private static String CACHE_EXTENSION = ".pluginInfo"; //$NON-NLS-1$

	private static String ATTR_BUNDLE_ID = "bundleID"; //$NON-NLS-1$
	private static String ATTR_BUNDLE_STRUCTURE = "isBundle"; //$NON-NLS-1$
	private static String ATTR_CLASS = "class"; //$NON-NLS-1$
	private static String ATTR_EXPORTED = "exported"; //$NON-NLS-1$
	private static String ATTR_EXTENSIBLE_API = "hasExtensibleAPI"; //$NON-NLS-1$
	private static String ATTR_LOCALIZATION = "localization"; //$NON-NLS-1$
	private static String ATTR_NAME = "name"; //$NON-NLS-1$
	private static String ATTR_PATCH = "patch"; //$NON-NLS-1$
	private static String ATTR_PROJECT = "project"; //$NON-NLS-1$
	private static String ATTR_PROVIDER = "provider"; //$NON-NLS-1$
	private static String ATTR_BUNDLE_SOURCE = "bundleSource"; //$NON-NLS-1$

	private static String ELEMENT_BUNDLE = "bundle"; //$NON-NLS-1$
	private static String ELEMENT_LIB = "library"; //$NON-NLS-1$
	private static String ELEMENT_ROOT = "map"; //$NON-NLS-1$

	protected Map fPluginInfos;

	/**
	 * Constructor
	 */
	protected PDEAuxiliaryState() {
		fPluginInfos = new HashMap();
	}

	/**
	 * Constructor, gets the plugin info objects stored in the passed state
	 * and adds them to this state.
	 * @param state state containing plugin infos to initialize this state with 
	 */
	protected PDEAuxiliaryState(PDEAuxiliaryState state) {
		fPluginInfos = new HashMap(state.fPluginInfos);
	}

	/**
	 * Provides a simple way of storing auxiliary data for a plugin 
	 */
	class PluginInfo {
		String name;
		String providerName;
		String className;
		boolean hasExtensibleAPI;
		boolean isPatchFragment;
		boolean hasBundleStructure;
		String[] libraries;
		String project;
		String localization;
		String bundleSourceEntry;
	}

	/**
	 * Helper method to create a plugin info object for the given
	 * element.  The plugin info object is added to the map.
	 * @param element
	 */
	private void createPluginInfo(Element element) {
		PluginInfo info = new PluginInfo();
		if (element.hasAttribute(ATTR_NAME))
			info.name = element.getAttribute(ATTR_NAME);
		if (element.hasAttribute(ATTR_PROVIDER))
			info.providerName = element.getAttribute(ATTR_PROVIDER);
		if (element.hasAttribute(ATTR_CLASS))
			info.className = element.getAttribute(ATTR_CLASS);
		info.hasExtensibleAPI = "true".equals(element.getAttribute(ATTR_EXTENSIBLE_API)); //$NON-NLS-1$ 
		info.isPatchFragment = "true".equals(element.getAttribute(ATTR_PATCH)); //$NON-NLS-1$
		info.hasBundleStructure = !"false".equals(element.getAttribute(ATTR_BUNDLE_STRUCTURE)); //$NON-NLS-1$ 
		if (element.hasAttribute(ATTR_PROJECT))
			info.project = element.getAttribute(ATTR_PROJECT);
		if (element.hasAttribute(ATTR_LOCALIZATION))
			info.localization = element.getAttribute(ATTR_LOCALIZATION);
		if (element.hasAttribute(ATTR_BUNDLE_SOURCE))
			info.bundleSourceEntry = element.getAttribute(ATTR_BUNDLE_SOURCE);

		NodeList libs = element.getChildNodes();
		ArrayList list = new ArrayList(libs.getLength());
		for (int i = 0; i < libs.getLength(); i++) {
			if (libs.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element lib = (Element) libs.item(i);
				list.add(lib.getAttribute(ATTR_NAME));
			}
		}
		info.libraries = (String[]) list.toArray(new String[list.size()]);
		fPluginInfos.put(element.getAttribute(ATTR_BUNDLE_ID), info);
	}

	public String getClassName(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.className;
	}

	public boolean hasExtensibleAPI(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? false : info.hasExtensibleAPI;
	}

	public boolean isPatchFragment(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? false : info.isPatchFragment;
	}

	public boolean hasBundleStructure(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? false : info.hasBundleStructure;
	}

	public String getPluginName(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.name;
	}

	public String getProviderName(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.providerName;
	}

	public String[] getLibraryNames(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? new String[0] : info.libraries;
	}

	public String getBundleLocalization(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.localization;
	}

	public String getProject(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.project;
	}

	public String getBundleSourceEntry(long bundleID) {
		PluginInfo info = (PluginInfo) fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.bundleSourceEntry;
	}

	/**
	 * Builds an xml document storing the auxiliary plugin info.
	 * @param dir directory location to create the file
	 */
	protected void savePluginInfo(File dir) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement(ELEMENT_ROOT);

			Iterator iter = fPluginInfos.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next().toString();
				Element element = doc.createElement(ELEMENT_BUNDLE);
				element.setAttribute(ATTR_BUNDLE_ID, key);
				PluginInfo info = (PluginInfo) fPluginInfos.get(key);
				if (info.className != null)
					element.setAttribute(ATTR_CLASS, info.className);
				if (info.providerName != null)
					element.setAttribute(ATTR_PROVIDER, info.providerName);
				if (info.name != null)
					element.setAttribute(ATTR_NAME, info.name);
				if (info.hasExtensibleAPI)
					element.setAttribute(ATTR_EXTENSIBLE_API, "true"); //$NON-NLS-1$ 
				if (info.isPatchFragment)
					element.setAttribute(ATTR_PATCH, "true"); //$NON-NLS-1$ 
				if (!info.hasBundleStructure)
					element.setAttribute(ATTR_BUNDLE_STRUCTURE, "false"); //$NON-NLS-1$ 
				if (info.localization != null)
					element.setAttribute(ATTR_LOCALIZATION, info.localization);
				if (info.bundleSourceEntry != null)
					element.setAttribute(ATTR_BUNDLE_SOURCE, info.bundleSourceEntry);
				if (info.libraries != null) {
					for (int i = 0; i < info.libraries.length; i++) {
						Element lib = doc.createElement(ELEMENT_LIB);
						lib.setAttribute(ATTR_NAME, info.libraries[i]);
						element.appendChild(lib);
					}
				}
				root.appendChild(element);
			}
			doc.appendChild(root);
			XMLPrintHandler.writeFile(doc, new File(dir, CACHE_EXTENSION));
		} catch (Exception e) {
			PDECore.log(e);
		}
	}

	/**
	 * Loads plugin info objects from the pluginInfo xml file stored in the
	 * given directory.
	 * @param dir location to look for the pluginInfo file
	 * @return true if the file was read successfully, false otherwise
	 */
	protected boolean readPluginInfoCache(File dir) {
		File file = new File(dir, CACHE_EXTENSION);
		if (file.exists() && file.isFile()) {
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = factory.newDocumentBuilder();
				documentBuilder.setErrorHandler(new DefaultHandler());
				Document doc = documentBuilder.parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList list = root.getChildNodes();
					for (int i = 0; i < list.getLength(); i++) {
						if (list.item(i).getNodeType() == Node.ELEMENT_NODE)
							createPluginInfo((Element) list.item(i));
					}
				}
				return true;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			}
		}
		return false;
	}

	/**
	 * Returns whether the auxiliary state exists in the given directory.
	 * 
	 * @param dir parent directory
	 * @return whether the state file exist
	 */
	protected boolean exists(File dir) {
		File file = new File(dir, CACHE_EXTENSION);
		return file.exists() && file.isFile();
	}

	/**
	 * Writes out auxiliary information from the given models to an xml file
	 * in the given destination directory.
	 * @param models models to collect information from
	 * @param destination directory to create the xml file in
	 */
	public static void writePluginInfo(IPluginModelBase[] models, File destination) {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();

			Element root = doc.createElement(ELEMENT_ROOT);
			doc.appendChild(root);
			for (int i = 0; i < models.length; i++) {
				IPluginBase plugin = models[i].getPluginBase();
				BundleDescription desc = models[i].getBundleDescription();
				if (desc == null)
					continue;
				Element element = doc.createElement(ELEMENT_BUNDLE);
				element.setAttribute(ATTR_BUNDLE_ID, Long.toString(desc.getBundleId()));
				element.setAttribute(ATTR_PROJECT, models[i].getUnderlyingResource().getProject().getName());
				if (plugin instanceof IPlugin && ((IPlugin) plugin).getClassName() != null)
					element.setAttribute(ATTR_CLASS, ((IPlugin) plugin).getClassName());
				if (plugin.getProviderName() != null)
					element.setAttribute(ATTR_PROVIDER, plugin.getProviderName());
				if (plugin.getName() != null)
					element.setAttribute(ATTR_NAME, plugin.getName());
				if (ClasspathUtilCore.hasExtensibleAPI(models[i]))
					element.setAttribute(ATTR_EXTENSIBLE_API, "true"); //$NON-NLS-1$ 
				else if (ClasspathUtilCore.isPatchFragment(models[i]))
					element.setAttribute(ATTR_PATCH, "true"); //$NON-NLS-1$ 
				if (!(models[i] instanceof IBundlePluginModelBase))
					element.setAttribute(ATTR_BUNDLE_STRUCTURE, "false"); //$NON-NLS-1$ 
				if (models[i] instanceof IBundlePluginModelBase) {
					String localization = ((IBundlePluginModelBase) models[i]).getBundleLocalization();
					if (localization != null)
						element.setAttribute(ATTR_LOCALIZATION, localization);
				}
				if (models[i] instanceof IBundlePluginModelBase) {
					IBundleModel bundleModel = ((IBundlePluginModelBase) models[i]).getBundleModel();
					if (bundleModel != null) {
						String bundleSourceEntry = bundleModel.getBundle().getHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE);
						if (bundleSourceEntry != null) {
							element.setAttribute(ATTR_BUNDLE_SOURCE, bundleSourceEntry);
						}
					}
				}
				IPluginLibrary[] libraries = plugin.getLibraries();
				for (int j = 0; j < libraries.length; j++) {
					Element lib = doc.createElement(ELEMENT_LIB);
					lib.setAttribute(ATTR_NAME, libraries[j].getName());
					if (!libraries[j].isExported())
						lib.setAttribute(ATTR_EXPORTED, "false"); //$NON-NLS-1$ 
					element.appendChild(lib);
				}
				root.appendChild(element);
			}
			XMLPrintHandler.writeFile(doc, new File(destination, CACHE_EXTENSION));
		} catch (ParserConfigurationException e) {
		} catch (FactoryConfigurationError e) {
		} catch (IOException e) {
		}
	}

	/**
	 * Collects auxiliary information from the manifest and stores it in this state.
	 * @param desc bundle description for the given manifest
	 * @param manifest dictionary of headers in the bundle's manifest file
	 * @param hasBundleStructure whether the plugin has bundle structure
	 */
	protected void addAuxiliaryData(BundleDescription desc, Dictionary manifest, boolean hasBundleStructure) {
		PluginInfo info = new PluginInfo();
		info.name = (String) manifest.get(Constants.BUNDLE_NAME);
		info.providerName = (String) manifest.get(Constants.BUNDLE_VENDOR);

		String className = (String) manifest.get(ICoreConstants.PLUGIN_CLASS);
		info.className = className != null ? className : (String) manifest.get(Constants.BUNDLE_ACTIVATOR);
		info.libraries = getClasspath(manifest);
		info.hasExtensibleAPI = "true".equals(manifest.get(ICoreConstants.EXTENSIBLE_API)); //$NON-NLS-1$ 
		info.isPatchFragment = "true".equals(manifest.get(ICoreConstants.PATCH_FRAGMENT)); //$NON-NLS-1$
		info.localization = (String) manifest.get(Constants.BUNDLE_LOCALIZATION);
		info.hasBundleStructure = hasBundleStructure;
		info.bundleSourceEntry = (String) manifest.get(ICoreConstants.ECLIPSE_SOURCE_BUNDLE);
		fPluginInfos.put(Long.toString(desc.getBundleId()), info);
	}

	/**
	 * Retrieves the classpath entries from the manifest dictionary
	 * @param manifest dictionary containing manifest headers
	 * @return string array of classpath entries
	 */
	protected String[] getClasspath(Dictionary manifest) {
		String fullClasspath = (String) manifest.get(Constants.BUNDLE_CLASSPATH);
		String[] result = new String[0];
		try {
			if (fullClasspath != null) {
				ManifestElement[] classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, fullClasspath);
				result = new String[classpathEntries.length];
				for (int i = 0; i < classpathEntries.length; i++) {
					result[i] = classpathEntries[i].getValue();
				}
			}
		} catch (BundleException e) {
		}
		return result;
	}

	/**
	 * Clears the plugin info object map.
	 */
	protected void clear() {
		fPluginInfos.clear();
	}

}
