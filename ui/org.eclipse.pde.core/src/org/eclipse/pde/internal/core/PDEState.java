/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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
import java.util.jar.*;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;
import org.w3c.dom.*;

public class PDEState {
	
	class PluginInfo {
		String name;
		String providerName;
		String className;
		String[] libraries;
	}
	
	private static boolean DEBUG = false;	
	private static StateObjectFactory stateObjectFactory;
	private static String DIR;
	
	protected State fState;
	private long fId;
	private URL[] fURLs;
	private boolean fResolve;
	
	private String fTargetMode = null;
	private PluginConverter fConverter = null;
	private IProgressMonitor fMonitor;

	private IPluginModelBase[] fModels;
	private HashMap fPluginInfos;
	private HashMap fExtensions;
	
	static {
		DEBUG  = PDECore.getDefault().isDebugging() 
					&& "true".equals(Platform.getDebugOption("org.eclipse.pde.core/cache"));
		DIR = DIR = PDECore.getDefault().getStateLocation().append(".cache").toOSString();
		stateObjectFactory = Platform.getPlatformAdmin().getFactory();
	}
	
	public PDEState() {
		fMonitor = new NullProgressMonitor();
		fPluginInfos = new HashMap();
		fState = stateObjectFactory.createState();
		fState.setResolver(Platform.getPlatformAdmin().getResolver());
	}
	
	public PDEState(URL[] urls, boolean resolve, IProgressMonitor monitor) {
		fURLs = urls;
		fMonitor = monitor;
		fResolve = resolve;
		fPluginInfos = new HashMap();

		long start = System.currentTimeMillis();
		if (fResolve) {
			long timestamp = computeTimestamp(fURLs);
			if (!readStateCache(timestamp) || !reachPluginInfoCache(timestamp)) {
				createState();
				saveState(timestamp);
				savePluginInfo(timestamp);
			}
			if (!readExtensionsCache(timestamp)) {
				saveExtensions(timestamp);
			}
		} else {
			createState();
		}
		fState.setResolver(Platform.getPlatformAdmin().getResolver());
		fState.resolve();
		if (fResolve)
			logResolutionErrors();
		createModels();
		
		long end = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("Total time elapsed to initialize models: " + (end - start) + " ms");
	}
	
	private boolean reachPluginInfoCache(long timestamp) {
		File file = new File(DIR, Long.toString(timestamp) + ".pluginInfo");
		if (file.exists() && file.isFile()) {
			long start = System.currentTimeMillis();
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList bundles = root.getElementsByTagName("bundle");
					for (int i = 0; i < bundles.getLength(); i++) {
						createPluginInfo((Element)bundles.item(i));
					}
				}
				return true;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			} finally {
				long end = System.currentTimeMillis();
				if (DEBUG)
					System.out.println("########Time to read plugin info from cache: " + (end - start) + "ms");
			}
		} 
		return false;
	}

	private boolean readExtensionsCache(long timestamp) {
		fExtensions = new HashMap();
		File file = new File(DIR, Long.toString(timestamp) + ".extensions");
		if (file.exists() && file.isFile()) {
			long start = System.currentTimeMillis();
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				Document doc = factory.newDocumentBuilder().parse(file);
				Element root = doc.getDocumentElement();
				if (root != null) {
					NodeList bundles = root.getElementsByTagName("bundle");
					for (int i = 0; i < bundles.getLength(); i++) {
						Element bundle = (Element)bundles.item(i); 
						String id = bundle.getAttribute("bundleID");
						fExtensions.put(id, bundle.getChildNodes());
					}
				}
				return true;
			} catch (org.xml.sax.SAXException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} catch (ParserConfigurationException e) {
				PDECore.log(e);
			} finally {
				long end = System.currentTimeMillis();
				if (DEBUG)
					System.out.println("########Time to read extensions from cache: " + (end - start) + "ms");
			}
		} 
		return false;
	}
	
	private boolean readStateCache(long timestamp) {
		File file = new File(DIR, Long.toString(timestamp) + ".state");
		if (file.exists() && file.isFile()) {
			long start = System.currentTimeMillis();
			InputStream stream = null;
			try {
				stream = new FileInputStream(file);
				fState = stateObjectFactory.readState(stream);	
				return true;
			} catch (IllegalStateException e) {
				PDECore.log(e);
			} catch (FileNotFoundException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			} finally {
				try {
					if (stream != null)
						stream.close();
				} catch (IOException e1) {
				}
				long end = System.currentTimeMillis();
				if (DEBUG)
					System.out.println("########Time to read state from cache: " + (end - start) + "ms");
			}
		} 
		return false;
	}

	private void createState() {
		long start = System.currentTimeMillis();
		fState = stateObjectFactory.createState();
		fPluginInfos.clear();
		populate();
		long end = System.currentTimeMillis();
		
		if (DEBUG)
			System.out.println("########Time to create state from scratch: " + (end - start) + "ms");
	}
	
	private void saveState(long timestamp) {
		long start = System.currentTimeMillis();
		File file = createFile(timestamp, "state");
		OutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			stateObjectFactory.writeState(fState, stream);
		} catch (FileNotFoundException e) {
			PDECore.log(e);
		} catch (IOException e) {
			PDECore.log(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e1) {
			}
		}
		long end = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("########Time to save new state: " + (end - start) + "ms");
	}
	
	private void savePluginInfo(long timestamp) {
		long start = System.currentTimeMillis();
		File file = createFile(timestamp, "pluginInfo");
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("map");
			
			Iterator iter = fPluginInfos.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next().toString();
				Element element = doc.createElement("bundle");
				element.setAttribute("bundleID", key);
				PluginInfo info = (PluginInfo)fPluginInfos.get(key);
				if (info.className != null)
					element.setAttribute("class", info.className);
				if (info.providerName != null)
					element.setAttribute("provider", info.providerName);
				if (info.name != null)
					element.setAttribute("name", info.name);
				if (info.libraries != null) {
					for (int i = 0; i < info.libraries.length; i++) {
						Element lib = doc.createElement("library");
						lib.setAttribute("name", info.libraries[i]);
						element.appendChild(lib);
					}
				}
				root.appendChild(element);
			}
			doc.appendChild(root);
			XMLPrintHandler.printNode(writer, doc, "UTF-8");
		} catch (Exception e) {
			PDECore.log(e);
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e1) {
			}
		}
		long end = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("########Time to save new aux plugin info: " + (end - start) + "ms");
	}
	
	private void saveExtensions(long timestamp) {
		fExtensions = new HashMap();
		long start = System.currentTimeMillis();
		File file = createFile(timestamp, "extensions");
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			Document doc = factory.newDocumentBuilder().newDocument();
			Element root = doc.createElement("extensions");
			
			BundleDescription[] bundles = fState.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription desc = bundles[i];
				Element element = doc.createElement("bundle");
				element.setAttribute("bundleID", Long.toString(desc.getBundleId()));
				PDEStateHelper.parseExtensions(desc, element);
				if (element.hasChildNodes()) {
					root.appendChild(element);
					fExtensions.put(Long.toString(desc.getBundleId()), element);
				}
			}	
			doc.appendChild(root);
			XMLPrintHandler.printNode(writer, doc, "UTF-8");
		} catch (Exception e) {
			PDECore.log(e);
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e1) {
			}
		}
		long end = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("########Time to parse and save extensions: " + (end - start) + "ms");
	}

	private File createFile(long timestamp, String extension) {
		File dir = new File(DIR);
		if (!dir.exists() || !dir.isDirectory()) {
			dir.mkdirs();
		} else {
			File[] files = dir.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					File file = files[i];
					if (file.isFile() && file.getName().endsWith("." + extension))
						file.delete();
				}
			}
		}	
		return new File(dir, Long.toString(timestamp) + "." + extension);
	}

	private long getNextId() {
		return ++fId;
	}
	
	private StateHelper acquireStateHelper(){
		return PDECore.getDefault().acquirePlatform().getStateHelper();
	}
	
	private BundleDescription findActiveBundle(String symbolicName) {
		BundleDescription[] bundles = fState.getBundles(symbolicName);
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].isResolved())
				return bundles[i];
		}
		return null;
	}

	private void logResolutionErrors() {
		MultiStatus errors =
			new MultiStatus(
				PDECore.getPluginId(),
				1,
				PDECore.getResourceString("ExternalModelManager.scanningProblems"),
				null);
		
		StateHelper helper = acquireStateHelper();
		BundleDescription[] all = fState.getBundles();
		for (int i = 0; i < all.length; i++) {
			if (!all[i].isResolved()) {
				VersionConstraint[] unsatisfiedConstraints = helper.getUnsatisfiedConstraints(all[i]);
				if (unsatisfiedConstraints.length == 0) {
					BundleDescription activeBundle = findActiveBundle(all[i].getSymbolicName());
					if (activeBundle == null) {
						String message = PDECore.getFormattedMessage("ECLIPSE_IGNORE", all[i].getLocation()); //$NON-NLS-1$
						errors.add(new Status(IStatus.ERROR, all[i].getSymbolicName(), IStatus.WARNING, message, null));
					} else {
						String message = PDECore.getFormattedMessage("ECLIPSE_OTHER_VERSION", new String[] {all[i].getLocation(), activeBundle.getLocation()}); //$NON-NLS-1$
						errors.add(new Status(IStatus.INFO, all[i].getSymbolicName(), IStatus.INFO, message, null));
					}
				} else {
					for (int j = 0; j < unsatisfiedConstraints.length; j++) {
                        String message = getResolutionFailureMessage(unsatisfiedConstraints[j]);
    	                errors.add(new Status(IStatus.WARNING, all[i].getSymbolicName(), IStatus.WARNING, message, null));
					}
                }
			}
		}
		if (errors.getChildren().length > 0)
			PDECore.log(errors);		
	}
	
	private String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof PackageSpecification)
			return PDECore.getFormattedMessage("ECLIPSE_MISSING_IMPORTED_PACKAGE", toString(unsatisfied)); //$NON-NLS-1$
		if (unsatisfied instanceof BundleSpecification) {
			if (((BundleSpecification) unsatisfied).isOptional())
				return PDECore.getFormattedMessage("ECLIPSE_MISSING_OPTIONAL_REQUIRED_BUNDLE", toString(unsatisfied)); //$NON-NLS-1$
			return PDECore.getFormattedMessage("ECLIPSE_MISSING_REQUIRED_BUNDLE", toString(unsatisfied)); //$NON-NLS-1$
		}
		return PDECore.getFormattedMessage("ECLIPSE_MISSING_HOST", toString(unsatisfied)); //$NON-NLS-1$
	}
	
	private String toString(VersionConstraint constraint) {
		VersionRange versionRange = constraint.getVersionRange();
		if (versionRange == null || versionRange.getMinimum() != null)
			return constraint.getName();
		return constraint.getName() + '_' + versionRange;
	}

	private void populate() {
		if (fURLs == null || fURLs.length == 0)
			return;
		setTargetMode();
		fMonitor.beginTask("", fURLs.length); //$NON-NLS-1$
		for (int i = 0; i < fURLs.length; i++) {
			addBundle(new File(fURLs[i].getFile()), true, true);
			fMonitor.worked(1);
		}
	}
	
	private long computeTimestamp(URL[] urls) {
		long timestamp = 0;
		for (int i = 0; i < urls.length; i++) {
			File file = new File(urls[i].getFile());
			if (file.exists()) {
				if (file.isFile()) {
					timestamp ^= file.lastModified();
				} else {
					File manifest = new File(file, "META-INF/MANIFEST.MF");
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "plugin.xml");
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
					manifest = new File(file, "fragment.xml");
					if (manifest.exists())
						timestamp ^= manifest.lastModified();
				}
			}
		}
		return timestamp;
	}
	
	public void addBundleDescription(BundleDescription toAdd) {
		fState.addBundle(toAdd);
	}
	
	private PluginConverter acquirePluginConverter() throws Exception {
		if (fConverter == null) { 
			ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			fConverter = (PluginConverter) tracker.getService();
			tracker.close();
		}
		return fConverter;
	}
	
	public BundleDescription addBundle(Dictionary manifest, File bundleLocation, boolean keepLibraries) {
		try {
			BundleDescription descriptor = stateObjectFactory.createBundleDescription(manifest, bundleLocation.getAbsolutePath(), getNextId());
			if (keepLibraries)
				createPluginInfo(descriptor, manifest);
			fState.addBundle(descriptor);
			return descriptor;
		} catch (BundleException e) {
		}
		return null;
	}
	
	private void createPluginInfo(BundleDescription desc, Dictionary manifest) {
		PluginInfo info = new PluginInfo();
		info.name = (String)manifest.get(Constants.BUNDLE_NAME);
		info.providerName = (String)manifest.get(Constants.BUNDLE_VENDOR);
		
		String className = (String)manifest.get("Plugin-Class");
		info.className	= className != null ? className : (String)manifest.get(Constants.BUNDLE_ACTIVATOR);	
		info.libraries = PDEStateHelper.getClasspath(manifest);
		
		fPluginInfos.put(Long.toString(desc.getBundleId()), info);
	}
	
	private void createPluginInfo(Element element) {
		PluginInfo info = new PluginInfo();
		info.name = element.getAttribute("name");
		info.providerName = element.getAttribute("provider");
		info.className	= element.getAttribute("class");
		
		NodeList libs = element.getElementsByTagName("library");
		info.libraries = new String[libs.getLength()];
		for (int i = 0; i < libs.getLength(); i++) {
			Element lib = (Element)libs.item(i);
			info.libraries[i] = lib.getAttribute("name");
		}
		fPluginInfos.put(element.getAttribute("bundleID"), info);
	}
	public BundleDescription addBundle(File bundleLocation) {
		return addBundle(bundleLocation, false, false);
	}
	
	public BundleDescription addBundle(File bundleLocation, boolean keepLibraries, boolean logException) {
		Dictionary manifest =  loadManifest(bundleLocation);
		if (manifest == null || manifest.get(Constants.BUNDLE_SYMBOLICNAME) == null) {
			try {
				if (!new File(bundleLocation, "plugin.xml").exists() &&  //$NON-NLS-1$
						!new File(bundleLocation, "fragment.xml").exists()) //$NON-NLS-1$
					return null;
				PluginConverter converter = acquirePluginConverter();
				manifest = converter.convertManifest(bundleLocation, false, getTargetMode(), false);
				if (manifest == null || manifest.get(Constants.BUNDLE_SYMBOLICNAME) == null)
					throw new Exception();
			} catch (Exception e1) {
				if (logException)
					PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, "Error parsing plugin manifest file at " + bundleLocation.toString(), null)); //$NON-NLS-1$
				return null;
			}
		}
		return addBundle(manifest, bundleLocation, keepLibraries);
	}
	
	private Dictionary loadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		try {
			URL manifestLocation = null;
			String extension = new Path(bundleLocation.getName()).getFileExtension();
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				manifestLocation = new URL("jar:file:" + bundleLocation + "!/" + JarFile.MANIFEST_NAME); //$NON-NLS-1$ //$NON-NLS-2$
				manifestStream = manifestLocation.openStream();
			} else {
				manifestStream = new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME));
			}
		} catch (IOException e) {
		}
		if (manifestStream == null) 
			return null;
		try {
			Manifest m = new Manifest(manifestStream);
			return manifestToProperties(m.getMainAttributes());
		} catch (IOException e) {
			PDECore.log(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, PDECore.getResourceString("PDEState.invalidFormat") + bundleLocation.toString(), null)); //$NON-NLS-1$
			return null;
		} finally {
			try {
				manifestStream.close();
			} catch (IOException e1) {
			}
		}
	}
	
	private Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}
	
	public StateDelta resolveState() {
		return fState.resolve(false);
	}
	
	public void resolveState(boolean incremental) {
		fState.resolve(incremental);
	}
	
	public void removeBundleDescription(BundleDescription description) {
		fState.removeBundle(description);
	}
	
	public State getState() {
		return fState;
	}
	
	public void setTargetMode() {
		fTargetMode = "2.1"; //$NON-NLS-1$
		for (int i = 0; i < fURLs.length; i++) {
			if (fURLs[i].getFile().indexOf("org.eclipse.osgi") != -1) {//$NON-NLS-1$
				fTargetMode = null;
				break;
			}
		}			
	}
	
	public String getTargetMode() {
		return fTargetMode;
	}
	
	private void createModels() {
		long start = System.currentTimeMillis();
		BundleDescription[] bundleDescriptions = fResolve ? fState.getResolvedBundles() : fState.getBundles();
		fModels = new IPluginModelBase[bundleDescriptions.length];
		for (int i = 0; i < bundleDescriptions.length; i++) {
			BundleDescription desc = bundleDescriptions[i];
			fMonitor.subTask(bundleDescriptions[i].getSymbolicName());
			ExternalPluginModelBase model = null;
			if (desc.getHost() == null)
				model = new ExternalPluginModel();
			else
				model = new ExternalFragmentModel();
			model.load(desc, this, !fResolve);
			fModels[i] = model;
		}
		long end = System.currentTimeMillis();
		
		if (DEBUG) {
			System.out.println("########Time to populate models: " + (end - start) + " ms");
		}
		if (fExtensions != null)
			fExtensions.clear();
		fPluginInfos.clear();
		fMonitor.done();		
	}
	
	public IPluginModelBase[] getModels() {
		return fModels;
	}
	
	public String getClassName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.className;
	}
	
	public String getPluginName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.name;
	}
	
	public String getProviderName(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? null : info.providerName;
	}
	
	public String[] getLibraryNames(long bundleID) {
		PluginInfo info = (PluginInfo)fPluginInfos.get(Long.toString(bundleID));
		return info == null ? new String[0] : info.libraries;
	}
	
	public NodeList getExtensions(long bundleID) {
		return getChildren(bundleID, "extension");
	}
	
	public NodeList getExtensionPoints(long bundleID) {
		return getChildren(bundleID, "extension-point");
	}
	
	private NodeList getChildren(long bundleID, String tagName) {
		if (fExtensions != null) {
			Element bundle = (Element)fExtensions.get(Long.toString(bundleID));
			if (bundle != null) {
				return bundle.getElementsByTagName(tagName);
			}
		}
		return null;
	}

}