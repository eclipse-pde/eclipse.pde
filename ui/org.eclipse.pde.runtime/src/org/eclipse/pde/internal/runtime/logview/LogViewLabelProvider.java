package org.eclipse.pde.internal.runtime.logview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.runtime.*;

public class LogViewLabelProvider extends LabelProvider implements ITableLabelProvider {
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
	IStatus status = ((StatusAdapter) element).getStatus();
	if (columnIndex == 1) {
		if (status.isOK()) {
		} else {
			switch (status.getSeverity()) {
				case IStatus.INFO :
					return infoImage;
				case IStatus.WARNING :
					return warningImage;
				case IStatus.ERROR :
					return errorImage;
			}
		}
	}
	return null;
}
public String getColumnText(Object element, int columnIndex) {
	IStatus status = ((StatusAdapter)element).getStatus();
	switch (columnIndex) {
		case 1:
			/* return getSeverityText(status.getSeverity()); */
			break;
		case 2:
			return status.getMessage();
		case 3:
			return status.getPlugin();
	}
	return "";
}
}
