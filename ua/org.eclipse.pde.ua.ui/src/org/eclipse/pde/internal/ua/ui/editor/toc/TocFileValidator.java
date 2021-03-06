/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.toc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

public class TocFileValidator implements ISelectionStatusValidator {
	IBaseModel fModel;

	public TocFileValidator(IBaseModel model) {
		fModel = model;
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
		// Ensure we have a TOC file
		if (!HelpEditorUtil.isTOCFile(file.getFullPath())) {
			return Status.error(TocMessages.TocFileValidator_errorMessage1);
		}

		//Ensure that the TOC file selected isn't the current file
		if (HelpEditorUtil.isCurrentResource(file.getFullPath(), fModel)) {
			return Status.error(TocMessages.TocFileValidator_errorMessage2);
		}

		// If we got this far, we have a valid file
		return Status.OK_STATUS;

	}
}
