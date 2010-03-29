/*******************************************************************************
 *  Copyright (c) 2006, 2010 IBM Corporation and others.
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
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.osgi.framework.Constants;

public class ContainerRenameParticipant extends PDERenameParticipant {

	public String getName() {
		return PDEUIMessages.ContainerRenameParticipant_renameFolders;
	}

	protected boolean initialize(Object element) {
		if (element instanceof IContainer) {
			IProject project = ((IContainer) element).getProject();
			if (WorkspaceModelManager.isPluginProject(project)) {
				IPath path = ((IContainer) element).getProjectRelativePath().removeLastSegments(1);
				String newName = path.append(getArguments().getNewName()).toString();
				fProject = project;
				fElements = new HashMap();
				fElements.put(element, newName);
				return true;
			}
		}
		return false;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		// for the special case of a project rename, we need to only check the manifest for changes
		if (fElements.size() == 1 && fElements.keySet().iterator().next() instanceof IProject) {
			if (!getArguments().getUpdateReferences())
				return null;
			return createManifestChange(pm);
		}
		return super.createChange(pm);
	}

	protected Change createManifestChange(IProgressMonitor monitor) throws CoreException {
		IFile manifest = PDEProject.getManifest(fProject);
		if (manifest.exists()) {
			monitor.beginTask("", 4); //$NON-NLS-1$
			try {
				String newText = (String) fElements.get(fProject);
				CompositeChange result = new CompositeChange(PDEUIMessages.ContainerRenameParticipant_renameBundleId);
				IBundle bundle = BundleManifestChange.getBundle(manifest, new SubProgressMonitor(monitor, 1));
				if (bundle != null) {
					BundleTextChangeListener listener = new BundleTextChangeListener(((BundleModel) bundle.getModel()).getDocument());
					bundle.getModel().addModelChangedListener(listener);

					BundleSymbolicNameHeader header = (BundleSymbolicNameHeader) bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
					if (header != null) {
						// can't check the id to the project name.  Must run Id calculation code incase project name has invalid OSGi chars
						String calcProjectId = IdUtil.getValidId(fProject.getName());

						String oldText = header.getId();
						// don't update Bundle-SymbolicName if the id and project name don't match
						if (!oldText.equals(calcProjectId))
							return null;
						// remember to create a valid OSGi Bundle-SymbolicName.  Project name does not have that guarante
						String newId = IdUtil.getValidId(newText);

						header.setId(newId);
						// at this point, neither the project or file will exist.  
						// The project/resources get refactored before the TextChange is applied, therefore we need their future locations
						IProject newProject = ((IWorkspaceRoot) manifest.getProject().getParent()).getProject(newText);
						// If the manifest is in a non-standard location the new project will keep that location, only the project will be changed
						IPath oldManifest = manifest.getFullPath().removeFirstSegments(1);
						IFile newManifest = newProject.getFile(oldManifest);

						MovedTextFileChange change = new MovedTextFileChange("", newManifest, manifest); //$NON-NLS-1$
						MultiTextEdit edit = new MultiTextEdit();
						edit.addChildren(listener.getTextOperations());
						change.setEdit(edit);
						PDEModelUtility.setChangeTextType(change, manifest);
						result.add(change);

						// find all the references to the changing Bundle-SymbolicName and update all references to it
						FindReferenceOperation op = new FindReferenceOperation(PluginRegistry.findModel(fProject).getBundleDescription(), newId);
						op.run(new SubProgressMonitor(monitor, 2));
						result.addAll(op.getChanges());
						return result;
					}
				}
			} catch (CoreException e) {
			} catch (MalformedTreeException e) {
			} catch (BadLocationException e) {
			} finally {
				FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), LocationKind.NORMALIZE, new SubProgressMonitor(monitor, 1));
				monitor.done();
			}
		}
		return null;
	}

}
