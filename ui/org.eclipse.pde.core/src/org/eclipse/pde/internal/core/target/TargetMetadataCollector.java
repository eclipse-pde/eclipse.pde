/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;

/**
 * Utility class that collects the metadata repositories that a target definition has access to.
 * This class was created specifically to support providing pde build with a metadata context when
 * exporting features so that the metadata would be reused rather than be generated again.
 *
 * @since 3.6
 */
public class TargetMetadataCollector {

	/**
	 * Returns the list of URI locations that contain metadata repositories describing plug-ins in the
	 * given target definition.  The returned list may be empty or may not contain metadata for all
	 * plug-ins in the target.  The definition does not have to be resolved and this method will not
	 * resolve it.  If <code>null</code> is passed as the definition, this method will use {@link ITargetPlatformService}
	 * to get the active target definition.
	 *
	 * @param definition the target definition to load metadata repositories for or <code>null</code> to use the active target definition
	 * @return a list of URIs that specify metadata repository locations, possibly empty
	 * @throws CoreException if there is a problem working with the target definition
	 */
	public static URI[] getMetadataRepositories(ITargetDefinition definition) throws CoreException {
		// Lookup the active target definition
		if (definition == null) {
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (service == null) {
				return null;
			}
			definition = service.getWorkspaceTargetDefinition();
		}

		Set<URI> repos = new HashSet<>();

		ITargetLocation[] containers = definition.getTargetLocations();
		if (containers != null) {
			for (ITargetLocation currentContainer : containers) {
				if (currentContainer instanceof ProfileBundleContainer) {
					File profileLocation = ((ProfileBundleContainer) currentContainer).getProfileFileLocation();
					if (profileLocation != null) {
						repos.add(profileLocation.toURI());
					}
				} else if (currentContainer instanceof IUBundleContainer) {
					// PDE Build only wants local repositories as downloading can take as long as publishing new metadata.  Currently no way to get cached/downloaded metadata.
					URI[] locations = ((IUBundleContainer) currentContainer).getRepositories();
					if (locations != null) {
						for (URI location : locations) {
							if (URIUtil.isFileURI(location)) {
								repos.add(location);
							}
						}
					}
				}
			}
		}

		return repos.toArray(new URI[repos.size()]);
	}
}
