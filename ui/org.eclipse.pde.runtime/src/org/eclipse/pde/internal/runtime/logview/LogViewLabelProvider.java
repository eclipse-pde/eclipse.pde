/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

	public LogViewLabelProvider() {
		errorImage = PDERuntimePluginImages.DESC_ERROR_ST_OBJ.createImage();
		warningImage = PDERuntimePluginImages.DESC_WARNING_ST_OBJ.createImage();
		infoImage = PDERuntimePluginImages.DESC_INFO_ST_OBJ.createImage();
	}
	public void dispose() {
		errorImage.dispose();
		infoImage.dispose();
		warningImage.dispose();
		super.dispose();
	}
	public Image getColumnImage(Object element, int columnIndex) {
		LogEntry entry = (LogEntry) element;
		if (columnIndex == 1) {
			switch (entry.getSeverity()) {
				case IStatus.INFO :
					return infoImage;
				case IStatus.WARNING :
					return warningImage;
				case IStatus.ERROR :
					return errorImage;
			}
		}
		return null;
	}
	
	public String getColumnText(Object element, int columnIndex) {
		LogEntry entry = (LogEntry) element;
		switch (columnIndex) {
			case 2 :
				return entry.getMessage();
			case 3 :
				return entry.getPluginId();
			case 4 :
				return entry.getDate();
		}
		return "";
	}
}
