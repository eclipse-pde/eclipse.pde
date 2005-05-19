/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.swt.graphics.Image;

public class LogViewLabelProvider
	extends LabelProvider
	implements ITableLabelProvider {
	private Image infoImage;
	private Image errorImage;
	private Image warningImage;
	private Image errorWithStackImage;

	public LogViewLabelProvider() {
		errorImage = PDERuntimePluginImages.DESC_ERROR_ST_OBJ.createImage();
		warningImage = PDERuntimePluginImages.DESC_WARNING_ST_OBJ.createImage();
		infoImage = PDERuntimePluginImages.DESC_INFO_ST_OBJ.createImage();
		errorWithStackImage = PDERuntimePluginImages.DESC_ERROR_STACK_OBJ.createImage();
	}
	public void dispose() {
		errorImage.dispose();
		infoImage.dispose();
		warningImage.dispose();
		errorWithStackImage.dispose();
		super.dispose();
	}
	public Image getColumnImage(Object element, int columnIndex) {
		LogEntry entry = (LogEntry) element;
		if (columnIndex == 0) {
			switch (entry.getSeverity()) {
				case IStatus.INFO :
					return infoImage;
				case IStatus.WARNING :
					return warningImage;
				default :
					return (entry.getStack() == null ? errorImage : errorWithStackImage);
			}
		}
		return null;
	}
	
	public String getColumnText(Object element, int columnIndex) {
		LogEntry entry = (LogEntry) element;
		switch (columnIndex) {
		case 0:
			return entry.getMessage() != null ? entry.getMessage() : ""; //$NON-NLS-1$
		case 1:
			return entry.getPluginId() != null ? entry.getPluginId() : ""; //$NON-NLS-1$
		case 2:
			return entry.getDate() != null ? entry.getDate() : ""; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
}
