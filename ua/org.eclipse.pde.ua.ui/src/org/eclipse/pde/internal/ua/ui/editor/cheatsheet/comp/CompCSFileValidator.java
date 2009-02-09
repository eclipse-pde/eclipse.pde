/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.ISelectionStatusValidator#validate(java.lang.Object
	 * [])
	 */
	public IStatus validate(Object[] selection) {

		// Ensure something was selected
		if (selection.length == 0) {
			return errorStatus(""); //$NON-NLS-1$
		}
		// Ensure we have a file
		if ((selection[0] instanceof IFile) == false) {
			return errorStatus(""); //$NON-NLS-1$
		}
		IFile file = (IFile) selection[0];
		// Ensure we have a simple cheat sheet file
		if (isSimpleCSFile(file) == false) {
			return errorStatus(Messages.CompCSFileValidator_0);
		}
		// If we got this far, we have a valid file
		return okStatus(""); //$NON-NLS-1$

	}

	/**
	 * @param file
	 */
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

	/**
	 * @param message
	 * @return
	 */
	private IStatus errorStatus(String message) {
		return new Status(IStatus.ERROR, PDEUserAssistanceUIPlugin.PLUGIN_ID,
				IStatus.ERROR, message, null);
	}

	/**
	 * @param message
	 * @return
	 */
	private IStatus okStatus(String message) {
		return new Status(IStatus.OK, PDEUserAssistanceUIPlugin.PLUGIN_ID,
				IStatus.OK, message, null);
	}

}
