/*******************************************************************************
 *  Copyright (c) 2012, 2024 Christian Pontesegger and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
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
		if (fProjects == null) {
			initialize(monitor);
		}

		if (!fProjects.isEmpty()) {

			final IProject project = fProjects.remove(0);

			// look for a manifest
			IFile manifest = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$

			if (manifest.exists()) {
				try {
					// extract plugin name
					Optional<String> name = getPluginName(manifest.getContents());
					if (!name.isPresent()) {
						return !fProjects.isEmpty();
					}
					final String pluginName = name.get();

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
									IFile resource = (IFile) proxy.requestResource();
									addImageElement(new ImageElement(() -> createImageData(resource), pluginName, resource.getProjectRelativePath().toPortableString()));

									if (monitor.isCanceled()) {
										throw new OperationCanceledException();
									}
								}

								break;
						}

						return false;
					}, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
				} catch (CoreException | IOException e) {
					PDEPlugin.log(e);
				}
			}
			return true;
		}

		return false;
	}

	private void initialize(IProgressMonitor monitor) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		if (projects != null) {
			fProjects = new ArrayList<>(Arrays.asList(projects));
		} else {
			fProjects = Collections.emptyList();
		}
	}

	@Override
	protected synchronized IStatus run(IProgressMonitor monitor) {
		super.run(monitor);
		if (fProjects != null) {
			fProjects.clear();
			fProjects = null;
		}
		if (mElementsCache != null) {
			mElementsCache.clear();
		}
		return Status.OK_STATUS;
	}


	@Override
	public String toString() {
		return "Workspace"; //$NON-NLS-1$
	}
}
