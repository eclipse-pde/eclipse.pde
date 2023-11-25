/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools.change;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public final class BndProjectUpdateChange extends Change {

	private IProject project;

	public BndProjectUpdateChange(IProject project) {
		this.project = project;
	}

	@Override
	public String getName() {
		return PDEUIMessages.ProjectUpdateChange_configure_nature_and_builder;
	}

	@Override
	public void initializeValidationData(IProgressMonitor pm) {
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException {
		IProjectDescription description = project.getDescription();
		String[] newNatures;
		if (Arrays.stream(description.getNatureIds())
				.anyMatch(str -> "org.eclipse.pde.api.tools.apiAnalysisNature".equals(str))) { //$NON-NLS-1$
			// the API nature require the pde nature so we can only add our
			// nature here...
			newNatures = Stream
					.concat(Arrays.stream(description.getNatureIds()), Stream.of(BndProject.NATURE_ID))
					.toArray(String[]::new);
		} else {
			// replace plugin with bnd nature...
			newNatures = Arrays.stream(description.getNatureIds()).map(nature -> {
				if (IBundleProjectDescription.PLUGIN_NATURE.equals(nature)) {
					return BndProject.NATURE_ID;
				}
				return nature;
			}).toArray(String[]::new);
		}
		ICommand[] commands = Stream.concat(Arrays.stream(description.getBuildSpec()).filter(command -> {
			if (PDE.MANIFEST_BUILDER_ID.equals(command.getBuilderName())
					|| "org.eclipse.pde.SchemaBuilder".equals(command.getBuilderName())) { //$NON-NLS-1$
				return false;
			}
			return true;
		}), Stream.of(newBndBuilder(description))).toArray(ICommand[]::new);
		description.setBuildSpec(commands);
		description.setNatureIds(newNatures);
		project.setDescription(description, pm);
		return null;
	}

	private ICommand newBndBuilder(IProjectDescription description) {
		ICommand bndBuilder = description.newCommand();
		bndBuilder.setBuilderName(BndProject.BUILDER_ID);
		return bndBuilder;
	}

	@Override
	public Object getModifiedElement() {
		return project;
	}

}