/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction.java;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.graphics.Image;

public class AddPdeClasspathContainerClasspathFixProposal extends ClasspathFixProposal {

	private final IJavaProject project;

	public AddPdeClasspathContainerClasspathFixProposal(IJavaProject project) {
		this.project = project;
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException {
		return newAddClasspathChange(project,
				JavaCore.newContainerEntry(PDECore.REQUIRED_PLUGINS_CONTAINER_PATH));
	}

	@Override
	public String getDisplayString() {
		return PDEUIMessages.AddPdeClasspathContainerClasspathFixProposal_0;
	}

	@Override
	public String getAdditionalProposalInfo() {
		return PDEUIMessages.AddPdeClasspathContainerClasspathFixProposal_1;
	}

	@Override
	public Image getImage() {
		return PDEPluginImages.get(PDEPluginImages.OBJ_DESC_BUNDLE);
	}

	@Override
	public int getRelevance() {
		return 10;
	}

}
