/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.refactoring;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class ManifestTypeRenameParticipant extends PDERenameParticipant {

	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IType) {
			IType type = (IType) element;
			IJavaProject javaProject = (IJavaProject) type.getAncestor(IJavaElement.JAVA_PROJECT);
			IProject project = javaProject.getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				fProject = javaProject.getProject();
				fElements = new HashMap<>();
				fElements.put(type, getArguments().getNewName());
				return true;
			}
		}
		return false;
	}

	protected String[] getOldNames() {
		String[] result = new String[fElements.size()];
		Iterator<Object> iter = fElements.keySet().iterator();
		for (int i = 0; i < fElements.size(); i++)
			result[i] = ((IType) iter.next()).getFullyQualifiedName('$');
		return result;
	}

	@Override
	protected String[] getNewNames() {
		List<String> result = new ArrayList<>(fElements.size());
		for (Entry<Object, String> entry : fElements.entrySet()) {
			IType type = (IType) entry.getKey();
			String oldName = type.getFullyQualifiedName('$');
			int index = oldName.lastIndexOf(type.getElementName());
			StringBuilder buffer = new StringBuilder(oldName.substring(0, index));
			buffer.append(fElements.get(type));
			result.add(buffer.toString());
		}
		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestTypeRenameParticipant_composite;
	}

}
