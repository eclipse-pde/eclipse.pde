/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
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

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		IResource res = fInfo.getBase().getUnderlyingResource();
		if (res == null)
			status.addFatalError(PDEUIMessages.RenamePluginProcessor_externalBundleError);
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange(MessageFormat.format(PDEUIMessages.RenameExtensionPointProcessor_changeTitle, fInfo.getCurrentValue(), fInfo.getNewValue()));
		SubMonitor subMonitor = SubMonitor.convert(pm, 2);
		changeExtensionPoint(change, subMonitor.split(1));
		if (fInfo.isUpdateReferences())
			findReferences(change, subMonitor.split(1));
		return change;
	}

	@Override
	public Object[] getElements() {
		return new Object[] {fInfo.getSelection()};
	}

	@Override
	public String getIdentifier() {
		return getClass().getName();
	}

	@Override
	public String getProcessorName() {
		return PDEUIMessages.RenameExtensionPointProcessor_processorName;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
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
		SubMonitor subMonitor = SubMonitor.convert(monitor, bases.length);
		for (int i = 0; i < bases.length; i++) {
			IFile file = getModificationFile(bases[i]);
			if (file != null) {
				compositeChange.addAll(PDEModelUtility.changesForModelModication(getExtensionModification(file), subMonitor.split(1)));
			}
			subMonitor.setWorkRemaining(bases.length - i);
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

			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IPluginModelBase))
					return;
				IPluginModelBase modelBase = (IPluginModelBase) model;
				IPluginBase base = modelBase.getPluginBase();
				IPluginExtensionPoint[] points = base.getExtensionPoints();
				for (IPluginExtensionPoint point : points) {
					if (point.getId().equals(fInfo.getCurrentValue())) {
						point.setId(fInfo.getNewValue());
						// TODO Update schema
//						String schema = points[i].getSchema();
					}
				}
			}
		};
	}

	protected ModelModification getExtensionModification(IFile file) {
		return new ModelModification(file) {

			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IPluginModelBase))
					return;
				IPluginModelBase modelBase = (IPluginModelBase) model;
				IPluginBase base = modelBase.getPluginBase();
				IPluginExtension[] extensions = base.getExtensions();
				String oldValue = getId();
				for (IPluginExtension extension : extensions)
					if (extension.getPoint().equals(oldValue))
						extension.setPoint(getNewId());
			}
		};
	}

}
