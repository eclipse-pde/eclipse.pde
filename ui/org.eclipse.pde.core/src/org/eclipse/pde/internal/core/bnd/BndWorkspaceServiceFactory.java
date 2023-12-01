/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import org.eclipse.core.runtime.ILog;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import aQute.bnd.build.Workspace;

public class BndWorkspaceServiceFactory implements ServiceFactory<Workspace> {

	@Override
	public Workspace getService(Bundle bundle, ServiceRegistration<Workspace> registration) {
		try {
			return BndProjectManager.getWorkspace();
		} catch (Exception e) {
			ILog.get().error("Creating Bnd Workspace Service failed!", e); //$NON-NLS-1$
			return null;
		}
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<Workspace> registration, Workspace service) {
	}

}
