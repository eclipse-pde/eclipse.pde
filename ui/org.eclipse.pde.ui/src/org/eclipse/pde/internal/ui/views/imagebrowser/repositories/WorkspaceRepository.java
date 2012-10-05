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

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.views.imagebrowser.IImageTarget;

public class WorkspaceRepository extends AbstractRepository {

	public WorkspaceRepository() {
	}

	public IStatus searchImages(final IImageTarget target, final IProgressMonitor monitor) {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
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
								return Status.OK_STATUS;

							if (resource instanceof IFile) {
								try {
									if (isImageName(resource.getName()))
										target.notifyImage(createImageData((IFile) resource), pluginName, resource.getProjectRelativePath().toPortableString());

								} catch (Exception e) {
									// could not create image for location
								}
							} else if (resource instanceof IContainer)
								locations.add((IContainer) resource);
						}

					} while ((!locations.isEmpty()) && (!monitor.isCanceled()));
				} catch (CoreException e) {
				} catch (IOException e) {
				}
			}
		}

		return Status.OK_STATUS;
	}

	private String getPluginName(final InputStream manifest) throws IOException {
		Properties properties = new Properties();
		BufferedInputStream stream = new BufferedInputStream(manifest);
		properties.load(stream);
		stream.close();
		String property = properties.getProperty("Bundle-SymbolicName"); //$NON-NLS-1$
		if (property.contains(";")) //$NON-NLS-1$
			return property.substring(0, property.indexOf(';')).trim();

		return property.trim();
	}

	public String toString() {
		return "Workspace"; //$NON-NLS-1$
	}
}
