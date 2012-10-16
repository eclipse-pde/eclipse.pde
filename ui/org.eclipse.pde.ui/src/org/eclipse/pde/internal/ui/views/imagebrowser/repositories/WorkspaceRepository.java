/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.repositories;

import java.io.IOException;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.views.imagebrowser.IImageTarget;
import org.eclipse.pde.internal.ui.views.imagebrowser.ImageElement;

public class WorkspaceRepository extends AbstractRepository {

	private List<IProject> fProjects = null;

	public WorkspaceRepository(IImageTarget target) {
		super(target);
	}

	@Override
	protected boolean populateCache(IProgressMonitor monitor) {
		if (fProjects == null)
			initialize(monitor);

		if (!fProjects.isEmpty()) {
			IProject project = fProjects.get(fProjects.size() - 1);

			// look for a manifest
			IFile manifest = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
			if (manifest.exists()) {
				try {
					// extract plugin name
					String pluginName = getPluginName(manifest.getContents());

					// parse all folders
					Collection<IContainer> locations = new HashSet<IContainer>();
					locations.add(project);
					do {
						IContainer next = locations.iterator().next();
						locations.remove(next);

						for (IResource resource : next.members()) {
							if (monitor.isCanceled())
								return true;

							if (resource instanceof IFile) {
								try {
									if (isImageName(resource.getName().toLowerCase()))
										addImageElement(new ImageElement(createImageData((IFile) resource), pluginName, resource.getProjectRelativePath().toPortableString()));

								} catch (Exception e) {
									// could not create image for location
								}
							} else if (resource instanceof IContainer)
								locations.add((IContainer) resource);
						}

					} while ((!locations.isEmpty()) && (!monitor.isCanceled()));
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
			fProjects = new ArrayList<IProject>(Arrays.asList(projects));
		else
			fProjects = Collections.emptyList();
	}

	public String toString() {
		return "Workspace"; //$NON-NLS-1$
	}
}
