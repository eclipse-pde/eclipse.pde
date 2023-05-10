/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
 *     Kaloyan Raev - kaloyan.raev@sap.com - Bug 323246: IBundleProjectService.setBundleRoot() does not accept null argument
 *******************************************************************************/
package org.eclipse.pde.internal.core.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IHostDescription;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.core.project.IPackageImportDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.Messages;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * Factory class for creating bundle project descriptions and associated artifacts.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 3.6
 */
public final class BundleProjectService implements IBundleProjectService {

	/**
	 * Property key used in {@link ScmUrlImportDescription}s.
	 */
	public static final String BUNDLE_IMPORTER = "BUNDLE_IMPORTER"; //$NON-NLS-1$

	/**
	 * Property key used in {@link ScmUrlImportDescription}s.
	 */
	public static final String PLUGIN = "PLUGIN"; //$NON-NLS-1$

	private static IBundleProjectService fgDefault;

	/**
	 * Returns the bundle project service.
	 *
	 * @return bundle project service
	 */
	public static synchronized IBundleProjectService getDefault() {
		if (fgDefault == null) {
			fgDefault = new BundleProjectService();
		}
		return fgDefault;
	}

	/**
	 * Constructs a new service.
	 */
	private BundleProjectService() {
	}

	/**
	 * Returns a bundle description for the given project.
	 * If the project does not exist, the description can be used to create
	 * a new bundle project. If the project does exist, the description can be used to
	 * modify a project.
	 *
	 * @param project project
	 * @return bundle description for the associated project
	 * @exception CoreException if unable to create a description on an existing project
	 */
	@Override
	public IBundleProjectDescription getDescription(IProject project) throws CoreException {
		return new BundleProjectDescription(project);
	}

	/**
	 * Creates and returns a new host description.
	 *
	 * @param name symbolic name of the host
	 * @param range version constraint or <code>null</code>
	 * @return host description
	 */
	@Override
	public IHostDescription newHost(String name, VersionRange range) {
		return new HostDescriptoin(name, range);
	}

	/**
	 * Creates and returns a new package import description.
	 *
	 * @param name fully qualified name of imported package
	 * @param range version constraint or <code>null</code>
	 * @param optional whether the import is optional
	 * @return package import description
	 */
	@Override
	public IPackageImportDescription newPackageImport(String name, VersionRange range, boolean optional) {
		return new PackageImportDescription(name, range, optional);
	}

	/**
	 * Constructs a new package export description.
	 *
	 * @param name fully qualified package name
	 * @param version version or <code>null</code>
	 * @param api whether the package is considered API
	 * @param friends symbolic names of bundles that are friends, or <code>null</code>; when
	 *  friends are specified the package will not be API
	 * @return package export description
	 */
	@Override
	public IPackageExportDescription newPackageExport(String name, Version version, boolean api, String[] friends) {
		return new PackageExportDescription(name, version, friends, api);
	}

	/**
	 * Creates and returns a new required bundle description.
	 *
	 * @param name symbolic name of required bundle
	 * @param range version constraint or <code>null</code>
	 * @param optional whether the required bundle is optional
	 * @param export whether the required bundle is re-exported
	 * @return required bundle description
	 */
	@Override
	public IRequiredBundleDescription newRequiredBundle(String name, VersionRange range, boolean optional, boolean export) {
		return new RequiredBundleDescription(name, range, export, optional);
	}

	/**
	 * Creates and returns a new bundle classpath entry defining the relationship
	 * between a source, binaries, and library on the Bundle-Classpath header.
	 * <p>
	 * When a source folder is specified, the binary folder defines its output
	 * folder, or may be <code>null</code> to indicate that the project's default output
	 * folder is used by the source folder. When only a binary folder is specified, there
	 * is no source associated with the folder. When no source or binary are specified,
	 * it indicates the library is included in the project as an archive.
	 * </p>
	 * @param sourceFolder source folder or <code>null</code>
	 * @param binaryFolder binary folder or <code>null</code>
	 * @param library associated entry on the Bundle-Classpath header or <code>null</code>
	 * 	to indicate default entry "."
	 */
	@Override
	public IBundleClasspathEntry newBundleClasspathEntry(IPath sourceFolder, IPath binaryFolder, IPath library) {
		return new BundleClasspathSpecification(sourceFolder, binaryFolder, library);
	}

	/**
	 * Sets the location within the project where the root of the bundle and its associated
	 * artifacts will reside, or <code>null</code> to indicate the default bundle root location
	 * should be used (project folder).
	 * <p>
	 * The bundle root is the folder containing the <code>META-INF/</code> folder. When the bundle
	 * root location is modified, existing bundle artifacts at the old root are not moved or modified.
	 * When creating a new bundle project {@link IBundleProjectDescription#setBundleRoot(IPath)} can
	 * be used to specify an initial bundle root location. To modify the bundle root location of an
	 * existing project, this method must be used.
	 * </p>
	 * @param project project that must exist and be open
	 * @param bundleRoot project relative path to bundle root artifacts in the project or <code>null</code>
	 * @throws CoreException if setting the root fails
	 */
	@Override
	public void setBundleRoot(IProject project, IPath bundleRoot) throws CoreException {
		PDEProject.setBundleRoot(project, (bundleRoot == null) ? null : project.getFolder(bundleRoot));
	}

	public IBundleImporter getSourceReferenceHandler(String id) {

		return null;
	}

	/**
	 * Creates and returns a map of bundle import descriptions for the given bundles.
	 * The map is of {@link IBundleImporter} -> arrays of {@link ScmUrlImportDescription}.
	 * Adds 'BUNDLE_IMPORTER' property to each description that maps to the importer that
	 * created each description.
	 * Adds 'PLUGIN' property that maps to the original plug-in model.
	 *
	 * @param models plug-in models
	 * @return import instructions
	 * @exception CoreException if unable to read manifest
	 */
	public Map<IBundleImporter, ScmUrlImportDescription[]> getImportDescriptions(IPluginModelBase[] models) throws CoreException {
		// build manifests
		List<Map<String, String>> manifests = new ArrayList<>();
		List<IPluginModelBase> plugins = new ArrayList<>();
		for (IPluginModelBase model : models) {
			String location = model.getInstallLocation();
			if (location != null) {
				Map<String, String> manifest = loadManifest(new File(location));
				if (manifest != null) {
					manifests.add(manifest);
					plugins.add(model);
				}
			}
		}
		if (!manifests.isEmpty()) {
			@SuppressWarnings("rawtypes")
			Map[] marray = manifests.toArray(new Map[manifests.size()]);
			Map<IBundleImporter, ScmUrlImportDescription[]> result = new HashMap<>();
			IBundleImporter[] importers = Team.getBundleImporters();
			for (IBundleImporter importer : importers) {
				ScmUrlImportDescription[] descriptions = importer.validateImport(marray);
				List<ScmUrlImportDescription> descriptioonList = new ArrayList<>();
				for (int j = 0; j < descriptions.length; j++) {
					ScmUrlImportDescription description = descriptions[j];
					if (description != null) {
						descriptioonList.add(description);
						description.setProperty(BUNDLE_IMPORTER, importer);
						description.setProperty(PLUGIN, plugins.get(j));
					}
				}
				if (!descriptioonList.isEmpty()) {
					result.put(importer, descriptioonList.toArray(new ScmUrlImportDescription[descriptioonList.size()]));
				}
			}
			return result;
		}
		return Collections.emptyMap();
	}

	/**
	 * Parses a bunlde's manifest into a dictionary and returns the map
	 * or <code>null</code> if none. The bundle may be in a jar
	 * or in a directory at the specified location.
	 *
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary or <code>null</code>
	 * @throws CoreException if manifest has invalid syntax or is missing
	 */
	private Map<String, String> loadManifest(File bundleLocation) throws CoreException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		String extension = new Path(bundleLocation.getName()).getFileExtension();
		try {
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists()) {
					manifestStream = new FileInputStream(file);
				}
			}
			if (manifestStream == null) {
				return null;
			}
			return ManifestElement.parseBundleManifest(manifestStream, new Hashtable<>(10));
		} catch (BundleException | IOException e) {
			throw new CoreException(Status.error(NLS.bind(Messages.TargetBundle_ErrorReadingManifest, bundleLocation.getAbsolutePath()), e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
	}

	private void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
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
}
