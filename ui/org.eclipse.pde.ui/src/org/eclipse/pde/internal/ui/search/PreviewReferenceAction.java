/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class PreviewReferenceAction implements IObjectActionDelegate {
	private IFile fFile;
	private ShowDescriptionAction fDelegate;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	@Override
	public void run(IAction action) {
		if (fFile == null)
			return;
		SchemaDescriptor sd = new SchemaDescriptor(fFile, false);
		ISchema schema = sd.getSchema(false);
		if (fDelegate == null) {
			fDelegate = new ShowDescriptionAction(schema);
		} else
			fDelegate.setSchema(schema);
		fDelegate.run();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fFile = null;
		if (selection instanceof IStructuredSelection) {
			Object obj = ((IStructuredSelection) selection).getFirstElement();
			if (obj instanceof IFile)
				fFile = (IFile) obj;
		}
	}
}
