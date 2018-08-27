/*******************************************************************************
 *  Copyright (c) 2012, 2015 Christian Pontesegger and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.repositories;

import java.io.IOException;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.views.imagebrowser.IImageTarget;
import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public class WorkspaceRepository extends AbstractRepository {

	private List<IProject> fProjects = null;

	public WorkspaceRepository(IImageTarget target) {
		super(target);
	}

	@Override
	protected boolean populateCache(final IProgressMonitor monitor) {
		if (fProjects == null)
			initialize(monitor);

		if (!fProjects.isEmpty()) {

			final IProject project = fProjects.remove(0);

			// look for a manifest
			IFile manifest = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$

			if (manifest.exists()) {
				try {
					// extract plugin name
					final String pluginName = getPluginName(manifest.getContents());

					// parse all folders
					project.accept((IResourceProxyVisitor) proxy -> {

						switch (proxy.getType()) {
							case IResource.PROJECT :
								// fall through
							case IResource.FOLDER :
								// parse subfolders
								return true;

							case IResource.FILE :
								// look for image files
								if (isImageName(proxy.getName())) {
									try {
										IFile resource = (IFile) proxy.requestResource();
										addImageElement(new ImageElement(createImageData(resource), pluginName, resource.getProjectRelativePath().toPortableString()));
									} catch (Exception e) {
										// could not create image for location
									}

									if (monitor.isCanceled())
										throw new OperationCanceledException();
								}

								break;
						}

						return false;
					}, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				} catch (IOException e) {
					PDEPlugin.log(e);
				}
			}
			return true;
		}

		return false;
	}

	private void initialize(IProgressMonitor monitor) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects != null)
			fProjects = new ArrayList<>(Arrays.asList(projects));
		else
			fProjects = Collections.emptyList();
	}

	@Override
	protected synchronized IStatus run(IProgressMonitor monitor) {
		super.run(monitor);
		if (fProjects != null) {
			fProjects.clear();
			fProjects = null;
		}
		if (mElementsCache != null)
			mElementsCache.clear();
		return Status.OK_STATUS;
	}


	@Override
	public String toString() {
		return "Workspace"; //$NON-NLS-1$
	}
}
