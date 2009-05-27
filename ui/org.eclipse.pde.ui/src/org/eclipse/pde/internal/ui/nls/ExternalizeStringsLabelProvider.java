/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

public class ExternalizeStringsLabelProvider extends LabelProvider implements ITableLabelProvider, IFontProvider {

	private FontRegistry fFontRegistry;

	public ExternalizeStringsLabelProvider() {
		fFontRegistry = JFaceResources.getFontRegistry();
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof ModelChangeElement) {
			ModelChangeElement changeElement = (ModelChangeElement) element;
			if (columnIndex == ExternalizeStringsWizardPage.VALUE) {
				return StringHelper.unwindEscapeChars(changeElement.getValue());
			} else if (columnIndex == ExternalizeStringsWizardPage.KEY) {
				return StringHelper.unwindEscapeChars(changeElement.getKey());
			}
		}
		return ""; //$NON-NLS-1$
	}

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public Font getFont(Object element) {
		if (element instanceof ModelChangeElement) {
			ModelChangeElement changeElement = (ModelChangeElement) element;
			if (changeElement.isExternalized()) {
				return fFontRegistry.getBold(JFaceResources.DIALOG_FONT);
			}
		}
		return null;
	}
}
