/*******************************************************************************
 *  Copyright (c) 2007, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import com.ibm.icu.text.MessageFormat;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;

public class RenameExtensionPointProcessor extends RefactoringProcessor {

	RefactoringInfo fInfo;

	public RenameExtensionPointProcessor(RefactoringInfo info) {
		fInfo = info;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		IResource res = fInfo.getBase().getUnderlyingResource();
		if (res == null)
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_externalBundleError);
		return status;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange(MessageFormat.format(PDEUIMessages.RenameExtensionPointProcessor_changeTitle, new String[] {fInfo.getCurrentValue(), fInfo.getNewValue()}));
		pm.beginTask("", 2); //$NON-NLS-1$
		changeExtensionPoint(change, new SubProgressMonitor(pm, 1));
		if (fInfo.isUpdateReferences())
			findReferences(change, new SubProgressMonitor(pm, 1));
		return change;
	}

	public Object[] getElements() {
		return new Object[] {fInfo.getSelection()};
	}

	public String getIdentifier() {
		return getClass().getName();
	}

	public String getProcessorName() {
		return PDEUIMessages.RenameExtensionPointProcessor_processorName;
	}

	public boolean isApplicable() throws CoreException {
		return true;
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	protected void changeExtensionPoint(CompositeChange compositeChange, IProgressMonitor monitor) {
		IFile file = getModificationFile(fInfo.getBase());
		if (file != null)
			compositeChange.addAll(PDEModelUtility.changesForModelModication(getExtensionPointModification(file), monitor));
	}

	private void findReferences(CompositeChange compositeChange, IProgressMonitor monitor) {
		String pointId = getId();
		IPluginModelBase[] bases = PDECore.getDefault().getExtensionsRegistry().findExtensionPlugins(pointId, true);
		monitor.beginTask("", bases.length); //$NON-NLS-1$
		for (int i = 0; i < bases.length; i++) {
			IFile file = getModificationFile(bases[i]);
			if (file != null)
				compositeChange.addAll(PDEModelUtility.changesForModelModication(getExtensionModification(file), new SubProgressMonitor(monitor, 1)));
		}
	}

	private String getId() {
		String currentValue = fInfo.getCurrentValue();
		if (currentValue.indexOf('.') > 0)
			return currentValue;
		IPluginModelBase base = PluginRegistry.findModel(fInfo.getBase().getUnderlyingResource().getProject());
		return (base == null) ? currentValue : base.getPluginBase().getId() + "." + currentValue; //$NON-NLS-1$
	}

	private String getNewId() {
		String newValue = fInfo.getNewValue();
		if (newValue.indexOf('.') > 0)
			return newValue;
		IPluginModelBase base = PluginRegistry.findModel(fInfo.getBase().getUnderlyingResource().getProject());
		return (base == null) ? newValue : base.getPluginBase().getId() + "." + newValue; //$NON-NLS-1$
	}

	private IFile getModificationFile(IPluginModelBase base) {
		IResource res = base.getUnderlyingResource();
		if (res != null) {
			IProject proj = res.getProject();
			IFile file = PDEProject.getPluginXml(proj);
			if (file.exists())
				return file;
		}
		return null;
	}

	protected ModelModification getExtensionPointModification(IFile file) {
		return new ModelModification(file) {

			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IPluginModelBase))
					return;
				IPluginModelBase modelBase = (IPluginModelBase) model;
				IPluginBase base = modelBase.getPluginBase();
				IPluginExtensionPoint[] points = base.getExtensionPoints();
				for (int i = 0; i < points.length; i++) {
					if (points[i].getId().equals(fInfo.getCurrentValue())) {
						points[i].setId(fInfo.getNewValue());
						// TODO Update schema
//						String schema = points[i].getSchema();
					}
				}
			}
		};
	}

	protected ModelModification getExtensionModification(IFile file) {
		return new ModelModification(file) {

			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IPluginModelBase))
					return;
				IPluginModelBase modelBase = (IPluginModelBase) model;
				IPluginBase base = modelBase.getPluginBase();
				IPluginExtension[] extensions = base.getExtensions();
				String oldValue = getId();
				for (int i = 0; i < extensions.length; i++)
					if (extensions[i].getPoint().equals(oldValue))
						extensions[i].setPoint(getNewId());
			}
		};
	}

}
