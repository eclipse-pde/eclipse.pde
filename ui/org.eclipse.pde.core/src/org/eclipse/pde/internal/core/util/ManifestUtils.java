/*******************************************************************************
 * Copyright (c) 2006, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Hannes Wellmann - Add utility method to derive the BREE of a resource from its EE requirements
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import static org.osgi.framework.namespace.ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
import static org.osgi.framework.namespace.ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.TargetWeaver;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

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
	 * Status code given to the returned core exception when a manifest file is found,
	 * but not have the contain the required Bundle-SymbolicName header.
	 */
	public static final int STATUS_CODE_NOT_A_BUNDLE_MANIFEST = 204;

	/**
	 * Utility method to parse a bundle's manifest into a dictionary. The bundle
	 * may be in a directory or an archive at the specified location. If the
	 * manifest does not contain the necessary entries, the plugin.xml and
	 * fragment.xml will be checked for an old style plug-in.
	 * <p>
	 * If this method is being called from a dev mode workspace, the returned
	 * map should be passed to {@link TargetWeaver#weaveManifest(Map, File)} so
	 * that the bundle classpath can be corrected.
	 * </p>
	 * <p>
	 * This method is called by
	 * org.eclipse.pde.api.tools.internal.model.BundleComponent.getManifest()
	 * when OSGi is not running to load manifest information for a bundle.
	 * </p>
	 * <p>
	 * TODO This method may be removed in favour of one that caches manifest
	 * contents. Currently caching is not worthwhile as calling
	 * <code>ManifestElement.parseManifest()</code> takes trivial time (under
	 * 1ms) on repeat calls to the same file.
	 * </p>
	 *
	 * @param bundleLocation
	 *            root location of the bundle, may be a archive file or
	 *            directory
	 * @return map of bundle manifest properties
	 * @throws CoreException
	 *             if manifest has invalid syntax, is missing or there is a
	 *             problem converting as old style plug-in
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
					}
				} finally {
					closeZipFileAndStream(stream, jarFile);
				}
			} else {
				// Check the manifest.MF
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists()) {
					try (InputStream stream = new FileInputStream(file);) {
						Map<String, String> map = ManifestElement.parseBundleManifest(stream, new HashMap<>(10));
						if (map != null && map.containsKey(Constants.BUNDLE_SYMBOLICNAME)) {
							return map;
						}
					}
				}else {
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, STATUS_CODE_NOT_A_BUNDLE_MANIFEST, NLS.bind(UtilMessages.ErrorManifestFileAbsent, bundleLocation.getAbsolutePath()), null));
				}
			}

			// The necessary bundle information has not been found in manifest.mf or plugin.xml
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, STATUS_CODE_NOT_A_BUNDLE_MANIFEST, NLS.bind(UtilMessages.ErrorReadingManifest, bundleLocation.getAbsolutePath()), null));

		} catch (BundleException | IOException e) {
			throw new CoreException(Status.error(  NLS.bind(UtilMessages.ErrorReadingManifest, bundleLocation.getAbsolutePath()), e));
		}
	}

	public static IPackageFragmentRoot[] findPackageFragmentRoots(IManifestHeader header, IProject project) {
		IJavaProject javaProject = JavaCore.create(project);

		String[] libs;
		if (header == null || header.getValue() == null) {
			libs = new String[] {"."}; //$NON-NLS-1$
		} else {
			libs = header.getValue().split(","); //$NON-NLS-1$
		}

		IBuild build = getBuild(project);
		if (build == null) {
			try {
				return javaProject.getPackageFragmentRoots();
			} catch (JavaModelException e) {
				return new IPackageFragmentRoot[0];
			}
		}
		List<IPackageFragmentRoot> pkgFragRoots = new LinkedList<>();
		for (String lib : libs) {
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
				if (entry == null) {
					continue;
				}
				String[] tokens = entry.getTokens();
				for (String token : tokens) {
					IResource resource = project.findMember(token);
					if (resource == null) {
						continue;
					}
					root = javaProject.getPackageFragmentRoot(resource);
					if (root != null && root.exists()) {
						pkgFragRoots.add(root);
					}
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
		for (Entry<String, String> entry : manifestToWrite.entrySet()) {
			writeEntry(out, entry.getKey(), entry.getValue());
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
	 * Closes the stream and file
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
		if (value.length() < MANIFEST_MAXLINE || value.contains(MANIFEST_LINE_SEPARATOR)) {
			return value; // assume the line is already split
		}
		String[] values = ManifestElement.getArrayFromList(value);
		if (values == null || values.length == 0) {
			return value;
		}
		StringBuilder sb = new StringBuilder(value.length() + ((values.length - 1) * MANIFEST_LIST_SEPARATOR.length()));
		for (int i = 0; i < values.length - 1; i++) {
			sb.append(values[i]).append(MANIFEST_LIST_SEPARATOR);
		}
		sb.append(values[values.length - 1]);
		return sb.toString();
	}

	/**
	 * Returns the list all execution-environments required by the given OSGi
	 * resource. Only registered EEs are considered.
	 *
	 * @param resource
	 *            the osgi resource
	 * @return a list containing the id's of all required EEs
	 * @see ExecutionEnvironmentNamespace#EXECUTION_ENVIRONMENT_NAMESPACE
	 * @see IExecutionEnvironmentsManager#getExecutionEnvironments()
	 */
	public static Stream<String> getRequiredExecutionEnvironments(Resource resource) {
		List<Requirement> requirements = resource.getRequirements(EXECUTION_ENVIRONMENT_NAMESPACE);
		return requirements.stream()
				.map(requirement -> requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE))
				.mapMulti(ManifestUtils::parseRequiredEEsFromFilter);
	}

	// provide fast-path for simple filters like: (&(osgi.ee=JavaSE)(version=17)
	private static final Map<String, String> SIMPLE_EE_FILTERS = new HashMap<>();
	private static final Map<String, Map<String, String>> AVAILABLE_EE_ATTRIBUTES = new HashMap<>();

	static {
		// Manually add ancient EE id that are difficult to parse
		AVAILABLE_EE_ATTRIBUTES.put("CDC-1.0/Foundation-1.0", //$NON-NLS-1$
				Map.of(EXECUTION_ENVIRONMENT_NAMESPACE, "CDC/Foundation", CAPABILITY_VERSION_ATTRIBUTE, "1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		AVAILABLE_EE_ATTRIBUTES.put("CDC-1.1/Foundation-1.1", //$NON-NLS-1$
				Map.of(EXECUTION_ENVIRONMENT_NAMESPACE, "CDC/Foundation", CAPABILITY_VERSION_ATTRIBUTE, "1.1")); //$NON-NLS-1$ //$NON-NLS-2$

		for (String eeId : TargetPlatformHelper.getKnownExecutionEnvironments()) {
			String eeName;
			String eeVersion;
			// Extract the osgi.ee name and version attribute from the EE id
			Map<String, String> predefinedAttributes = AVAILABLE_EE_ATTRIBUTES.get(eeId);
			if (predefinedAttributes == null) {
				if (eeId.indexOf('/') >= eeId.indexOf('-')) {
					ILog.get().error("Cannot reliably parse filter attributes from BREE with id: " + eeId); //$NON-NLS-1$
					continue;
				}
				int versionSeparator = eeId.lastIndexOf('-');
				if (versionSeparator < 0) {
					throw new IllegalArgumentException("Missing version-separator in EE Id"); //$NON-NLS-1$
				}
				eeName = eeId.substring(0, versionSeparator);
				// OSGi spec chapter 3.4.1 Bundle-RequiredExecutionEnvironment
				if ("J2SE".equals(eeName)) { //$NON-NLS-1$
					eeName = "JavaSE"; //$NON-NLS-1$
				}
				eeVersion = eeId.substring(versionSeparator + 1);

				AVAILABLE_EE_ATTRIBUTES.put(eeId,
						Map.of(EXECUTION_ENVIRONMENT_NAMESPACE, eeName, CAPABILITY_VERSION_ATTRIBUTE, eeVersion));
			} else {
				eeName = predefinedAttributes.get(EXECUTION_ENVIRONMENT_NAMESPACE);
				eeVersion = predefinedAttributes.get(CAPABILITY_VERSION_ATTRIBUTE);
			}
			String eeNamespace = EXECUTION_ENVIRONMENT_NAMESPACE;
			String versionAttribute = CAPABILITY_VERSION_ATTRIBUTE;
			String filter1 = "(&(" + eeNamespace + "=" + eeName + ")(" + versionAttribute + "=" + eeVersion + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			String filter2 = "(&(" + versionAttribute + "=" + eeVersion + ")(" + eeNamespace + "=" + eeName + "))"; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			SIMPLE_EE_FILTERS.put(filter1, eeId); // add both variants
			SIMPLE_EE_FILTERS.put(filter2, eeId);
		}
	}

	private static final Pattern WHITESPACE = Pattern.compile("\\s*"); //$NON-NLS-1$

	public static void parseRequiredEEsFromFilter(String eeFilter, Consumer<String> collector) {
		if (eeFilter == null) {
			return;
		}
		if (eeFilter.chars().anyMatch(Character::isWhitespace)) {
			eeFilter = WHITESPACE.matcher(eeFilter).replaceAll(""); //$NON-NLS-1$
		}
		String ee = SIMPLE_EE_FILTERS.get(eeFilter);
		if (ee != null) {
			collector.accept(ee);
		} else {
			try { // complex filter. Collect all matching EEs
				Filter filter = FrameworkUtil.createFilter(eeFilter);
				AVAILABLE_EE_ATTRIBUTES.forEach((eeId, eeAttributes) -> {
					if (filter.matches(eeAttributes)) {
						collector.accept(eeId);
					}
				});
			} catch (InvalidSyntaxException e) { // should not happen
				throw new IllegalArgumentException("Invalid execution environment filter", e); //$NON-NLS-1$
			}
		}
	}

	public static Optional<VersionRange> createConsumerRequirementRange(Version version) {
		if (version != null && !Version.emptyVersion.equals(version)) {
			return Optional.ofNullable(new VersionRange(VersionRange.LEFT_CLOSED, //
					new Version(version.getMajor(), version.getMinor(), 0), //
					new Version(version.getMajor() + 1, 0, 0), //
					VersionRange.RIGHT_OPEN));
		}
		return Optional.empty();
	}

	/**
	 * Return the value of "Eclipse-SourceReferences" in MANIFEST.MF from the
	 * given bundle.
	 *
	 * @param bundle
	 *            The bundle to get the source repository
	 */
	public static String getSourceReferences(Bundle bundle) {
		String srcRef = findScmGit(bundle);
		if (srcRef != null) {
			try {
				var element = ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_REFERENCES, srcRef)[0];
				srcRef = element.getValue().trim().replaceFirst("^scm:git:", ""); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (BundleException e) {
			}
		}
		return srcRef;
	}

	private static String findScmGit(Bundle bundle) {
		if (bundle != null) {
			try (InputStream is = bundle.getEntry(ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR).openStream()) {
				Attributes attributes = new Manifest(is).getMainAttributes();
				return attributes.getValue(ICoreConstants.ECLIPSE_SOURCE_REFERENCES);
			} catch (IOException e) {
				throw new IllegalArgumentException(
						ICoreConstants.ECLIPSE_SOURCE_REFERENCES + " not found for bundle : " + bundle); //$NON-NLS-1$
			}
		}
		return null;
	}

}
