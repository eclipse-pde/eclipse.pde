/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.pde.internal.ua.ui.IConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public class CompCSFileValidator implements ISelectionStatusValidator {

	public CompCSFileValidator() {
		// NO-OP
	}

	@Override
	public IStatus validate(Object[] selection) {

		// Ensure something was selected
		if (selection.length == 0) {
			return Status.error(""); //$NON-NLS-1$
		}
		// Ensure we have a file
		if ((selection[0] instanceof IFile) == false) {
			return Status.error(""); //$NON-NLS-1$
		}
		IFile file = (IFile) selection[0];
		// Ensure we have a simple cheat sheet file
		if (isSimpleCSFile(file) == false) {
			return Status.error(Messages.CompCSFileValidator_0);
		}
		// If we got this far, we have a valid file
		return Status.OK_STATUS;

	}

	private boolean isSimpleCSFile(IFile file) {
		try {
			IContentDescription description = file.getContentDescription();
			IContentType type = description.getContentType();
			return type.getId().equalsIgnoreCase(
					IConstants.SIMPLE_CHEAT_SHEET_CONTENT_ID);
		} catch (CoreException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
		return false;
	}
}
