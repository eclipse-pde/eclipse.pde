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

import java.util.ArrayList;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.refactoring.descriptors.RenameResourceDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.IParticipantDescriptorFilter;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
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
		return null;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		IResource res = fInfo.getBase().getUnderlyingResource();
		if (res == null)
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_externalBundleError);
		else if (!res.getProject().getFile("META-INF/MANIFEST.MF").exists())  //$NON-NLS-1$
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_noManifestError);
		return status;
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
		change.add(createManifestChange(new SubProgressMonitor(pm, 1)));
		if (fInfo.isRenameProject())
			change.add(createProjectChange(new SubProgressMonitor(pm, 1)));
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

	private BundleTextChangeListener createListener(IBundle bundle) {
		if (bundle != null) {
			BundleTextChangeListener listener = new BundleTextChangeListener(((BundleModel)bundle.getModel()).getDocument());
			bundle.getModel().addModelChangedListener(listener);
			return listener;
		}
		return null;
	}
	
	private TextFileChange getTextChange(BundleTextChangeListener listener, IFile manifest) {
		TextEdit[] edits = listener.getTextOperations();
		if (edits.length == 0)
			return null;
		MultiTextEdit edit = new MultiTextEdit();
		edit.addChildren(edits);
		TextFileChange change = new TextFileChange("", manifest); //$NON-NLS-1$
		change.setEdit(edit);
		return change;
	}
	
	protected Change createManifestChange(IProgressMonitor monitor) throws CoreException {
		IProject proj = fInfo.getBase().getUnderlyingResource().getProject();
		IFile manifest = proj.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		monitor.beginTask("", 2); //$NON-NLS-1$
		try {
			String newId = fInfo.getNewID();
			IBundle bundle = BundleManifestChange.getBundle(manifest, new SubProgressMonitor(monitor, 1));
			if (bundle != null) {
				BundleTextChangeListener listener = createListener(bundle);
				if (listener != null) {
					BundleSymbolicNameHeader header = (BundleSymbolicNameHeader)bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
					header.setId(newId);

					return getTextChange(listener, manifest);
				}
			}
		} catch (CoreException e) {
		} catch (MalformedTreeException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), new SubProgressMonitor(monitor, 1));
			monitor.done();
		}
		return null;
	}
	
	protected Change createProjectChange(IProgressMonitor monitor) {
		RenameResourceDescriptor descriptor= new RenameResourceDescriptor();
		IProject project = fInfo.getBase().getUnderlyingResource().getProject();
		String newName = fInfo.getNewID();
		descriptor.setDescription(MessageFormat.format(PDEUIMessages.RenamePluginProcessor_renameProjectDesc, new String[] { project.getName(), newName }));
		descriptor.setComment(""); //$NON-NLS-1$
		descriptor.setFlags(RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE | RefactoringDescriptor.BREAKING_CHANGE);
		descriptor.setResource(project);
		descriptor.setNewName(fInfo.getNewID());
		monitor.done();
		return new RenameProjectChange(descriptor, project, fInfo.getNewID(), null);
	}
	
	protected Change[] createReferenceChanges(IProgressMonitor monitor) throws CoreException {
		ArrayList list = new ArrayList();
		IPluginModelBase currentBase = fInfo.getBase();
		BundleDescription desc = currentBase.getBundleDescription();
		if (desc != null) {
			String oldId = desc.getSymbolicName();
			BundleDescription[] dependents = desc.getDependents();
			monitor.beginTask("", dependents.length); //$NON-NLS-1$
			for (int i = 0; i < dependents.length; i++) {
				BundleSpecification[] requires = dependents[i].getRequiredBundles();
				for (int j = 0; j < requires.length; j++) {
					if (requires[j].getName().equals(oldId)) {
						IPluginModelBase base = PluginRegistry.findModel(dependents[i]);
						IResource res = base.getUnderlyingResource();
						if (res == null) {
							break;
						}
						IProject proj = res.getProject();
						IFile file = proj.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
						if (file.exists()) {
							TextFileChange change = updateRequireBundle(file, new SubProgressMonitor(monitor, 1));
							if (change != null)
								list.add(change);
						}
					}
				}
			}
		}
		return (Change[])list.toArray(new Change[list.size()]);
	}
	
	protected TextFileChange updateRequireBundle(IFile manifest, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("", 2); //$NON-NLS-1$
		try {
			String oldId = fInfo.getCurrentID();
			String newId = fInfo.getNewID();
			IBundle bundle = BundleManifestChange.getBundle(manifest, new SubProgressMonitor(monitor, 1));
			if (bundle != null) {
				BundleTextChangeListener listener = createListener(bundle);
				if (listener != null) {
					RequireBundleHeader header = (RequireBundleHeader)bundle.getManifestHeader(Constants.REQUIRE_BUNDLE);
					RequireBundleObject bundles[] = header.getRequiredBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].getId().equals(oldId)) 
							bundles[i].setId(newId);
					}
					
					return getTextChange(listener, manifest);
				}
			}
		} catch (MalformedTreeException e) {
		} catch (CoreException e) {
		} catch (BadLocationException e) {
		} finally {
			FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), new SubProgressMonitor(monitor, 1));
			monitor.done();
		}
		return null;
	}

}
