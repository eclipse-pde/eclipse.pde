/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
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
import org.eclipse.jdt.ui.actions.ShowInNavigatorViewAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ResourceAttributeRow extends ButtonAttributeRow {
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
		IResource file = getFile();
		boolean successful = false;
		if (file instanceof IFile)
			successful = openFile((IFile) file);
		else if (file instanceof IContainer)
			successful = openContainer((IContainer) file);
		if (!successful)
			Display.getCurrent().beep();
	}

	private boolean openFile(IFile file) {
		if (file != null && file.exists()) {
			try {
				IDE.openEditor(PDEPlugin.getActivePage(), file, true);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
				return false;
			}
			return true;
		}
		file = getNLFile();
		if (file != null && file.exists()) {
			try {
				IDE.openEditor(PDEPlugin.getActivePage(), file, true);
			} catch (PartInitException e) {
				PDEPlugin.logException(e);
				return false;
			}
			return true;
		}
		return false;
	}

	private boolean openContainer(IContainer container) {
		if (container != null && container.exists())
			try {
				IViewPart part = PDEPlugin.getActivePage().showView(IPageLayout.ID_RES_NAV);
				ShowInNavigatorViewAction action = new ShowInNavigatorViewAction(part.getSite());
				action.run(container);
			} catch (PartInitException e) {
				return false;
			}
		return true;
	}

	private IResource getFile() {
		String value = text.getText();
		if (value.length() == 0)
			return null;
		IPath path = getProject().getFullPath().append(value);
		return getProject().getWorkspace().getRoot().findMember(path);
	}

	private IFile getNLFile() {
		String value = text.getText();
		if (value.length() <= 5 || !value.startsWith("$nl$/"))return null; //$NON-NLS-1$
		IPath path = getProject().getFullPath().append(value.substring(5));
		return getProject().getWorkspace().getRoot().getFile(path);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#browse()
	 */
	protected void browse() {
		final IProject project = part.getPage().getPDEEditor().getCommonProject();
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setInput(project.getWorkspace());
		IResource resource = getFile();
		if (resource != null)
			dialog.setInitialSelection(resource);
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject)
					return ((IProject) element).equals(project);
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ResourceAttributeCellEditor_title);
		dialog.setMessage(PDEUIMessages.ResourceAttributeCellEditor_message);
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null && selection.length > 0 && (selection[0] instanceof IFile || selection[0] instanceof IContainer))
					return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$

				return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, "", null); //$NON-NLS-1$
			}
		});
		if (dialog.open() == Window.OK) {
			IResource res = (IResource) dialog.getFirstResult();
			IPath path = res.getProjectRelativePath();
			if (res instanceof IContainer)
				path = path.addTrailingSeparator();
			String value = path.toString();
			text.setText(value);
		}
	}
}
