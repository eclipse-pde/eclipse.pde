/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.project.PDEProject;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.ManifestResource;
import aQute.bnd.osgi.Resource;

public class ProjectJar extends Jar {

	public static final QualifiedName GENERATED_PROPERTY = new QualifiedName(PDECore.PLUGIN_ID, "bndgenerated"); //$NON-NLS-1$

	private final IContainer outputFolder;
	private IFile manifestFile;
	private final Map<String, IFile> orphanFilesMap = new HashMap<>();

	public ProjectJar(IProject project, Predicate<IResource> filter) throws CoreException {
		super(project.getName());
		outputFolder = PDEProject.getJavaOutputFolder(project);
		Predicate<IResource> resourceScanner = r -> {
			if (r instanceof IFile f) {
				try {
					String path = r.getPersistentProperty(GENERATED_PROPERTY);
					if (path != null) {
						orphanFilesMap.put(path, f);
					}
				} catch (CoreException e) {
					// can't use that the ...
				}
			}
			return filter.test(r);
		};
		FileResource.addResources(this, outputFolder, resourceScanner);
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IJavaProject javaProject = JavaCore.create(project);
			IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
			IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
			for (IClasspathEntry cp : classpath) {
				if (cp.getEntryKind() == IClasspathEntry.CPE_SOURCE && !cp.isTest()) {
					IPath location = cp.getOutputLocation();
					if (location != null) {
						IFolder otherOutputFolder = workspaceRoot.getFolder(location);
						FileResource.addResources(this, otherOutputFolder, resourceScanner);
					}
				}
			}
		}
		manifestFile = PDEProject.getManifest(project);
	}

	@Override
	public void setManifest(Manifest manifest) {
		super.setManifest(manifest);
		ManifestResource resource = new FormatedManifestResource(manifest);
		// We must handle this with a little care here, first we put it as a
		// resource, what will make other parts of BND find it and copy it to
		// the output location(so it can be found when using the output as a
		// classpath)
		putResource(JarFile.MANIFEST_NAME, resource);
		// but we also need to make sure if BUNDLE_ROOT != output location
		// another copy for PDE and other things that expect it at the bundle
		// root...
		IFile file = outputFolder.getFile(IPath.fromOSString(JarFile.MANIFEST_NAME));
		if (!file.getFullPath().equals(manifestFile.getFullPath())) {
			// bundle root is currently not where we store it...
			if (manifestFile.exists()) {
				try (InputStream stream = resource.openInputStream()) {
					manifestFile.setContents(stream, true, false, null);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				try {
					mkdirs(manifestFile);
				} catch (CoreException e) {
					throw new RuntimeException(e);
				}
				try (InputStream stream = resource.openInputStream()) {
					manifestFile.create(stream, true, null);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			try {
				manifestFile.setDerived(true, new NullProgressMonitor());
			} catch (CoreException e) {
				// if only that don't work just go on...
			}
		}

	}

	@Override
	public boolean putResource(String path, Resource resource, boolean overwrite) {
		if (resource instanceof FileResource) {
			return super.putResource(path, resource, overwrite);
		}
		IFile file = outputFolder.getFile(IPath.fromOSString(path));
		try {
			if (file.exists()) {
				if (overwrite) {
					try (InputStream stream = resource.openInputStream()) {
						file.setContents(stream, true, false, null);
					}
				}
			} else {
				mkdirs(file);
				try (InputStream stream = resource.openInputStream()) {
					file.create(stream, true, null);
				}
			}
			file.setPersistentProperty(GENERATED_PROPERTY, path);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		orphanFilesMap.remove(path);
		return super.putResource(path, new FileResource(file), overwrite);
	}

	private void mkdirs(IResource resource) throws CoreException {
		if (resource == null) {
			return;
		}
		mkdirs(resource.getParent());
		if (resource instanceof IFolder folder) {
			if (!folder.exists()) {
				folder.create(true, true, null);
			}
		}
	}

	@Override
	public String toString() {
		return "Project" + super.toString(); //$NON-NLS-1$
	}

	@Override
	public void close() {
		cleanup();
		super.close();
	}

	private void cleanup() {
		for (IFile file : orphanFilesMap.values()) {
			try {
				file.delete(true, null);
			} catch (Exception e) {
				// must wait for next clean then...
			}
		}
	}

}
