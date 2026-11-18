/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.util.BundleResourceLocator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ResourceAttributeRow extends ButtonAttributeRow {
	public ResourceAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}

	@Override
	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}

	@Override
	protected void openReference() {
		if (!BundleResourceLocator.open(getProject(), text.getText())) {
			Display.getCurrent().beep();
		}
	}

	/**
	 * Returns the workspace resource the current value points at, which the browse
	 * dialog uses as its initial selection.
	 */
	private IResource getFile() {
		return BundleResourceLocator.findWorkspaceResource(getProject(), text.getText());
	}

	@Override
	protected void browse() {
		final IProject project = part.getPage().getPDEEditor().getCommonProject();
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setInput(project.getWorkspace());
		IResource resource = getFile();
		if (resource != null) {
			dialog.setInitialSelection(resource);
		}
		dialog.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject) {
					return ((IProject) element).equals(project);
				}
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.ResourceAttributeCellEditor_title);
		dialog.setMessage(PDEUIMessages.ResourceAttributeCellEditor_message);
		dialog.setValidator(selection -> {
			if (selection != null && selection.length > 0 && (selection[0] instanceof IFile || selection[0] instanceof IContainer)) {
				return Status.OK_STATUS;
			}
			return Status.error(""); //$NON-NLS-1$
		});
		if (dialog.open() == Window.OK) {
			IResource res = (IResource) dialog.getFirstResult();
			IPath path = res.getProjectRelativePath();
			if (res instanceof IContainer) {
				path = path.addTrailingSeparator();
			}
			String value = path.toString();
			text.setText(value);
		}
	}
}
