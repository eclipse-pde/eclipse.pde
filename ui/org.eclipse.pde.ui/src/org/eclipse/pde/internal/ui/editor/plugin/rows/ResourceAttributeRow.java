/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin.rows;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.*;

public class ResourceAttributeRow extends ReferenceAttributeRow {
	public ResourceAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#openReference()
	 */
	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}	
	protected void openReference() {
		IFile file = getFile();
		if (file!=null && file.exists()) {
			try {
				IDE.openEditor(PDEPlugin.getActivePage(), file, true);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
			}
		} else {
			Display.getCurrent().beep();
		}
	}
	private IFile getFile() {
		String value = text.getText();
		if (value.length()==0) return null;
		IPath path = getProject().getFullPath().append(value);
		return getProject().getWorkspace().getRoot().getFile(path);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#browse()
	 */
	protected void browse() {
		final IProject project = part.getPage().getPDEEditor()
				.getCommonProject();
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setInput(project.getWorkspace());
		IFile file = getFile();
		if (file!=null)
			dialog.setInitialSelection(file);
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof IProject)
					return ((IProject) element).equals(project);
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog
				.setTitle(PDEPlugin
						.getResourceString("ResourceAttributeCellEditor.title")); //$NON-NLS-1$
		dialog
				.setMessage(PDEPlugin
						.getResourceString("ResourceAttributeCellEditor.message")); //$NON-NLS-1$
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null && selection.length > 0
						&& selection[0] instanceof IFile)
					return new Status(IStatus.OK, PDEPlugin.getPluginId(),
							IStatus.OK, "", null); //$NON-NLS-1$
				
				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
						IStatus.ERROR, "", null); //$NON-NLS-1$
			}
		});
		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			file = (IFile) dialog.getFirstResult();
			String value = file.getProjectRelativePath().toString();
			text.setText(value);
		}
	}
}
