/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * Service used to create and configure bundle project descriptions.
 * <p>
 * An instance of this service can be obtained from a {@link BundleContext} as
 * follows
 * <pre>
 * BundleContext context = ...
 * ServiceReference ref = context.getServiceReference(IBundleContextService.class.getName());
 * IBundleContextService service = (IBundleContextService)context.getService(ref);
 * ...
 * context.ungetService(ref); 
 * </pre>
 * </p>
 * @since 3.6
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBundleProjectService {

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
	public IBundleProjectDescription getDescription(IProject project) throws CoreException;

	/**
	 * Creates and returns a new host description.
	 * 
	 * @param name symbolic name of the host
	 * @param range version constraint or <code>null</code>
	 * @return host description
	 */
	public IHostDescription newHost(String name, VersionRange range);

	/**
	 * Creates and returns a new package import description.
	 * 
	 * @param name fully qualified name of imported package
	 * @param range version constraint or <code>null</code>
	 * @param optional whether the import is optional
	 * @return package import description
	 */
	public IPackageImportDescription newPackageImport(String name, VersionRange range, boolean optional);

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
	public IPackageExportDescription newPackageExport(String name, Version version, boolean api, String[] friends);

	/**
	 * Creates and returns a new required bundle description.
	 * 
	 * @param name symbolic name of required bundle
	 * @param range version constraint or <code>null</code>
	 * @param optional whether the required bundle is optional
	 * @param export whether the required bundle is re-exported
	 * @return required bundle description
	 */
	public IRequiredBundleDescription newRequiredBundle(String name, VersionRange range, boolean optional, boolean export);

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
	 * @return a new bundle classpath entry
	 */
	public IBundleClasspathEntry newBundleClasspathEntry(IPath sourceFolder, IPath binaryFolder, IPath library);

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
	public void setBundleRoot(IProject project, IPath bundleRoot) throws CoreException;

}
