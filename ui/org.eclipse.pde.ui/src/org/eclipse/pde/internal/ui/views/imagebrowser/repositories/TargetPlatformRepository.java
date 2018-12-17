/*******************************************************************************
 *  Copyright (c) 2012, 2016 Christian Pontesegger and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser.repositories;

import java.io.File;
import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.imagebrowser.IImageTarget;
import org.eclipse.ui.PlatformUI;

public class TargetPlatformRepository extends AbstractRepository {

	private List<TargetBundle> fBundles = null;
	private boolean fUseCurrent;

	/**
	 * Creates a new target platform repository.  If useCurrent is <code>true</code>
	 * the current target platform set on the preference page.  If <code>false</code>
	 * a default target definition (the running application) will be used.
	 *
	 * @param target whom to notify upon found images
	 * @param useCurrent whether to use the current target platform or the default target (running application)
	 */
	public TargetPlatformRepository(IImageTarget target, boolean useCurrent) {
		super(target);

		fUseCurrent = useCurrent;
	}

	@Override
	protected boolean populateCache(final IProgressMonitor monitor) {
		if (fBundles == null)
			initialize(monitor);

		if ((fBundles != null) && (!fBundles.isEmpty())) {
			TargetBundle bundle = fBundles.remove(fBundles.size() - 1);
			URI location = bundle.getBundleInfo().getLocation();
			File file = new File(location);
			if (isJar(file)) {
				searchJarFile(file, monitor);

			} else if (file.isDirectory()) {
				searchDirectory(file, monitor);
			}

			return true;
		}

		return false;
	}

	private void initialize(final IProgressMonitor monitor) {

		try {

			ITargetPlatformService service = PlatformUI.getWorkbench().getService(ITargetPlatformService.class);
			if (service != null) {
				ITargetDefinition fDefinition = null;
				if (fUseCurrent) {
					fDefinition = service.getWorkspaceTargetDefinition();
				} else {
					fDefinition = service.newDefaultTarget();
				}

				if (fDefinition != null) {

					if (!fDefinition.isResolved())
						fDefinition.resolve(monitor);

					TargetBundle[] allBundles = fDefinition.getAllBundles();

					// populate bundles to visit
					if (allBundles != null) {
						fBundles = new ArrayList<>(Arrays.asList(allBundles));
					} else {
						fBundles = Collections.emptyList();
					}
				}

			} else {
				PDEPlugin.log(PDEUIMessages.TargetPlatformRepository_CouldNotFindTargetPlatformService);
			}

		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
	}

	@Override
	protected synchronized IStatus run(IProgressMonitor monitor) {
		super.run(monitor);
		if (fBundles != null) {
			fBundles.clear();
			fBundles = null;
		}
		if (mElementsCache != null)
			mElementsCache.clear();
		return Status.OK_STATUS;
	}

	@Override
	public String toString() {
		if (!fUseCurrent) {
			return PDEUIMessages.TargetPlatformRepository_RunningPlatform;
		}

		try {
			ITargetPlatformService service = PlatformUI.getWorkbench().getService(ITargetPlatformService.class);
			if (service != null) {
				ITargetDefinition definition = service.getWorkspaceTargetDefinition();
				String name = definition.getName();
				if (name == null)
					return ""; //$NON-NLS-1$
				if (name.length() > 30) {
					name = name.substring(0, 30);
				}
				return NLS.bind(PDEUIMessages.TargetPlatformRepository_TargetPlatformLabel, name);
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}

		return super.toString();
	}

}
