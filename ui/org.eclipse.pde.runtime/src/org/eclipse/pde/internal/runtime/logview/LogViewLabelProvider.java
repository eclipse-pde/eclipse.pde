/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.logview;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.pde.internal.runtime.PDERuntimePluginImages;
import org.eclipse.swt.graphics.Image;

public class LogViewLabelProvider
	extends LabelProvider
	implements ITableLabelProvider {
	
	private static int MAX_LABEL_LENGTH = 200;
	
	private Image infoImage;
	private Image okImage;
	private Image errorImage;
	private Image warningImage;
	private Image errorWithStackImage;
	ArrayList consumers = new ArrayList();

	public LogViewLabelProvider() {
		errorImage = PDERuntimePluginImages.DESC_ERROR_ST_OBJ.createImage();
		warningImage = PDERuntimePluginImages.DESC_WARNING_ST_OBJ.createImage();
		infoImage = PDERuntimePluginImages.DESC_INFO_ST_OBJ.createImage();
		okImage = PDERuntimePluginImages.DESC_OK_ST_OBJ.createImage();
		errorWithStackImage = PDERuntimePluginImages.DESC_ERROR_STACK_OBJ.createImage();
	}
	public void dispose() {
		if (consumers.size() == 0){
			errorImage.dispose();
			infoImage.dispose();
			okImage.dispose();
			warningImage.dispose();
			errorWithStackImage.dispose();
			super.dispose();
		}
	}
	public Image getColumnImage(Object element, int columnIndex) {
		LogEntry entry = (LogEntry) element;
		if (columnIndex == 0) {
			switch (entry.getSeverity()) {
				case IStatus.INFO :
					return infoImage;
				case IStatus.OK :
					return okImage;
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
			if (entry.getMessage() != null) {
				String message = entry.getMessage();
				if (message.length() > MAX_LABEL_LENGTH) {
					String warning = PDERuntimeMessages.LogViewLabelProvider_truncatedMessage;
					StringBuffer sb = new StringBuffer(message.substring(0, MAX_LABEL_LENGTH - warning.length()));
					sb.append(warning);
					return sb.toString();
				}
				return entry.getMessage();
			}
		case 1:
			if (entry.getPluginId() != null)
				return entry.getPluginId();
		case 2:
			return entry.getFormattedDate();
		}
		return ""; //$NON-NLS-1$
	}

	public void connect(Object consumer) {
		if (!consumers.contains(consumer))
			consumers.add(consumer);
	}
	
	public void disconnect(Object consumer) {
		consumers.remove(consumer);
		if (consumers.size() == 0) {
			dispose();
		}
	}
}
