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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.core.resources.*;

public class ResourceAttributeCellEditor extends DialogCellEditor {
	class ContentProvider extends WorkbenchContentProvider {
		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof IFolder) {
					return true;
				}
				if (children[i] instanceof IFile) {
					String extension = ((IFile) children[i]).getFileExtension();
					if (extension == null)
						continue;
					if (extension.equals("bmp") || extension.equals("gif") || extension.equals("ico") || extension.equals("jpeg") || extension.equals("png"))
						return true;
				}
			}
			return false;
		}
		
	}
	public static final String TITLE = "ManifestEditor.ResourceAttributeCellEditor.title";
	private Label label;


protected ResourceAttributeCellEditor(Composite parent) {
	super(parent);
}
protected Control createContents(Composite cell) {
	label = new Label(cell, SWT.LEFT);
	label.setFont(cell.getFont());
	label.setBackground(cell.getBackground());
	return label;
}
protected Object openDialogBox(Control cellEditorWindow) {
	ResourceAttributeValue value = (ResourceAttributeValue) getValue();
	final IProject project = value.getProject();

	ElementTreeSelectionDialog dialog =
		new ElementTreeSelectionDialog(
			PDEPlugin.getActiveWorkbenchShell(),
			new WorkbenchLabelProvider(),
			new ContentProvider());
	dialog.setInput(project.getWorkspace());
	
	dialog.addFilter(new ViewerFilter() {
		
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IFile) {
				String extension = ((IFile) element).getFileExtension();
				if (extension == null)
					return false;
				return (extension.equals("bmp") || extension.equals("gif") || extension.equals("ico") || extension.equals("jpeg") || extension.equals("png"));
			} 
			if (element instanceof IProject)
				return ((IProject)element).equals(project);
			return true;
		}
	});
	dialog.setAllowMultiple(false);
	dialog.setTitle(PDEPlugin.getResourceString(TITLE));
	dialog.setMessage(PDEPlugin.getResourceString("ManifestEditor.ResourceAttributeCellEditor.message"));

	if (dialog.open() == ElementTreeSelectionDialog.OK) {
		IResource resource = (IResource) dialog.getResult()[0];
		if (resource instanceof IFile) {
			String stringValue = resource.getProjectRelativePath().toString();
			return new ResourceAttributeValue(project, stringValue);
		}
	}
	return value;
}
protected void updateContents(Object value) {
	if (value != null)
		label.setText(value.toString());
	else
		label.setText("");
}
}
