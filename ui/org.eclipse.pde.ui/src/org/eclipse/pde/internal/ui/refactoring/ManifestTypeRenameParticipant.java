/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestTypeRenameParticipant extends PDERenameParticipant {

	protected boolean initialize(Object element) {
		if (element instanceof IType) {
			IType type = (IType)element;
			IJavaProject javaProject = (IJavaProject)type.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = javaProject.getProject();
				fElement = type;
				return true;
			}
		}
		return false;
	}
	
	protected String getOldName() {
		return ((IType)fElement).getFullyQualifiedName('$');
	}
	
	protected String getNewName() {
		IType type = (IType)fElement;
		String oldName = type.getFullyQualifiedName('$');
		int index = oldName.lastIndexOf(fElement.getElementName());
		StringBuffer buffer = new StringBuffer(oldName.substring(0, index));
		buffer.append(getArguments().getNewName());
		return buffer.toString();
	}

	public String getName() {
		return PDEUIMessages.ManifestTypeRenameParticipant_composite;
	}

}
