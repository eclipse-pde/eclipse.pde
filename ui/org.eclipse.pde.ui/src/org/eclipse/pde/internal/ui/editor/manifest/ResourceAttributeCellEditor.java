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
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ResourceAttributeCellEditor extends DialogCellEditor {
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
				new WorkbenchContentProvider());
		dialog.setInput(project.getWorkspace());

		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject)
					return ((IProject) element).equals(project);
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString(TITLE));
		dialog.setMessage(
			PDEPlugin.getResourceString(
				"ManifestEditor.ResourceAttributeCellEditor.message"));
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null
					&& selection.length > 0
					&& selection[0] instanceof IFile)
					return new Status(
						IStatus.OK,
						PDEPlugin.getPluginId(),
						IStatus.OK,
						"",
						null);
				else
					return new Status(
						IStatus.ERROR,
						PDEPlugin.getPluginId(),
						IStatus.ERROR,
						"",
						null);
			}
		});

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			return new ResourceAttributeValue(
				project,
				file.getProjectRelativePath().toString());
		}
		return value;
	}
	
	protected void updateContents(Object value) {
		label.setText(value == null ? "" : value.toString());
	}
}
