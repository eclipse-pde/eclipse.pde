/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ExternalArchiveSourceContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourcePathComputer;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceLocationManager;

public class SWTSourcePathComputer extends JavaSourcePathComputer {
	
	private static final String ID = "org.eclipse.pde.ui.swtSourcePathComputer"; //$NON-NLS-1$

	public String getId() {
		return ID;
	}
	
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		ISourceContainer[] containers =  super.computeSourceContainers(configuration, monitor);
		ArrayList extra = new ArrayList();

		BundleDescription[] fragments = SWTLaunchConfiguration.findFragments();
		
		for (int j = 0; j < fragments.length; j++) {
			if (!fragments[j].isResolved())
				continue;
			
			IFragmentModel model = PDECore.getDefault().getModelManager().findFragmentModel(fragments[j].getSymbolicName());
			IFragment fragment = model != null ? model.getFragment() : null;			
			if (fragment == null)
				return containers;
			
			IPath fragmentPath = new Path(fragment.getModel().getInstallLocation());
			for (int i = 0; i < containers.length; i++) {
				if (containers[i] instanceof PackageFragmentRootSourceContainer) {
					PackageFragmentRootSourceContainer container = (PackageFragmentRootSourceContainer)containers[i];
					IPackageFragmentRoot root = container.getPackageFragmentRoot();
					if (root.getSourceAttachmentPath() == null) {
						int matchCount = fragmentPath.matchingFirstSegments(root.getPath());
						if (matchCount == fragmentPath.segmentCount()) {
							IPath libPath = root.getPath().removeFirstSegments(matchCount);
							String libLocation = getLibrarySourceLocation(fragment, libPath);
							if (libLocation != null) {
								containers[i] = new ExternalArchiveSourceContainer(libLocation, false);
							}
						}
					}
				}
			}
			IResource resource = fragment.getModel().getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject jProject = JavaCore.create(project);
					extra.add(new JavaProjectSourceContainer(jProject));
					IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
					for (int i = 0; i < roots.length; i++) {
						if (roots[i].getKind() == IPackageFragmentRoot.K_BINARY 
								&& roots[i].getRawClasspathEntry().getEntryKind() == IClasspathEntry.CPE_LIBRARY) 
							extra.add(new PackageFragmentRootSourceContainer(roots[i]));
					}
				}
			} else {
				IPluginLibrary[] libraries = fragment.getLibraries();
				for (int i = 0; i < libraries.length; i++) {
					String name = ClasspathUtilCore.expandLibraryName(libraries[i].getName());
					String location = getLibrarySourceLocation(fragment, new Path(name));
					if (location != null)
						extra.add(new ExternalArchiveSourceContainer(location, false));
				}
			}			
		}
		
		if (extra.size() > 0) {
			ISourceContainer[] all = new ISourceContainer[containers.length + extra.size()];
			System.arraycopy(containers, 0, all, 0, containers.length);
			for (int i = 0; i < extra.size(); i++) {
				all[i+containers.length] = (ISourceContainer) extra.get(i);
			}
			return all;
		}
		return containers;
	}
	
	private String getLibrarySourceLocation(IFragment fragment, IPath path) {
		String library = path.segmentCount() == 0 ? "." : path.setDevice(null).toString(); //$NON-NLS-1$
		SourceLocationManager manager = PDECore.getDefault().getSourceLocationManager();
		int dot = library.lastIndexOf('.');
		if (dot != -1) {
			library = library.substring(0, dot) + "src.zip"; //$NON-NLS-1$
		}
		File file = manager.findSourceFile(fragment, new Path(library));
		return file == null ? null : file.getAbsolutePath();
	}
}
