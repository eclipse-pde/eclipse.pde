/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.internal.core.project.*;
import org.osgi.framework.Version;

/**
 * Factory class for creating bundle project descriptions and associated artifacts.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 3.6
 */
public final class Factory {

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
	public static IBundleProjectDescription getDescription(IProject project) throws CoreException {
		return new BundleProjectDescription(project);
	}

	/**
	 * Creates and returns a new host description.
	 * 
	 * @param name symbolic name of the host
	 * @param range version constraint or <code>null</code>
	 * @return host description
	 */
	public static IHostDescription newHost(String name, VersionRange range) {
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
	public static IPackageImportDescription newPackageImport(String name, VersionRange range, boolean optional) {
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
	public static IPackageExportDescription newPackageExport(String name, Version version, boolean api, String[] friends) {
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
	public static IRequiredBundleDescription newRequiredBundle(String name, VersionRange range, boolean optional, boolean export) {
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
	public static IBundleClasspathEntry newBundleClasspathEntry(IPath sourceFolder, IPath binaryFolder, IPath library) {
		return new BundleClasspathSpecification(sourceFolder, binaryFolder, library);
	}
}
