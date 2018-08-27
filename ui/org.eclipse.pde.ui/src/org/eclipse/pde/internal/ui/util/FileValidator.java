/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public class FileValidator implements ISelectionStatusValidator {

	@Override
	public IStatus validate(Object[] selection) {
		if (selection.length > 0 && selection[0] instanceof IFile) {
			return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", //$NON-NLS-1$
					null);
		}
		return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, "", //$NON-NLS-1$
				null);
	}

}
