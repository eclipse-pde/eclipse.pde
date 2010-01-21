/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import com.ibm.icu.text.MessageFormat;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

public class RenamePluginProcessor extends RefactoringProcessor {

	RefactoringPluginInfo fInfo;

	public RenamePluginProcessor(RefactoringInfo info) {
		fInfo = (RefactoringPluginInfo) info;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		IResource res = fInfo.getBase().getUnderlyingResource();
		if (res == null)
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_externalBundleError);
		else if (!PDEProject.getManifest(res.getProject()).exists())
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_noManifestError);
		if (fInfo.isRenameProject()) {
			String newName = fInfo.getNewValue();
			IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newName);
			// if destination exists and it is not the same project we are currently trying to rename, show error message
			if (newProject.exists() && !(res.getProject().equals(newProject)))
				status.addFatalError(MessageFormat.format(PDEUIMessages.RenameProjectChange_destinationExists, new String[] {newName}));
		}
		return status;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	public Object[] getElements() {
		return new Object[] {fInfo.getBase()};
	}

	public String getIdentifier() {
		return getClass().getName();
	}

	public String getProcessorName() {
		return PDEUIMessages.RenamePluginProcessor_processorName;
	}

	public boolean isApplicable() throws CoreException {
		return true;
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		if (fInfo.isRenameProject()) {
			// filter out PDE's container rename refactor participant.  We will already update the Manifest, so we don't need to run our participant
			IParticipantDescriptorFilter filter = new IParticipantDescriptorFilter() {
				static final String PDE_CONTAINER_RENAME_PARTICIPANT = "org.eclipse.pde.ui.manifestFolderRenameParticipant"; //$NON-NLS-1$

				public boolean select(IConfigurationElement element, RefactoringStatus status) {
					if (PDE_CONTAINER_RENAME_PARTICIPANT.equals(element.getAttribute("id"))) //$NON-NLS-1$
						return false;
					return true;
				}
			};

			IProject project = fInfo.getBase().getUnderlyingResource().getProject();
			return ParticipantManager.loadRenameParticipants(status, this, project, new RenameArguments(fInfo.getNewValue(), true), filter, getAffectedNatures(project), sharedParticipants);
		}
		return new RefactoringParticipant[0];
	}

	private String[] getAffectedNatures(IProject project) throws CoreException {
		// NOTE: JDT searches each dependent project for additional natures
		return project.getDescription().getNatureIds();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange(MessageFormat.format(PDEUIMessages.RenamePluginProcessor_changeTitle, new String[] {fInfo.getCurrentValue(), fInfo.getNewValue()}));
		pm.beginTask("", getTotalWork()); //$NON-NLS-1$
		// update manifest with new Id
		CreateHeaderChangeOperation op = new CreateHeaderChangeOperation(fInfo.getBase(), Constants.BUNDLE_SYMBOLICNAME, fInfo.getCurrentValue(), fInfo.getNewValue());
		op.run(new SubProgressMonitor(pm, 1));
		change.add(op.getChange());

		if (fInfo.isRenameProject()) {
			change.add(createProjectChange(new SubProgressMonitor(pm, 1)));
		}
		if (fInfo.isUpdateReferences())
			change.addAll(createReferenceChanges(new SubProgressMonitor(pm, 2)));
		return change;
	}

	private int getTotalWork() {
		int total = 1;
		if (fInfo.isRenameProject())
			total += 1;
		if (fInfo.isUpdateReferences())
			total += 2;
		return total;
	}

	protected Change createProjectChange(IProgressMonitor monitor) {
		RenameResourceDescriptor descriptor = new RenameResourceDescriptor();
		IProject project = fInfo.getBase().getUnderlyingResource().getProject();
		String newName = fInfo.getNewValue();
		// if project's name is already the same as the destination, then we don't have to do anything to rename project
		if (project.getName().equals(newName))
			return null;
		descriptor.setDescription(MessageFormat.format(PDEUIMessages.RenamePluginProcessor_renameProjectDesc, new String[] {project.getName(), newName}));
		descriptor.setComment(""); //$NON-NLS-1$
		descriptor.setFlags(RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE);
		descriptor.setResourcePath(project.getFullPath());
		descriptor.setNewName(fInfo.getNewValue());
		monitor.done();
		return new RenameProjectChange(descriptor, project, fInfo.getNewValue(), null);
	}

	protected Change[] createReferenceChanges(IProgressMonitor monitor) throws CoreException {
		IPluginModelBase currentBase = fInfo.getBase();
		BundleDescription desc = currentBase.getBundleDescription();
		if (desc == null) {
			IPluginModelBase savedBase = PluginRegistry.findModel(currentBase.getUnderlyingResource().getProject());
			desc = (savedBase != null) ? savedBase.getBundleDescription() : null;
		}
		if (desc != null) {
			FindReferenceOperation op = new FindReferenceOperation(desc, fInfo.getNewValue());
			op.run(monitor);
			return op.getChanges();
		}
		return new Change[0];
	}

}
