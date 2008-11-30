/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * TocFileValidator
 *
 */
public class TocFileValidator implements ISelectionStatusValidator {
	IBaseModel fModel;

	/**
	 * 
	 */
	public TocFileValidator(IBaseModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.ISelectionStatusValidator#validate(java.lang.Object[])
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
		// Ensure we have a TOC file
		if (!HelpEditorUtil.isTOCFile(file.getFullPath())) {
			return errorStatus(TocMessages.TocFileValidator_errorMessage1);
		}

		//Ensure that the TOC file selected isn't the current file
		if (HelpEditorUtil.isCurrentResource(file.getFullPath(), fModel)) {
			return errorStatus(TocMessages.TocFileValidator_errorMessage2);
		}

		// If we got this far, we have a valid file
		return okStatus(""); //$NON-NLS-1$

	}

	private IStatus errorStatus(String message) {
		return new Status(IStatus.ERROR, PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.ERROR, message, null);
	}

	private IStatus okStatus(String message) {
		return new Status(IStatus.OK, PDEUserAssistanceUIPlugin.PLUGIN_ID, IStatus.OK, message, null);
	}

}
