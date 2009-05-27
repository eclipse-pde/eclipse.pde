/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestTypeRenameParticipant extends PDERenameParticipant {

	protected boolean initialize(Object element) {
		if (element instanceof IType) {
			IType type = (IType) element;
			IJavaProject javaProject = (IJavaProject) type.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = javaProject.getProject();
				fElements = new HashMap();
				fElements.put(type, getArguments().getNewName());
				return true;
			}
		}
		return false;
	}

	protected String[] getOldNames() {
		String[] result = new String[fElements.size()];
		Iterator iter = fElements.keySet().iterator();
		for (int i = 0; i < fElements.size(); i++)
			result[i] = ((IType) iter.next()).getFullyQualifiedName('$');
		return result;
	}

	protected String[] getNewNames() {
		String[] result = new String[fElements.size()];
		Iterator iter = fElements.keySet().iterator();
		for (int i = 0; i < fElements.size(); i++) {
			IType type = (IType) iter.next();
			String oldName = type.getFullyQualifiedName('$');
			int index = oldName.lastIndexOf(type.getElementName());
			StringBuffer buffer = new StringBuffer(oldName.substring(0, index));
			buffer.append(fElements.get(type));
			result[i] = buffer.toString();
		}
		return result;
	}

	public String getName() {
		return PDEUIMessages.ManifestTypeRenameParticipant_composite;
	}

}
