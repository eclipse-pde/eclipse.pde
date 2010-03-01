/******************************************************************************* 
* Copyright (c) 2010 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.net.URI;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;

public class InstallIUIntoTarget {

	public static void install(ITargetDefinition target, IInstallableUnit[] units, URI[] repositories) {
		ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
		IUBundleContainer container = (IUBundleContainer) service.newIUContainer(units, repositories);
		// TODO what other setup do we need ;)?
		// container.setIncludeAllRequired
		IBundleContainer[] oldContainers = target.getBundleContainers();
		if (oldContainers == null) {
			target.setBundleContainers(new IBundleContainer[] {container});
		} else {
			IBundleContainer[] newContainers = new IBundleContainer[oldContainers.length + 1];
			System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
			newContainers[newContainers.length - 1] = container;
			target.setBundleContainers(newContainers);
			try {
				service.saveTargetDefinition(target);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				LoadTargetDefinitionJob job = new LoadTargetDefinitionJob(target);
				job.schedule();
			}
		}
	}

}
