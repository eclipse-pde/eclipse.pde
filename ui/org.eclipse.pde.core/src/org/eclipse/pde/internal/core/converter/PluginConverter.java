/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.converter;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.Build;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.converter.PluginConverterParser.PluginInfo;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.*;

public class PluginConverter {
	public static boolean DEBUG = false;
	/** bundle manifest type unknown */
	static public final byte MANIFEST_TYPE_UNKNOWN = 0x00;
	/** bundle manifest type bundle (META-INF/MANIFEST.MF) */
	static public final byte MANIFEST_TYPE_BUNDLE = 0x01;
	/** bundle manifest type plugin (plugin.xml) */
	static public final byte MANIFEST_TYPE_PLUGIN = 0x02;
	/** bundle manifest type fragment (fragment.xml) */
	static public final byte MANIFEST_TYPE_FRAGMENT = 0x04;
	/** bundle manifest type jared bundle */
	static public final byte MANIFEST_TYPE_JAR = 0x08;

	private static final String SEMICOLON = "; "; //$NON-NLS-1$
	private static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
	public static final String LIST_SEPARATOR = ",\n "; //$NON-NLS-1$
	public static final String LINE_SEPARATOR = "\n "; //$NON-NLS-1$
	private static int MAXLINE = 511;
	private BundleContext context;
	private PluginInfo pluginInfo;
	private File pluginManifestLocation;
	private Dictionary generatedManifest;
	private byte manifestType;
	private Version target;
	static final Version TARGET31 = new Version(3, 1, 0);
	static final Version TARGET32 = new Version(3, 2, 0);
	static final Version TARGET34 = new Version(3, 4, 0);
	private static final String MANIFEST_VERSION = "Manifest-Version"; //$NON-NLS-1$
	private static final String PLUGIN_PROPERTIES_FILENAME = "plugin"; //$NON-NLS-1$
	private static PluginConverter instance;
	static public final String GENERATED_FROM = "Generated-from"; //$NON-NLS-1$
	static public final String MANIFEST_TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
	protected static final String PI_BOOT = "org.eclipse.core.boot"; //$NON-NLS-1$
	protected static final String PI_RUNTIME_COMPATIBILITY = "org.eclipse.core.runtime.compatibility"; //$NON-NLS-1$
	private static final String COMPATIBILITY_ACTIVATOR = "org.eclipse.core.internal.compatibility.PluginActivator"; //$NON-NLS-1$
	private static final String SOURCE_PREFIX = "source."; //$NON-NLS-1$

	public static PluginConverter getDefault() {
		if (instance == null)
			instance = new PluginConverter(PDECore.getDefault().getBundleContext());
		return instance;
	}

	public PluginConverter(BundleContext context) {
		this.context = context;
		instance = this;
	}

	private void init() {
		// need to make sure these fields are cleared out for each conversion.
		pluginInfo = null;
		pluginManifestLocation = null;
		generatedManifest = new Hashtable(10);
		manifestType = MANIFEST_TYPE_UNKNOWN;
		target = null;
	}

	private void fillPluginInfo(File pluginBaseLocation) throws PluginConversionException {
		pluginManifestLocation = pluginBaseLocation;
		if (pluginManifestLocation == null)
			throw new IllegalArgumentException();
		URL pluginFile = findPluginManifest(pluginBaseLocation);
		if (pluginFile == null) {
			throw new PluginConversionException(NLS.bind(PDECoreMessages.PluginConverter_EclipseConverterFileNotFound, pluginBaseLocation.getAbsolutePath()));
		}
		pluginInfo = parsePluginInfo(pluginFile);
		String validation = pluginInfo.validateForm();
		if (validation != null)
			throw new PluginConversionException(validation);
	}

	private URL findPluginManifest(File baseLocation) {
		//Here, we can not use the bundlefile because it may explode the jar and returns a location from which we will not be able to derive the jars location 
		URL xmlFileLocation;
		InputStream stream = null;
		URL baseURL = null;
		try {
			if (!baseLocation.isDirectory()) {
				baseURL = new URL("jar:file:" + baseLocation.toString() + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
				manifestType |= MANIFEST_TYPE_JAR;
			} else {
				baseURL = baseLocation.toURL();
			}
		} catch (MalformedURLException e1) {
			//this can't happen since we are building the urls ourselves from a file
		}
		try {
			xmlFileLocation = new URL(baseURL, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			stream = xmlFileLocation.openStream();
			manifestType |= MANIFEST_TYPE_PLUGIN;
			return xmlFileLocation;
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException ioe) {
			//ignore
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				//ignore
			}
		}
		try {
			xmlFileLocation = new URL(baseURL, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
			stream = xmlFileLocation.openStream();
			manifestType |= MANIFEST_TYPE_FRAGMENT;
			return xmlFileLocation;
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException ioe) {
			// Ignore
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				//ignore
			}
		}
		return null;
	}

	protected void fillManifest(boolean compatibilityManifest, boolean analyseJars) {
		generateManifestVersion();
		generateHeaders();
		generateClasspath();
		generateActivator();
		generatePluginClass();
		if (analyseJars)
			generateProvidePackage();
		generateRequireBundle();
		generateLocalizationEntry();
		generateEclipseHeaders();
		if (compatibilityManifest) {
			generateTimestamp();
		}
	}

	public void writeManifest(File generationLocation, Map manifestToWrite, boolean compatibilityManifest) throws PluginConversionException {
		long start = System.currentTimeMillis();
		BufferedWriter out = null;
		try {
			File parentFile = new File(generationLocation.getParent());
			parentFile.mkdirs();
			generationLocation.createNewFile();
			if (!generationLocation.isFile()) {
				String message = NLS.bind(PDECoreMessages.PluginConverter_EclipseConverterErrorCreatingBundleManifest, this.pluginInfo.getUniqueId(), generationLocation);
				throw new PluginConversionException(message);
			}
			// MANIFEST.MF files must be written using UTF-8
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(generationLocation), UTF_8));
			writeManifest(manifestToWrite, out);
		} catch (IOException e) {
			String message = NLS.bind(PDECoreMessages.PluginConverter_EclipseConverterErrorCreatingBundleManifest, this.pluginInfo.getUniqueId(), generationLocation);
			throw new PluginConversionException(message, e);
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// only report problems writing to/flushing the file
				}
		}
		if (DEBUG)
			System.out.println("Time to write out converted manifest to: " + generationLocation + ": " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public void writeManifest(Map manifestToWrite, Writer out) throws IOException {
		// replaces any eventual existing file
		manifestToWrite = new Hashtable(manifestToWrite);

		writeEntry(out, MANIFEST_VERSION, (String) manifestToWrite.remove(MANIFEST_VERSION));
		writeEntry(out, GENERATED_FROM, (String) manifestToWrite.remove(GENERATED_FROM)); //Need to do this first uptoDate check expect the generated-from tag to be in the first line
		// always attempt to write the Bundle-ManifestVersion header if it exists (bug 109863)
		writeEntry(out, Constants.BUNDLE_MANIFESTVERSION, (String) manifestToWrite.remove(Constants.BUNDLE_MANIFESTVERSION));
		writeEntry(out, Constants.BUNDLE_NAME, (String) manifestToWrite.remove(Constants.BUNDLE_NAME));
		writeEntry(out, Constants.BUNDLE_SYMBOLICNAME, (String) manifestToWrite.remove(Constants.BUNDLE_SYMBOLICNAME));
		writeEntry(out, Constants.BUNDLE_VERSION, (String) manifestToWrite.remove(Constants.BUNDLE_VERSION));
		writeEntry(out, Constants.BUNDLE_CLASSPATH, (String) manifestToWrite.remove(Constants.BUNDLE_CLASSPATH));
		writeEntry(out, Constants.BUNDLE_ACTIVATOR, (String) manifestToWrite.remove(Constants.BUNDLE_ACTIVATOR));
		writeEntry(out, Constants.BUNDLE_VENDOR, (String) manifestToWrite.remove(Constants.BUNDLE_VENDOR));
		writeEntry(out, Constants.FRAGMENT_HOST, (String) manifestToWrite.remove(Constants.FRAGMENT_HOST));
		writeEntry(out, Constants.BUNDLE_LOCALIZATION, (String) manifestToWrite.remove(Constants.BUNDLE_LOCALIZATION));
		// always attempt to write the Export-Package header if it exists (bug 109863)
		writeEntry(out, Constants.EXPORT_PACKAGE, (String) manifestToWrite.remove(Constants.EXPORT_PACKAGE));
		// always attempt to write the Provide-Package header if it exists (bug 109863)
		writeEntry(out, ICoreConstants.PROVIDE_PACKAGE, (String) manifestToWrite.remove(ICoreConstants.PROVIDE_PACKAGE));
		writeEntry(out, Constants.REQUIRE_BUNDLE, (String) manifestToWrite.remove(Constants.REQUIRE_BUNDLE));
		Iterator keys = manifestToWrite.keySet().iterator();
		// TODO makes sure the update works from Dictionary
		while (keys.hasNext()) {
			String key = (String) keys.next();
			writeEntry(out, key, (String) manifestToWrite.get(key));
		}
		out.flush();
	}

	private void generateLocalizationEntry() {
		generatedManifest.put(Constants.BUNDLE_LOCALIZATION, PLUGIN_PROPERTIES_FILENAME);
	}

	private void generateManifestVersion() {
		generatedManifest.put(MANIFEST_VERSION, "1.0"); //$NON-NLS-1$ 
	}

	private boolean requireRuntimeCompatibility() {
		ArrayList requireList = pluginInfo.getRequires();
		for (Iterator iter = requireList.iterator(); iter.hasNext();) {
			if (((PluginConverterParser.Prerequisite) iter.next()).getName().equalsIgnoreCase(PI_RUNTIME_COMPATIBILITY))
				return true;
		}
		return false;
	}

	private void generateActivator() {
		if (!pluginInfo.isFragment())
			if (!requireRuntimeCompatibility()) {
				String pluginClass = pluginInfo.getPluginClass();
				if (pluginClass != null && !pluginClass.trim().equals("")) //$NON-NLS-1$
					generatedManifest.put(Constants.BUNDLE_ACTIVATOR, pluginClass);
			} else {
				generatedManifest.put(Constants.BUNDLE_ACTIVATOR, COMPATIBILITY_ACTIVATOR);
			}
	}

	private void generateClasspath() {
		String[] classpath = pluginInfo.getLibrariesName();
		if (classpath.length != 0)
			generatedManifest.put(Constants.BUNDLE_CLASSPATH, getStringFromArray(classpath, LIST_SEPARATOR));
	}

	private void generateHeaders() {
		if (TARGET31.compareTo(target) <= 0)
			generatedManifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		generatedManifest.put(Constants.BUNDLE_NAME, pluginInfo.getPluginName());
		generatedManifest.put(Constants.BUNDLE_VERSION, pluginInfo.getVersion());
		generatedManifest.put(Constants.BUNDLE_SYMBOLICNAME, getSymbolicNameEntry());
		String provider = pluginInfo.getProviderName();
		if (provider != null)
			generatedManifest.put(Constants.BUNDLE_VENDOR, provider);
		if (pluginInfo.isFragment()) {
			StringBuffer hostBundle = new StringBuffer();
			hostBundle.append(pluginInfo.getMasterId());
			String versionRange = getVersionRange(pluginInfo.getMasterVersion(), pluginInfo.getMasterMatch()); // TODO need to get match rule here!
			if (versionRange != null)
				hostBundle.append(versionRange);
			generatedManifest.put(Constants.FRAGMENT_HOST, hostBundle.toString());
		}
	}

	/*
	 * Generates an entry in the form: 
	 * 	<symbolic-name>[; singleton=true]
	 */
	private String getSymbolicNameEntry() {
		// false is the default, so don't bother adding anything 
		if (!pluginInfo.isSingleton())
			return pluginInfo.getUniqueId();
		StringBuffer result = new StringBuffer(pluginInfo.getUniqueId());
		result.append(SEMICOLON);
		result.append(Constants.SINGLETON_DIRECTIVE);
		String assignment = TARGET31.compareTo(target) <= 0 ? ":=" : "="; //$NON-NLS-1$ //$NON-NLS-2$
		result.append(assignment).append("true"); //$NON-NLS-1$
		return result.toString();
	}

	private void generatePluginClass() {
		if (requireRuntimeCompatibility()) {
			String pluginClass = pluginInfo.getPluginClass();
			if (pluginClass != null)
				generatedManifest.put(ICoreConstants.PLUGIN_CLASS, pluginClass);
		}
	}

	private void generateProvidePackage() {
		Collection exports = getExports();
		if (exports != null && exports.size() != 0) {
			generatedManifest.put(TARGET31.compareTo(target) <= 0 ? Constants.EXPORT_PACKAGE : ICoreConstants.PROVIDE_PACKAGE, getStringFromCollection(exports, LIST_SEPARATOR));
		}
	}

	private void generateRequireBundle() {
		ArrayList requiredBundles = pluginInfo.getRequires();
		if (requiredBundles.size() == 0)
			return;
		StringBuffer bundleRequire = new StringBuffer();
		for (Iterator iter = requiredBundles.iterator(); iter.hasNext();) {
			PluginConverterParser.Prerequisite element = (PluginConverterParser.Prerequisite) iter.next();
			StringBuffer modImport = new StringBuffer(element.getName());
			String versionRange = getVersionRange(element.getVersion(), element.getMatch());
			if (versionRange != null)
				modImport.append(versionRange);
			if (element.isExported()) {
				if (TARGET31.compareTo(target) <= 0)
					modImport.append(';').append(Constants.VISIBILITY_DIRECTIVE).append(":=").append(Constants.VISIBILITY_REEXPORT);//$NON-NLS-1$
				else
					modImport.append(';').append(ICoreConstants.REPROVIDE_ATTRIBUTE).append("=true");//$NON-NLS-1$
			}
			if (element.isOptional()) {
				if (TARGET31.compareTo(target) <= 0)
					modImport.append(';').append(Constants.RESOLUTION_DIRECTIVE).append(":=").append(Constants.RESOLUTION_OPTIONAL);//$NON-NLS-1$
				else
					modImport.append(';').append(ICoreConstants.OPTIONAL_ATTRIBUTE).append("=true");//$NON-NLS-1$
			}
			bundleRequire.append(modImport.toString());
			if (iter.hasNext())
				bundleRequire.append(LIST_SEPARATOR);
		}
		generatedManifest.put(Constants.REQUIRE_BUNDLE, bundleRequire.toString());
	}

	private void generateTimestamp() {
		// so it is easy to tell which ones are generated
		generatedManifest.put(GENERATED_FROM, Long.toString(getTimeStamp(pluginManifestLocation, manifestType)) + ";" + MANIFEST_TYPE_ATTRIBUTE + "=" + manifestType); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void generateEclipseHeaders() {
		if (pluginInfo.isFragment())
			return;

		String pluginClass = pluginInfo.getPluginClass();
		if (pluginInfo.hasExtensionExtensionPoints() || (pluginClass != null && !pluginClass.trim().equals(""))) { //$NON-NLS-1$
			if (TARGET34.compareTo(target) <= 0)
				generatedManifest.put(Constants.BUNDLE_ACTIVATIONPOLICY, Constants.ACTIVATION_LAZY);
			else
				generatedManifest.put(TARGET32.compareTo(target) <= 0 ? ICoreConstants.ECLIPSE_LAZYSTART : ICoreConstants.ECLIPSE_AUTOSTART, "true"); //$NON-NLS-1$
		}
	}

	private Set getExports() {
		Map libs = pluginInfo.getLibraries();
		if (libs == null)
			return null;

		String projName = pluginManifestLocation.getName();
		IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
		if (proj == null)
			return null;

		return getExports(proj, libs);
	}

	public Set getExports(IProject proj, Map libs) {
		IFile buildProperties = PDEProject.getBuildProperties(proj);
		IBuild build = null;
		if (buildProperties != null) {
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildProperties);
			build = buildModel.getBuild();
		} else
			build = new Build();
		return findPackages(proj, libs, build);
	}

	private Set findPackages(IProject proj, Map libs, IBuild build) {
		TreeSet result = new TreeSet();
		IJavaProject jp = JavaCore.create(proj);
		Iterator it = libs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String libName = entry.getKey().toString();
			List filter = (List) entry.getValue();
			IBuildEntry libEntry = build.getEntry(SOURCE_PREFIX + libName);
			if (libEntry != null) {
				String[] tokens = libEntry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IResource folder = null;
					if (tokens[i].equals(".")) //$NON-NLS-1$
						folder = proj;
					else
						folder = proj.getFolder(tokens[i]);
					if (folder != null)
						addPackagesFromFragRoot(jp.getPackageFragmentRoot(folder), result, filter);
				}
			} else {
				IResource res = proj.findMember(libName);
				if (res != null)
					addPackagesFromFragRoot(jp.getPackageFragmentRoot(res), result, filter);
			}
		}
		return result;
	}

	private void addPackagesFromFragRoot(IPackageFragmentRoot root, Collection result, List filter) {
		if (root == null)
			return;
		try {
			if (filter != null && !filter.contains("*")) { //$NON-NLS-1$
				ListIterator li = filter.listIterator();
				while (li.hasNext()) {
					String pkgName = li.next().toString();
					if (pkgName.endsWith(".*")) //$NON-NLS-1$
						pkgName = pkgName.substring(0, pkgName.length() - 2);

					IPackageFragment frag = root.getPackageFragment(pkgName);
					if (frag != null)
						result.add(pkgName);
				}
				return;
			}
			IJavaElement[] children = root.getChildren();
			for (int j = 0; j < children.length; j++) {
				IPackageFragment fragment = (IPackageFragment) children[j];
				String name = fragment.getElementName();
				if (fragment.hasChildren() && !result.contains(name)) {
					result.add(name);
				}
			}
		} catch (JavaModelException e) {
		}
	}

	/**
	 * Parses the plugin manifest to find out: - the plug-in unique identifier -
	 * the plug-in version - runtime/libraries entries - the plug-in class -
	 * the master plugin (for a fragment)
	 */
	private PluginInfo parsePluginInfo(URL pluginLocation) throws PluginConversionException {
		InputStream input = null;
		try {
			input = new BufferedInputStream(pluginLocation.openStream());
			return new PluginConverterParser(context, target).parsePlugin(input);
		} catch (Exception e) {
			String message = NLS.bind(PDECoreMessages.PluginConverter_EclipseConverterErrorParsingPluginManifest, pluginManifestLocation);
			throw new PluginConversionException(message, e);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					//ignore exception
				}
		}
	}

	public static boolean upToDate(File generationLocation, File pluginLocation, byte manifestType) {
		if (!generationLocation.isFile())
			return false;
		String secondLine = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(generationLocation)));
			reader.readLine();
			secondLine = reader.readLine();
		} catch (IOException e) {
			// not a big deal - we could not read an existing manifest
			return false;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
		}
		String tag = GENERATED_FROM + ": "; //$NON-NLS-1$
		if (secondLine == null || !secondLine.startsWith(tag))
			return false;

		secondLine = secondLine.substring(tag.length());
		ManifestElement generatedFrom;
		try {
			generatedFrom = ManifestElement.parseHeader(PluginConverter.GENERATED_FROM, secondLine)[0];
		} catch (BundleException be) {
			return false;
		}
		String timestampStr = generatedFrom.getValue();
		try {
			return Long.parseLong(timestampStr.trim()) == getTimeStamp(pluginLocation, manifestType);
		} catch (NumberFormatException nfe) {
			// not a big deal - just a bogus existing manifest that will be ignored
		}
		return false;
	}

	public static long getTimeStamp(File pluginLocation, byte manifestType) {
		if ((manifestType & MANIFEST_TYPE_JAR) != 0)
			return pluginLocation.lastModified();
		else if ((manifestType & MANIFEST_TYPE_PLUGIN) != 0)
			return new File(pluginLocation, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR).lastModified();
		else if ((manifestType & MANIFEST_TYPE_FRAGMENT) != 0)
			return new File(pluginLocation, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR).lastModified();
		else if ((manifestType & MANIFEST_TYPE_BUNDLE) != 0)
			return new File(pluginLocation, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR).lastModified();
		return -1;
	}

	private void writeEntry(Writer out, String key, String value) throws IOException {
		if (value != null && value.length() > 0) {
			out.write(splitOnComma(key + ": " + value)); //$NON-NLS-1$
			out.write('\n');
		}
	}

	private String splitOnComma(String value) {
		if (value.length() < MAXLINE || value.indexOf(LINE_SEPARATOR) >= 0)
			return value; // assume the line is already split
		String[] values = ManifestElement.getArrayFromList(value);
		if (values == null || values.length == 0)
			return value;
		StringBuffer sb = new StringBuffer(value.length() + ((values.length - 1) * LIST_SEPARATOR.length()));
		for (int i = 0; i < values.length - 1; i++)
			sb.append(values[i]).append(LIST_SEPARATOR);
		sb.append(values[values.length - 1]);
		return sb.toString();
	}

	private String getStringFromArray(String[] values, String separator) {
		if (values == null)
			return ""; //$NON-NLS-1$
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			if (i > 0)
				result.append(separator);
			result.append(values[i]);
		}
		return result.toString();
	}

	private String getStringFromCollection(Collection collection, String separator) {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (Iterator i = collection.iterator(); i.hasNext();) {
			if (first)
				first = false;
			else
				result.append(separator);
			result.append(i.next());
		}
		return result.toString();
	}

	public synchronized Dictionary convertManifest(File pluginBaseLocation, boolean compatibility, String target, boolean analyseJars, Dictionary devProperties) throws PluginConversionException {
		long start = System.currentTimeMillis();
		if (DEBUG)
			System.out.println("Convert " + pluginBaseLocation); //$NON-NLS-1$
		init();
		this.target = target == null ? TARGET32 : new Version(target);
		fillPluginInfo(pluginBaseLocation);
		fillManifest(compatibility, analyseJars);
		if (DEBUG)
			System.out.println("Time to convert manifest for: " + pluginBaseLocation + ": " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return generatedManifest;
	}

	public synchronized File convertManifest(File pluginBaseLocation, File bundleManifestLocation, boolean compatibilityManifest, String target, boolean analyseJars, Dictionary devProperties) throws PluginConversionException {
		if (bundleManifestLocation == null)
			throw new PluginConversionException(PDECoreMessages.PluginConverter_BundleLocationIsNull);
		convertManifest(pluginBaseLocation, compatibilityManifest, target, analyseJars, devProperties);
		if (upToDate(bundleManifestLocation, pluginManifestLocation, manifestType))
			return bundleManifestLocation;
		writeManifest(bundleManifestLocation, (Map) generatedManifest, compatibilityManifest);
		return bundleManifestLocation;
	}

	private String getVersionRange(String reqVersion, String matchRule) {
		if (reqVersion == null)
			return null;

		Version minVersion = Version.parseVersion(reqVersion);
		String versionRange;
		if (matchRule != null) {
			if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_PERFECT)) {
				versionRange = new VersionRange(minVersion, true, minVersion, true).toString();
			} else if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_EQUIVALENT)) {
				versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor(), minVersion.getMinor() + 1, 0, ""), false).toString(); //$NON-NLS-1$
			} else if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_COMPATIBLE)) {
				versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor() + 1, 0, 0, ""), false).toString(); //$NON-NLS-1$
			} else if (matchRule.equalsIgnoreCase(IModel.PLUGIN_REQUIRES_MATCH_GREATER_OR_EQUAL)) {
				// just return the reqVersion here without any version range
				versionRange = reqVersion;
			} else {
				versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor() + 1, 0, 0, ""), false).toString(); //$NON-NLS-1$
			}
		} else {
			versionRange = new VersionRange(minVersion, true, new Version(minVersion.getMajor() + 1, 0, 0, ""), false).toString(); //$NON-NLS-1$
		}

		StringBuffer result = new StringBuffer();
		result.append(';').append(Constants.BUNDLE_VERSION_ATTRIBUTE).append('=');
		result.append('\"').append(versionRange).append('\"');
		return result.toString();
	}
}
