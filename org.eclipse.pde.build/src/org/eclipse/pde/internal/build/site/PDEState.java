/**********************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.site;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.build.*;
import org.osgi.framework.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

// This class provides a higher level API on the state
public class PDEState implements IPDEBuildConstants, IBuildPropertiesConstants {
	private StateObjectFactory factory;
	protected State state;
	private long id;
	private Properties repositoryVersions;
	private HashMap bundleClasspaths;

	protected long getNextId() {
		return ++id;
	}

	public PDEState() {
		factory = Platform.getPlatformAdmin().getFactory();
		state = factory.createState();
		state.setResolver(Platform.getPlatformAdmin().getResolver());
		id = 0;
		bundleClasspaths = new HashMap();
		loadPluginVersionFile();
	}

	public StateObjectFactory getFactory() {
		return factory;
	}

	public void addBundleDescription(BundleDescription toAdd) {
		state.addBundle(toAdd);
	}

	private PluginConverter acquirePluginConverter() throws Exception {
		return (PluginConverter) BundleHelper.getDefault().acquireService(PluginConverter.class.getName());
	}

	//Add a bundle to the state, updating the version number 
	public boolean addBundle(Dictionary enhancedManifest, File bundleLocation) {
		updateVersionNumber(enhancedManifest);
		try {
			BundleDescription descriptor;
			descriptor = factory.createBundleDescription(enhancedManifest, bundleLocation.getAbsolutePath(), getNextId());
			bundleClasspaths.put(new Long(descriptor.getBundleId()), getClasspath(enhancedManifest));
			state.addBundle(descriptor);
		} catch (BundleException e) {
			IStatus status = new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, EXCEPTION_STATE_PROBLEM, Policy.bind("exception.stateAddition", (String) enhancedManifest.get(Constants.BUNDLE_NAME)), e);//$NON-NLS-1$
			BundleHelper.getDefault().getLog().log(status);
			return false;
		}
		return true;
	}

	private String[] getClasspath(Dictionary manifest) {
		String fullClasspath = (String) manifest.get(Constants.BUNDLE_CLASSPATH);
		String[] result = new String[0];
		try {
			if (fullClasspath != null) {
				ManifestElement[] classpathEntries;
				classpathEntries = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, fullClasspath);
				result = new String[classpathEntries.length];
				for (int i = 0; i < classpathEntries.length; i++) {
					result[i] = classpathEntries[i].getValue();
				}
			}
		} catch (BundleException e) {
			//Ignore
		}
		return result;
	}

	private void loadPluginVersionFile() {
		repositoryVersions = new Properties();
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(AbstractScriptGenerator.getWorkingDirectory() + '/' + DEFAULT_PLUGIN_VERSION_FILENAME_DESCRIPTOR));
			try {
				repositoryVersions.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			//Ignore
		}
	}

	public boolean addBundle(File bundleLocation) {
		Dictionary manifest;
		manifest = loadManifest(bundleLocation);
		if (manifest == null)
			return false;
		try {
			String symbolicHeader = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			if (symbolicHeader != null && ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicHeader)[0].getValue().equals("org.eclipse.osgi")) { //$NON-NLS-1$
				manifest.put(Constants.BUNDLE_CLASSPATH, findOSGiJars(bundleLocation));
			}

			hasQualifier(bundleLocation, manifest);
		} catch (BundleException e) {
			//should not happen since we know the header
		}
		return addBundle(manifest, bundleLocation);
	}

	private String findOSGiJars(File bundleLocation) {
		String eclipseProperies = "eclipse.properties"; //$NON-NLS-1$
		InputStream manifestStream = null;
		try {
			URL manifestLocation = null;
			if (bundleLocation.getName().endsWith("jar")) { //$NON-NLS-1$
				manifestLocation = new URL("jar:file:" + bundleLocation + "!/" + eclipseProperies); //$NON-NLS-1$//$NON-NLS-2$
				manifestStream = manifestLocation.openStream();
			} else {
				manifestStream = new FileInputStream(new File(bundleLocation, eclipseProperies));
			}
		} catch (IOException e) {
			//ignore
		}
		Properties properties = new Properties();
		try {
			properties.load(manifestStream);
			manifestStream.close();
		} catch (IOException e1) {
			//Ignore
		}
		String osgiPath = properties.getProperty("osgi.frameworkClassPath"); //$NON-NLS-1$
		if (osgiPath == null)
			osgiPath = "core.jar, console.jar, osgi.jar, resolver.jar, defaultAdaptor.jar, eclipseAdaptor.jar"; //$NON-NLS-1$

		return osgiPath;
	}

	private void updateVersionNumber(Dictionary manifest) {
		String newVersion = QualifierReplacer.replaceQualifierInVersion((String) manifest.get(Constants.BUNDLE_VERSION), (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), (String) manifest.get(PROPERTY_QUALIFIER), repositoryVersions);
		if (newVersion != null)
			manifest.put(Constants.BUNDLE_VERSION, newVersion);
	}

	/**
	 * @param bundleLocation
	 * @param manifest
	 * @throws BundleException
	 */
	private void hasQualifier(File bundleLocation, Dictionary manifest) throws BundleException {
		ManifestElement[] versionInfo = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, (String) manifest.get(Constants.BUNDLE_VERSION));
		if (versionInfo != null) {
			if (versionInfo[0].getValue().endsWith(PROPERTY_QUALIFIER)) {
				try {
					String qualifierInfo = AbstractScriptGenerator.readProperties(bundleLocation.getAbsolutePath(), IPDEBuildConstants.PROPERTIES_FILE, IStatus.INFO).getProperty(PROPERTY_QUALIFIER);
					//TODO Log a warning when no qualifier has been found in the manifest, or if the qualifierInfo is null
					if (qualifierInfo != null)
						manifest.put(PROPERTY_QUALIFIER, qualifierInfo);
				} catch (CoreException e1) {
					//Ignore
				}
			}
		}
	}

	private Dictionary loadManifest(File bundleLocation) {
		InputStream manifestStream = null;
		try {
			URL manifestLocation = null;
			if (bundleLocation.getName().endsWith("jar")) { //$NON-NLS-1$
				manifestLocation = new URL("jar:file:" + bundleLocation + "!/" + JarFile.MANIFEST_NAME); //$NON-NLS-1$//$NON-NLS-2$
				manifestStream = manifestLocation.openStream();
			} else {
				manifestStream = new FileInputStream(new File(bundleLocation, JarFile.MANIFEST_NAME));
			}
		} catch (IOException e) {
			//ignore
		}

		//The manifestStream is not present 
		if (manifestStream == null)
			return convertPluginManifest(bundleLocation, true);

		try {
			Manifest m = new Manifest(manifestStream);
			Properties originalManifest = manifestToProperties(m.getMainAttributes());

			//The manifest has a symbolic name
			if (originalManifest.get(Constants.BUNDLE_SYMBOLICNAME) != null) {
				//Add dot on the classpath if none has been specified
				String classpath = (String) originalManifest.get(Constants.BUNDLE_CLASSPATH);
				if (classpath == null)
					originalManifest.put(Constants.BUNDLE_CLASSPATH, "."); //$NON-NLS-1$
				return originalManifest;
			}

			//The manifest does not have a symbolic name
			Dictionary generatedManifest = convertPluginManifest(bundleLocation, false);
			if (generatedManifest == null)
				return originalManifest;
			//merge manifests
			Enumeration enumeration = originalManifest.keys();
			while (enumeration.hasMoreElements()) {
				Object key = enumeration.nextElement();
				generatedManifest.put(key, originalManifest.get(key));
			}
			return generatedManifest;
		} catch (IOException e) {
			return null;
		} finally {
			try {
				manifestStream.close();
			} catch (IOException e1) {
				//Ignore
			}
		}
	}

	private Dictionary convertPluginManifest(File bundleLocation, boolean logConversionException) {
		PluginConverter converter;
		try {
			converter = acquirePluginConverter();
			return converter.convertManifest(bundleLocation, false, AbstractScriptGenerator.isBuildingOSGi() ? null : "2.1", false); //$NON-NLS-1$
		} catch (PluginConversionException convertException) {
			if (bundleLocation.getName().equals("feature.xml")) //$NON-NLS-1$
				return null;
			if (logConversionException) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, Policy.bind("exception.errorConverting", bundleLocation.getAbsolutePath()), convertException); //$NON-NLS-1$
				BundleHelper.getDefault().getLog().log(status);
			}
			return null;
		} catch (Exception serviceException) {
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, 0, Policy.bind("exception.cannotAcquireService", "Plugin converter"), serviceException); //$NON-NLS-1$ //$NON-NLS-2$
			BundleHelper.getDefault().getLog().log(status);
			return null;
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

	public void addBundles(Collection bundles) {
		for (Iterator iter = bundles.iterator(); iter.hasNext();) {
			File bundle = (File) iter.next();
			addBundle(bundle);
		}
	}

	public void resolveState() {
		state.resolve(false);
	}

	public State getState() {
		return state;
	}

	public BundleDescription[] getDependentBundles(String bundleId, Version version) {
		BundleDescription root = state.getBundle(bundleId, version);
		return getDependentBundles(root);
	}

	/**
	 * This methods return the bundleDescriptions to which imports have been
	 * bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getImportedBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		ExportPackageDescription[] packages = root.getResolvedImports();
		ArrayList resolvedImports = new ArrayList(packages.length);
		for (int i = 0; i < packages.length; i++)
			if (!root.getLocation().equals(packages[i].getExporter().getLocation()) && !resolvedImports.contains(packages[i].getExporter()))
				resolvedImports.add(packages[i].getExporter());
		return (BundleDescription[]) resolvedImports.toArray(new BundleDescription[resolvedImports.size()]);
		}

	/**
	 * This methods return the bundleDescriptions to which required bundles
	 * have been bound to.
	 * 
	 * @param root
	 */
	public static BundleDescription[] getRequiredBundles(BundleDescription root) {
		if (root == null)
			return new BundleDescription[0];
		return root.getResolvedRequires();
		}

	public BundleDescription getResolvedBundle(String bundleId, String version) {
		if (IPDEBuildConstants.GENERIC_VERSION_NUMBER.equals(version) || version == null) {
			return getResolvedBundle(bundleId);
		}
		BundleDescription description = getState().getBundle(bundleId, Version.parseVersion(version));
		if (description != null && description.isResolved())
			return description;

		int qualifierIdx = -1;
		if ((qualifierIdx = version.indexOf('.' + IBuildPropertiesConstants.PROPERTY_QUALIFIER)) != -1) {
			BundleDescription[] bundles = getState().getBundles(bundleId);
			Version versionToMatch = Version.parseVersion(version.substring(0, qualifierIdx));
			for (int i = 0; i < bundles.length; i++) {
				Version bundleVersion = bundles[i].getVersion();
				if (bundleVersion.getMajor() == versionToMatch.getMajor() &&
						bundleVersion.getMinor() == versionToMatch.getMinor() &&
						bundleVersion.getMicro() >= versionToMatch.getMicro() &&
						bundleVersion.getQualifier().compareTo(versionToMatch.getQualifier()) >= 0)
					return bundles[i];
			}
		}
		return null;
	}

	public BundleDescription getResolvedBundle(String bundleId) {
		BundleDescription[] description = getState().getBundles(bundleId);
		if (description == null)
			return null;
		for (int i = 0; i < description.length; i++) {
			if (description[i].isResolved())
				return description[i];
		}
		return null;
	}

	public static BundleDescription[] getDependentBundles(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + required.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(required, 0, dependents, imported.length, required.length);
		return dependents;
	}

	public static BundleDescription[] getDependentBundlesWithFragments(BundleDescription root) {
		BundleDescription[] imported = getImportedBundles(root);
		BundleDescription[] importedByFragments = getImportedByFragments(root);
		BundleDescription[] required = getRequiredBundles(root);
		BundleDescription[] requiredByFragments = getRequiredByFragments(root);
		BundleDescription[] dependents = new BundleDescription[imported.length + importedByFragments.length + required.length + requiredByFragments.length];
		System.arraycopy(imported, 0, dependents, 0, imported.length);
		System.arraycopy(importedByFragments, 0, dependents, imported.length, importedByFragments.length);
		System.arraycopy(required, 0, dependents, imported.length + importedByFragments.length, required.length);
		System.arraycopy(requiredByFragments, 0, dependents, imported.length + importedByFragments.length + required.length, requiredByFragments.length);
		return dependents;
	}

	public static BundleDescription[] getImportedByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getImportedBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}

	public static BundleDescription[] getRequiredByFragments(BundleDescription root) {
		BundleDescription[] fragments = root.getFragments();
		List importedByFragments = new ArrayList();
		for (int i = 0; i < fragments.length; i++) {
			if (!fragments[i].isResolved())
				continue;
			merge(importedByFragments, getRequiredBundles(fragments[i]));
		}
		BundleDescription[] result = new BundleDescription[importedByFragments.size()];
		return (BundleDescription[]) importedByFragments.toArray(result);
	}

	public static void merge(List source, BundleDescription[] toAdd) {
		for (int i = 0; i < toAdd.length; i++) {
			if (!source.contains(toAdd[i]))
				source.add(toAdd[i]);
		}
	}

	public Properties loadPropertyFileIn(Map toMerge, File location) {
		Properties result = new Properties();
		result.putAll(toMerge);
		try {
			InputStream propertyStream = new BufferedInputStream(new FileInputStream(new File(location, PROPERTIES_FILE)));
			try {
				result.load(propertyStream);
			} finally {
				propertyStream.close();
			}
		} catch (Exception e) {
			//ignore because compiled plug-ins do not have such files
		}
		return result;
	}

	public HashMap getExtraData() {
		return bundleClasspaths;
	}

	public List getSortedBundles() {
		return Utils.computePrerequisiteOrder(Arrays.asList(getState().getResolvedBundles()));
	}
}