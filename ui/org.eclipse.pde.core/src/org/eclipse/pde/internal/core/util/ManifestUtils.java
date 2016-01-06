/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ManifestUtils {

	/**
	 * Manifest header for the syntax version of the jar manifest. Not part of
	 * the OSGi specification. Must be the first header in the manifest. Typically
	 * set to '1.0'.
	 */
	public static final String MANIFEST_VERSION = "Manifest-Version"; //$NON-NLS-1$
	public static final String MANIFEST_LIST_SEPARATOR = ",\n "; //$NON-NLS-1$
	public static final String MANIFEST_LINE_SEPARATOR = "\n "; //$NON-NLS-1$
	private static int MANIFEST_MAXLINE = 511;

	/**
	 * Status code given to the returned core exception when an old style plug-in manifest
	 * cannot be converted because the {@link PluginConverter} service is not available.
	 */
	public static final int STATUS_CODE_PLUGIN_CONVERTER_UNAVAILABLE = 201;

	/**
	 * Status code given to the returned core exception when a manifest file is found,
	 * but not have the contain the required Bundle-SymbolicName header.
	 */
	public static final int STATUS_CODE_NOT_A_BUNDLE_MANIFEST = 204;

	/**
	 * Utility method to parse a bundle's manifest into a dictionary. The bundle may be in
	 * a directory or an archive at the specified location.  If the manifest does not contain
	 * the necessary entries, the plugin.xml and fragment.xml will be checked for an old style
	 * plug-in.  If the plugin.xml cannot be converted because the {@link PluginConverter}
	 * service is not available, the thrown core exception will have a status code of
	 * {@link #STATUS_CODE_PLUGIN_CONVERTER_UNAVAILABLE} to allow special processing.
	 * <p>
	 * If this method is being called from a dev mode workspace, the returned map should be passed to
	 * {@link TargetWeaver#weaveManifest(Map)} so that the bundle classpath can be corrected.
	 * </p><p>
	 * This method is called by org.eclipse.pde.api.tools.internal.model.BundleComponent.getManifest()
	 * when OSGi is not running to load manifest information for a bundle.
	 * </p><p>
	 * TODO This method may be removed in favour of one that caches manifest contents. Currently caching is not
	 * worthwhile as calling <code>ManifestElement.parseManifest()</code> takes trivial time (under 1ms) on repeat
	 * calls to the same file.
	 * </p>
	 *
	 * @param bundleLocation root location of the bundle, may be a archive file or directory
	 * @return map of bundle manifest properties
	 * @throws CoreException if manifest has invalid syntax, is missing or there is a problem converting as old style plug-in
	 */
	public static Map<String, String> loadManifest(File bundleLocation) throws CoreException {
		// Check if the file is a archive or a directory
		try {
			if (bundleLocation.isFile()) {
				ZipFile jarFile = null;
				InputStream stream = null;
				try {
					jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
					// Check the manifest.MF
					ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
					if (manifestEntry != null) {
						stream = jarFile.getInputStream(manifestEntry);
						if (stream != null) {
							Map<String, String> map = ManifestElement.parseBundleManifest(stream, null);
							// Symbolic name is the only required manifest entry, this is an ok bundle
							if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
								return map;
							}
						}
						// Manifest.MF wasn't good, check for plugin.xml or fragment.xml
						ZipEntry pluginEntry = jarFile.getEntry(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
						if (pluginEntry == null) {
							pluginEntry = jarFile.getEntry(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
						}
						if (pluginEntry != null) {
							Map<String, String> map = loadPluginXML(bundleLocation);
							if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
								return map;
							}
						}
					}
				} finally {
					closeZipFileAndStream(stream, jarFile);
				}
			} else {
				// Check the manifest.MF
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists()) {
					InputStream stream = null;
					try {
						stream = new FileInputStream(file);
						Map<String, String> map = ManifestElement.parseBundleManifest(stream, new HashMap<String, String>(10));
						if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
							return map;
						}
					} finally {
						if (stream != null) {
							stream.close();
						}
					}
				}
				// Manifest.MF wasn't good, check for plugin.xml or fragment.xml
				File pxml = new File(bundleLocation, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
				File fxml = new File(bundleLocation, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
				if (pxml.exists() || fxml.exists()) {
					Map<String, String> map = loadPluginXML(bundleLocation);
					if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
						return map;
					}
				}
			}

			// The necessary bundle information has not been found in manifest.mf or plugin.xml
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, STATUS_CODE_NOT_A_BUNDLE_MANIFEST, NLS.bind(UtilMessages.ErrorReadingManifest, bundleLocation.getAbsolutePath()), null));

		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(UtilMessages.ErrorReadingManifest, bundleLocation.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(UtilMessages.ErrorReadingManifest, bundleLocation.getAbsolutePath()), e));
		}
	}

	/**
	 * Uses the OSGi PluginConverter to generate a manifest for the given project. Will
	 * attempt to refresh the project when complete. If the plugin.xml cannot be
	 * converted because the {@link PluginConverter} service is not available, the
	 * thrown core exception will have a status code of
	 * {@link #STATUS_CODE_PLUGIN_CONVERTER_UNAVAILABLE} to allow special processing.
	 *
	 * @param project the project to convert
	 * @param targetVersion the runtime version the converted manifest is targeted for
	 * @param devProperties a dictionary of development time classpath properties.
	 * 			The dictionary contains a mapping from plugin id to development time
	 * 			classpath. A value of null indicates that the default development time
	 * 			classpath properties will be used.
	 * @throws CoreException if there is a problem converting the manifest or the compatibility fragment hosting the converter service is not available
	 */
	public static void convertToOSGIFormat(IProject project, String targetVersion, Dictionary<String, String> devProperties) throws CoreException {
		File outputFile = new File(PDEProject.getManifest(project).getLocation().toOSString());
		File inputFile = new File(project.getLocation().toOSString());
		PluginConverter converter = (PluginConverter) PDECore.getDefault().acquireService(PluginConverter.class.getName());
		if (converter == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, STATUS_CODE_PLUGIN_CONVERTER_UNAVAILABLE, NLS.bind(UtilMessages.ManifestUtils_NeedCompatFragmentToConvertManifestFile, project.getLocation()), null));
		}
		try {
			converter.convertManifest(inputFile, outputFile, false, targetVersion, true, devProperties);
		} catch (PluginConversionException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(UtilMessages.ErrorReadingOldStyleManifest, inputFile.getAbsolutePath()), e));
		}
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// If there is a problem due to resource lock, skip the refresh
		}
	}

	public static IPackageFragmentRoot[] findPackageFragmentRoots(IManifestHeader header, IProject project) {
		IJavaProject javaProject = JavaCore.create(project);

		String[] libs;
		if (header == null || header.getValue() == null)
			libs = new String[] {"."}; //$NON-NLS-1$
		else
			libs = header.getValue().split(","); //$NON-NLS-1$

		IBuild build = getBuild(project);
		if (build == null) {
			try {
				return javaProject.getPackageFragmentRoots();
			} catch (JavaModelException e) {
				return new IPackageFragmentRoot[0];
			}
		}
		List<IPackageFragmentRoot> pkgFragRoots = new LinkedList<>();
		for (int j = 0; j < libs.length; j++) {
			String lib = libs[j];
			//https://bugs.eclipse.org/bugs/show_bug.cgi?id=230469
			IPackageFragmentRoot root = null;
			if (!lib.equals(".")) { //$NON-NLS-1$
				try {
					root = javaProject.getPackageFragmentRoot(project.getFile(lib));
				} catch (IllegalArgumentException e) {
					return new IPackageFragmentRoot[0];
				}
			}
			if (root != null && root.exists()) {
				pkgFragRoots.add(root);
			} else {
				IBuildEntry entry = build.getEntry("source." + lib); //$NON-NLS-1$
				if (entry == null)
					continue;
				String[] tokens = entry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IResource resource = project.findMember(tokens[i]);
					if (resource == null)
						continue;
					root = javaProject.getPackageFragmentRoot(resource);
					if (root != null && root.exists())
						pkgFragRoots.add(root);
				}
			}
		}
		return pkgFragRoots.toArray(new IPackageFragmentRoot[pkgFragRoots.size()]);
	}

	public static boolean isImmediateRoot(IPackageFragmentRoot root) throws JavaModelException {
		int kind = root.getKind();
		return kind == IPackageFragmentRoot.K_SOURCE || (kind == IPackageFragmentRoot.K_BINARY && !root.isExternal());
	}

	/**
	 * Writes out a manifest file to the given stream.  Orders the manifest in an expected
	 * order.  Will flush the output, but will not close the stream.
	 *
	 * @param manifestToWrite manifest headers to write to the stream
	 * @param out stream to write output to
	 * @throws IOException if there is a problem with the stream
	 */
	public static void writeManifest(Map<String, String> manifestToWrite, Writer out) throws IOException {
		// replaces any eventual existing file
		manifestToWrite = new Hashtable<>(manifestToWrite);

		// The manifest-version header is not used by OSGi but must be the first header according to the JDK Jar specification
		writeEntry(out, MANIFEST_VERSION, manifestToWrite.remove(MANIFEST_VERSION));
		// always attempt to write the Bundle-ManifestVersion header if it exists (bug 109863)
		writeEntry(out, Constants.BUNDLE_MANIFESTVERSION, manifestToWrite.remove(Constants.BUNDLE_MANIFESTVERSION));
		writeEntry(out, Constants.BUNDLE_NAME, manifestToWrite.remove(Constants.BUNDLE_NAME));
		writeEntry(out, Constants.BUNDLE_SYMBOLICNAME, manifestToWrite.remove(Constants.BUNDLE_SYMBOLICNAME));
		writeEntry(out, Constants.BUNDLE_VERSION, manifestToWrite.remove(Constants.BUNDLE_VERSION));
		writeEntry(out, Constants.BUNDLE_CLASSPATH, manifestToWrite.remove(Constants.BUNDLE_CLASSPATH));
		writeEntry(out, Constants.BUNDLE_ACTIVATOR, manifestToWrite.remove(Constants.BUNDLE_ACTIVATOR));
		writeEntry(out, Constants.BUNDLE_VENDOR, manifestToWrite.remove(Constants.BUNDLE_VENDOR));
		writeEntry(out, Constants.FRAGMENT_HOST, manifestToWrite.remove(Constants.FRAGMENT_HOST));
		writeEntry(out, Constants.BUNDLE_LOCALIZATION, manifestToWrite.remove(Constants.BUNDLE_LOCALIZATION));
		writeEntry(out, Constants.EXPORT_PACKAGE, manifestToWrite.remove(Constants.EXPORT_PACKAGE));
		writeEntry(out, ICoreConstants.PROVIDE_PACKAGE, manifestToWrite.remove(ICoreConstants.PROVIDE_PACKAGE));
		writeEntry(out, Constants.REQUIRE_BUNDLE, manifestToWrite.remove(Constants.REQUIRE_BUNDLE));
		Iterator<?> keys = manifestToWrite.keySet().iterator();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			writeEntry(out, key, manifestToWrite.get(key));
		}
		out.flush();
	}

	private static IBuild getBuild(IProject project) {
		IFile buildProps = PDEProject.getBuildProperties(project);
		if (buildProps.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildProps);
			return model.getBuild();
		}
		return null;
	}

	/**
	 * Parses an old style plug-in's (or fragment's) XML definition file into a dictionary.
	 * The provided file may be an zip archive or directory.  The existence of a plugin.xml
	 * or fragment.xml is not checked in this method. If the plugin.xml cannot be converted
	 * because the {@link PluginConverter} service is not available, the thrown core
	 * exception will have a status code of {@link #STATUS_CODE_PLUGIN_CONVERTER_UNAVAILABLE}
	 * to allow special processing.
	 *
	 * @param pluginLocation location of the bundle (archive file or directory)
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if manifest has invalid syntax
	 */
	private static Map<String, String> loadPluginXML(File pluginLocation) throws CoreException {
		// This may be called from API Tools without OSGi running, so we try to return a useful status
		if (PDECore.getDefault() == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, STATUS_CODE_PLUGIN_CONVERTER_UNAVAILABLE, UtilMessages.ManifestUtils_PluginConverterOnlyAvailableWithOSGi, null));
		}

		PluginConverter converter = (PluginConverter) PDECore.getDefault().acquireService(PluginConverter.class.getName());
		if (converter == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, STATUS_CODE_PLUGIN_CONVERTER_UNAVAILABLE, NLS.bind(UtilMessages.ManifestUtils_NeedCompatFragmentToConvertManifest, pluginLocation.toString()), null));
		}

		Dictionary<String, String> convert;
		try {
			convert = converter.convertManifest(pluginLocation, false, null, false, null);
		} catch (PluginConversionException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(UtilMessages.ErrorReadingOldStyleManifest, pluginLocation.getAbsolutePath()), e));
		}

		if (convert == null) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(UtilMessages.ErrorReadingOldStyleManifest, pluginLocation.getAbsolutePath())));
		}

		Map<String, String> map = new HashMap<>(convert.size(), 1.0f);
		Enumeration<String> keys = convert.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			map.put(key, convert.get(key));
		}
		return map;
	}

	/**
	 * Closes the stream and file
	 * @param stream
	 * @param jarFile
	 */
	private static void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
	}

	private static void writeEntry(Writer out, String key, String value) throws IOException {
		if (value != null && value.length() > 0) {
			out.write(splitOnComma(key + ": " + value)); //$NON-NLS-1$
			out.write('\n');
		}
	}

	private static String splitOnComma(String value) {
		if (value.length() < MANIFEST_MAXLINE || value.indexOf(MANIFEST_LINE_SEPARATOR) >= 0)
			return value; // assume the line is already split
		String[] values = ManifestElement.getArrayFromList(value);
		if (values == null || values.length == 0)
			return value;
		StringBuffer sb = new StringBuffer(value.length() + ((values.length - 1) * MANIFEST_LIST_SEPARATOR.length()));
		for (int i = 0; i < values.length - 1; i++)
			sb.append(values[i]).append(MANIFEST_LIST_SEPARATOR);
		sb.append(values[values.length - 1]);
		return sb.toString();
	}
}
