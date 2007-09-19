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
package org.eclipse.pde.internal.ui.correction.java;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.graphics.Image;

/**
 * Abstract class defining a classpath fix proposal that will 
 * offer to fix an unresolved import by adding a dependency to
 * the plugin.  Creates a Change object that will add the dependency
 * when invoked.  When invoked, the Change object will also add an
 * undo change that will remove the dependency.  Extending classes
 * must implement handleDependencyChange to determine how the
 * dependency will be added (bundle requirement, package import, etc).
 * Two other abstract methods allow extending classes to define the
 * label and description associated with this proposal and it's changes.
 *  
 * @since 3.4
 * @see UnresolvedImportFixProcessor
 * @see UnresolvedImportDependencyChange
 */
public abstract class UnresolvedImportFixProposal extends ClasspathFixProposal{
		
	private IProject fProject;
	private ExportPackageDescription fDependency;
	
	/**
	 * Constructor
	 * @param project The project that needs the new required bundle
	 * @param packageDescription the package we want a dependency on
	 */
	/**
	 * Constructor
	 * @param project the project that will be modified to add the dependency
	 * @param packageDependency the package that must be added as a dependency
	 */
	public UnresolvedImportFixProposal(IProject project, ExportPackageDescription packageDependency) {
		fProject = project;
		fDependency = packageDependency;
	}
	
	/**
	 * Returns the label to use for the proposal and change.
	 * Label can be different if this is the undo change and
	 * the dependency is being removed rather than added.
	 * 
	 * @param isAdd whether the dependency is being added vs removed
	 * @return label to use for the proposal and change
	 */
	public abstract String getLabel(boolean isAdd);
	
	/**
	 * Return the description that will be displayed for this
	 * proposal in the 'Additional Information' area of the 
	 * quick fix popup.
	 * 
	 * @return description to use for this proposal
	 */
	public abstract String getDescription();
	
	/**
	 * Implementors must handle the addition or removal of the given package
	 * as a dependency for the given model in this method.
	 * 
	 * @param pm progress monitor to display progress as change takes place
	 * @param model model that will be changed to add/remove the dependency
	 * @param dependency the package to add/remove as a dependency
	 * @param isAdd whether the package is being added or removed
	 * @throws CoreException if there is a problem trying to change the dependency.
	 */
	public abstract void handleDependencyChange(IProgressMonitor pm, IPluginModelBase model, ExportPackageDescription dependency, boolean isAdd) throws CoreException;

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor monitor) throws CoreException {
		return new UnresolvedImportDependencyChange(fProject,fDependency, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getAdditionalProposalInfo()
	 */
	public String getAdditionalProposalInfo() {
		return getDescription();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getDisplayString()
	 */
	public String getDisplayString() {
		return getLabel(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getImage()
	 */
	public Image getImage() {
		return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_MF_OBJ);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal#getRelevance()
	 */
	public int getRelevance() {
		return 16;
	}
	
	/**
	 * @return the project this proposal will change with the new dependency
	 */
	public IProject getProject(){
		return fProject;
	}
	
	/**
	 * @return the package dependency this proposal is offering add
	 */
	public ExportPackageDescription getDependency(){
		return fDependency;
	}

	/**
	 * A Change object that will modify a plugin to add or remove
	 * a dependency on a package.
	 * @see UnresolvedImportFixProposal 
	 * @see UnresolvedImportFixProcessor
	 */
	private class UnresolvedImportDependencyChange extends Change{

		private IProject fBaseProject;
		private ExportPackageDescription fDependency;
		private boolean fIsAdd;
		
		/**
		 * Constructor
		 * @param project the project to change dependency on
		 * @param packageDescription the package dependency to change
		 * @param isAdd whether the dependency will be added or removed
		 */
		public UnresolvedImportDependencyChange(IProject project, ExportPackageDescription packageDescription, boolean isAdd) {
			fBaseProject = project;
			fDependency = packageDescription;
			fIsAdd= isAdd;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#perform(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public Change perform(IProgressMonitor pm) throws CoreException {
			ModelModification modelMod = new ModelModification(fBaseProject){
				protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
					if (model instanceof IPluginModelBase) {
						handleDependencyChange(monitor, (IPluginModelBase)model, fDependency, fIsAdd);
					}
				}
			};
			PDEModelUtility.modifyModel(modelMod, pm);
			// Return the undo action to do the opposite
			return new UnresolvedImportDependencyChange(fBaseProject, fDependency, !fIsAdd);
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#isValid(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,	OperationCanceledException {
			return RefactoringStatus.create(Status.OK_STATUS);
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#initializeValidationData(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void initializeValidationData(IProgressMonitor pm) {
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#getName()
		 */
		public String getName() {
			return getLabel(fIsAdd);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ltk.core.refactoring.Change#getModifiedElement()
		 */
		public Object getModifiedElement() {
			return fBaseProject;
		}
	}
	
}
