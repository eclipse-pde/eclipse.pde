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

import org.eclipse.pde.internal.ui.PDEUIMessages;

import java.io.File;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.views.imagebrowser.IImageTarget;
import org.eclipse.ui.PlatformUI;

public class TargetPlatformRepository extends AbstractRepository {

	private boolean fUseCurrent;

	/**
	 * Creates a new target platform repository.  If useCurrent is <code>true</code>
	 * the current target platform set on the preference page.  If <code>false</code>
	 * a default target definition (the running application) will be used.
	 * 
	 * @param useCurrent whether to use the current target platform or the default target (running application)
	 */
	public TargetPlatformRepository(boolean useCurrent) {
		fUseCurrent = useCurrent;
	}

	public IStatus searchImages(final IImageTarget target, final IProgressMonitor monitor) {
		try {

			ITargetPlatformService service = (ITargetPlatformService) PlatformUI.getWorkbench().getService(ITargetPlatformService.class);
			if (service != null) {
				ITargetDefinition definition = null;
				if (fUseCurrent) {
					ITargetHandle workspaceTargetHandle = service.getWorkspaceTargetHandle();
					if (workspaceTargetHandle != null) {
						definition = workspaceTargetHandle.getTargetDefinition();
					}
				} else {
					definition = service.newDefaultTarget();
				}

				if (definition != null) {

					if (!definition.isResolved())
						definition.resolve(monitor);

					TargetBundle[] allBundles = definition.getAllBundles();
					if (allBundles != null) {
						for (TargetBundle bundle : allBundles) {
							URI location = bundle.getBundleInfo().getLocation();
							File file = new File(location);
							if (isJar(file))
								searchJarFile(file, target, monitor);
						}
					}
				}
			} else {
				PDEPlugin.logErrorMessage(PDEUIMessages.TargetPlatformRepository_CouldNotFindTargetPlatformService);
			}

		} catch (CoreException e) {
			PDEPlugin.log(e);
		}

		return Status.OK_STATUS;
	}

	public String toString() {
		if (!fUseCurrent) {
			return PDEUIMessages.TargetPlatformRepository_RunningPlatform;
		}

		try {
			ITargetPlatformService service = (ITargetPlatformService) PlatformUI.getWorkbench().getService(ITargetPlatformService.class);
			if (service != null) {
				ITargetHandle workspaceTargetHandle = service.getWorkspaceTargetHandle();
				if (workspaceTargetHandle != null) {
					ITargetDefinition definition = workspaceTargetHandle.getTargetDefinition();
					String name = definition.getName();
					if (name.length() > 30) {
						name = name.substring(0, 30);
					}
					return NLS.bind(PDEUIMessages.TargetPlatformRepository_TargetPlatformLabel, name);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}

		return super.toString();
	}

}
