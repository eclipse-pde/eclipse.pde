/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
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
		IFile file = (IFile)selection[0];
		// Ensure we have a TOC file
		if (!TocExtensionUtil.isTOCFile(file.getFullPath())) {
			return errorStatus(PDEUIMessages.TocFileValidator_errorInvalidTOC);
		}
		
		//Ensure that the TOC file selected isn't the current file
		if(TocExtensionUtil.isCurrentResource(file.getFullPath(), fModel))
		{	return errorStatus(PDEUIMessages.TocFileValidator_errorSameTOC);
		}

		// If we got this far, we have a valid file
		return okStatus(""); //$NON-NLS-1$
		
	}
	
	/**
	 * @param message
	 * @return
	 */
	private IStatus errorStatus(String message) {
		return new Status(
				IStatus.ERROR,
				PDEPlugin.getPluginId(),
				IStatus.ERROR,
				message,
				null);
	}
	
	/**
	 * @param message
	 * @return
	 */
	private IStatus okStatus(String message) {
		return new Status(
				IStatus.OK,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				message, 
				null);		
	}

}
