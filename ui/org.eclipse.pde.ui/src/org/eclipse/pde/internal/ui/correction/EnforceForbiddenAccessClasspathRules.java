/*******************************************************************************
 *  Copyright (c) 2026 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.service.prefs.BackingStoreException;

public class EnforceForbiddenAccessClasspathRules extends AbstractPDEMarkerResolution {

	public EnforceForbiddenAccessClasspathRules(IMarker marker) {
		super(AbstractPDEMarkerResolution.CONFIGURE_TYPE, marker);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.EnableStrictClasspathAccessRules_label;
	}

	@Override
	public void run(IMarker marker) {
		IProject project = marker.getResource().getProject();
		IEclipsePreferences node = new ProjectScope(project).getNode("org.eclipse.jdt.core"); //$NON-NLS-1$
		try {
			node.sync();
			node.put(JavaCore.COMPILER_PB_FORBIDDEN_REFERENCE, JavaCore.ERROR);
			node.flush();
		} catch (BackingStoreException e) {
			ILog.get().error("Failed to update JDT Core preferences", e); //$NON-NLS-1$
		}
	}

	@Override
	protected void createChange(IBaseModel model) {
		// handled by run
	}
}

