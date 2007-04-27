/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameResourceDescriptor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.IParticipantDescriptorFilter;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.Constants;

import com.ibm.icu.text.MessageFormat;

public class RenamePluginProcessor extends RefactoringProcessor {
	
	RenamePluginInfo fInfo;
	
	public RenamePluginProcessor (RenamePluginInfo info) { 
		fInfo = info;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws CoreException,
			OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		IResource res = fInfo.getBase().getUnderlyingResource();
		if (res == null)
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_externalBundleError);
		else if (!res.getProject().getFile("META-INF/MANIFEST.MF").exists())  //$NON-NLS-1$
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_noManifestError);
		if (fInfo.isRenameProject()) {
			String newName = fInfo.getNewID();
			IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(newName);
			// if destination exists and it is not the same project we are currently trying to rename, show error message
			if (newProject.exists() && !(res.getProject().equals(newProject)))
				status.addFatalError(MessageFormat.format(PDEUIMessages.RenameProjectChange_destinationExists, new String[] {newName}));
		}
		return status;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return null;
	}

	public Object[] getElements() {
		return new Object[] { fInfo.getBase() };
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

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status,
			SharableParticipants sharedParticipants) throws CoreException {
		if (fInfo.isRenameProject()) {
			// filter out PDE's container rename refactor participant.  We will already update the Manifest, so we don't need to run our participant
			IParticipantDescriptorFilter filter = new IParticipantDescriptorFilter() {
				static final String PDE_CONTAINER_RENAME_PARTICIPANT = "org.eclipse.pde.ui.manifestFolderRenameParticipant"; //$NON-NLS-1$
				
				public boolean select(IConfigurationElement element,
						RefactoringStatus status) {
					if (PDE_CONTAINER_RENAME_PARTICIPANT.equals(element.getAttribute("id"))) //$NON-NLS-1$
						return false;
					return true;
				}
			};
			
			IProject project = fInfo.getBase().getUnderlyingResource().getProject();
			return ParticipantManager.loadRenameParticipants(status,
					this,
					project,
					new RenameArguments(fInfo.getNewID(), true),
					filter,
					getAffectedNatures(project),
					sharedParticipants);
		}
		return new RefactoringParticipant[0];
	}
	
	private String[] getAffectedNatures(IProject project) throws CoreException {
		// NOTE: JDT searches each dependent project for additional natures
		return project.getDescription().getNatureIds();
	}
	
	public Change createChange(IProgressMonitor pm) throws CoreException,
	OperationCanceledException {
		CompositeChange change = new CompositeChange(MessageFormat.format(PDEUIMessages.RenamePluginProcessor_changeTitle, 
				new String[] {fInfo.getCurrentID(), fInfo.getNewID()}));
		pm.beginTask("", getTotalWork()); //$NON-NLS-1$
		// update manifest with new Id
		CreateHeaderChangeOperation op = new CreateHeaderChangeOperation(fInfo.getBase(),Constants.BUNDLE_SYMBOLICNAME, fInfo.getCurrentID(), fInfo.getNewID());
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
		if (fInfo.isRenameProject()) 		total += 1;
		if (fInfo.isUpdateReferences())		total += 2;
		return total;
	}
	
	protected Change createProjectChange(IProgressMonitor monitor) {
		RenameResourceDescriptor descriptor= new RenameResourceDescriptor();
		IProject project = fInfo.getBase().getUnderlyingResource().getProject();
		String newName = fInfo.getNewID();
		// if project's name is already the same as the destination, then we don't have to do anything to rename project
		if (project.getName().equals(newName))
			return null;
		descriptor.setDescription(MessageFormat.format(PDEUIMessages.RenamePluginProcessor_renameProjectDesc, new String[] { project.getName(), newName }));
		descriptor.setComment(""); //$NON-NLS-1$
		descriptor.setFlags(RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE);
		descriptor.setResource(project);
		descriptor.setNewName(fInfo.getNewID());
		monitor.done();
		return new RenameProjectChange(descriptor, project, fInfo.getNewID(), null);
	}
	
	protected Change[] createReferenceChanges(IProgressMonitor monitor) throws CoreException {
		IPluginModelBase currentBase = fInfo.getBase();
		BundleDescription desc = currentBase.getBundleDescription();
		if (desc == null) {
			IPluginModelBase savedBase = PluginRegistry.findModel(currentBase.getUnderlyingResource().getProject());
			desc = (savedBase != null) ? savedBase.getBundleDescription() : null;
		}
		if (desc != null) {
			FindReferenceOperation op =  new FindReferenceOperation(desc, fInfo.getNewID());
			op.run(monitor);
			return op.getChanges();
		}
		return new Change[0];
	}

}
