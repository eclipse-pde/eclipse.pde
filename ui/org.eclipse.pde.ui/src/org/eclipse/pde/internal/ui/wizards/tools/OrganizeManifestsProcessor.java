/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.search.dependencies.*;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;

public class OrganizeManifestsProcessor extends RefactoringProcessor implements IOrganizeManifestsSettings {

	// if operation is executed without setting operations, these defaults will
	// be used
	protected boolean fAddMissing = true; // add all packages to export-package
	protected boolean fMarkInternal = true; // mark export-package as internal
	protected String fPackageFilter = VALUE_DEFAULT_FILTER;
	protected boolean fRemoveUnresolved = true; // remove unresolved
												// export-package
	protected boolean fCalculateUses = false; // calculate the 'uses' directive
												// for exported packages
	protected boolean fModifyDep = true; // modify import-package /
											// require-bundle
	protected boolean fRemoveDependencies = true; // if true: remove, else mark
													// optional
	protected boolean fUnusedDependencies = false; // find/remove unused
													// dependencies - long
													// running op
	protected boolean fRemoveLazy = true; // remove lazy/auto start if no
											// activator
	protected boolean fRemoveUselessFiles = false; // remove fragment/plugin.xml
													// if no extension/extension
													// point defined
	protected boolean fPrefixIconNL = false; // prefix icon paths with $nl$
	protected boolean fUnusedKeys = false; // remove unused
											// <bundle-localization>.properties
											// keys
	protected boolean fAddDependencies = false;

	ArrayList<?> fProjectList;
	private IProject fCurrentProject;

	public OrganizeManifestsProcessor(ArrayList<?> projects) {
		fProjectList = projects;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		for (Object name : fProjectList) {
			if (!(name instanceof IProject))
				status.addFatalError(PDEUIMessages.OrganizeManifestsProcessor_invalidParam);
		}
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange(""); //$NON-NLS-1$
		change.markAsSynthetic();

		SubMonitor subMonitor = SubMonitor.convert(pm, PDEUIMessages.OrganizeManifestJob_taskName, fProjectList.size());
		for (Iterator<?> i = fProjectList.iterator(); i.hasNext() && !pm.isCanceled();) {
			CompositeChange projectChange = cleanProject((IProject) i.next(), subMonitor.split(1));
			if (projectChange.getChildren().length > 0)
				change.add(projectChange);
		}
		return change;
	}

	private CompositeChange cleanProject(IProject project, IProgressMonitor monitor) {
		fCurrentProject = project;
		CompositeChange change = new CompositeChange(NLS.bind(PDEUIMessages.OrganizeManifestsProcessor_rootMessage,
				new String[] { fCurrentProject.getName() }));
		final SubMonitor subMonitor = SubMonitor.convert(monitor, NLS.bind(
				PDEUIMessages.OrganizeManifestsProcessor_rootMessage, new String[] { fCurrentProject.getName() }), 1);
		final Change[] result = { null, null };
		final Exception[] ee = new Exception[1];
		ModelModification modification = new ModelModification(fCurrentProject) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase)
					try {
						SubMonitor subMonitor = SubMonitor.convert(monitor);
						runCleanup(subMonitor, (IBundlePluginModelBase) model, result);
					} catch (InvocationTargetException e) {
						ee[0] = e;
					} catch (InterruptedException e) {
						ee[0] = e;
					}
			}
		};
		Change[] changes = PDEModelUtility.changesForModelModication(modification, subMonitor);
		for (Change changeItem : changes)
			change.add(changeItem);
		if (result[0] != null)
			change.add(result[0]);
		if (result[1] != null)
			change.add(result[1]);
		if (ee[0] != null)
			PDEPlugin.log(ee[0]);
		return change;
	}

	private void runCleanup(IProgressMonitor monitor, IBundlePluginModelBase modelBase, Change[] result)
			throws InvocationTargetException, InterruptedException {

		IBundle currentBundle = modelBase.getBundleModel().getBundle();
		ISharedExtensionsModel sharedExtensionsModel = modelBase.getExtensionsModel();
		IPluginModelBase currentExtensionsModel = null;
		if (sharedExtensionsModel instanceof IPluginModelBase)
			currentExtensionsModel = (IPluginModelBase) sharedExtensionsModel;

		String projectName = fCurrentProject.getName();
		SubMonitor subMonitor = SubMonitor.convert(monitor,
				NLS.bind(PDEUIMessages.OrganizeManifestsOperation_export, projectName), 20);
		if (fAddMissing || fRemoveUnresolved) {
			if (!subMonitor.isCanceled())
				OrganizeManifest.organizeExportPackages(currentBundle, fCurrentProject, fAddMissing, fRemoveUnresolved);
			if (fAddMissing)
				subMonitor.worked(1);
			if (fRemoveUnresolved)
				subMonitor.worked(1);
		}

		if (fMarkInternal) {
			subMonitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_filterInternal, projectName));
			if (!subMonitor.isCanceled())
				OrganizeManifest.markPackagesInternal(currentBundle, fPackageFilter);
			subMonitor.worked(1);
		}

		if (fModifyDep) {
			String message = fRemoveDependencies
					? NLS.bind(PDEUIMessages.OrganizeManifestsOperation_removeUnresolved, projectName)
					: NLS.bind(PDEUIMessages.OrganizeManifestsOperation_markOptionalUnresolved, projectName);
			subMonitor.subTask(message);
			if (!subMonitor.isCanceled())
				OrganizeManifest.organizeImportPackages(currentBundle, fRemoveDependencies);
			subMonitor.worked(1);

			if (!subMonitor.isCanceled())
				OrganizeManifest.organizeRequireBundles(currentBundle, fRemoveDependencies);
			subMonitor.worked(1);
		}

		if (fCalculateUses) {
			// we don't set the subTask because it is done in the
			// CalculateUsesOperation, for each package it scans
			if (!subMonitor.isCanceled()) {
				CalculateUsesOperation op = new CalculateUsesOperation(fCurrentProject, modelBase);
				op.run(subMonitor.split(2));
			}
		}

		if (fAddDependencies) {
			subMonitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_additionalDeps, projectName));
			if (!subMonitor.isCanceled()) {
				AddNewDependenciesOperation op = new AddNewDependenciesOperation(fCurrentProject, modelBase);
				op.run(subMonitor.split(4));
			}
		}

		if (fUnusedDependencies) {
			subMonitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedDeps, projectName));
			if (!subMonitor.isCanceled()) {
				SubMonitor submon = subMonitor.split(4);
				GatherUnusedDependenciesOperation udo = new GatherUnusedDependenciesOperation(modelBase);
				udo.run(submon);
				GatherUnusedDependenciesOperation.removeDependencies(modelBase, udo.getList().toArray());
			}
		}

		if (fRemoveLazy) {
			subMonitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_lazyStart, fCurrentProject.getName()));
			if (!subMonitor.isCanceled())
				OrganizeManifest.removeUnneededLazyStart(currentBundle);
			subMonitor.worked(1);
		}

		if (fRemoveUselessFiles) {
			subMonitor.subTask(
					NLS.bind(PDEUIMessages.OrganizeManifestsOperation_uselessPluginFile, fCurrentProject.getName()));
			if (!subMonitor.isCanceled()) {
				result[1] = OrganizeManifest.deleteUselessPluginFile(fCurrentProject, currentExtensionsModel);
			}
			subMonitor.worked(1);
		}

		if (fPrefixIconNL) {
			subMonitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_nlIconPath, projectName));
			if (!subMonitor.isCanceled())
				OrganizeManifest.prefixIconPaths(currentExtensionsModel);
			subMonitor.worked(1);
		}

		if (fUnusedKeys) {
			subMonitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedKeys, projectName));
			if (!subMonitor.isCanceled()) {
				Change[] results = OrganizeManifest.removeUnusedKeys(fCurrentProject, currentBundle,
						currentExtensionsModel);
				if (results.length > 0)
					result[0] = results[0];
			}
			subMonitor.worked(1);
		}
		subMonitor.setWorkRemaining(0);
	}

	@Override
	public Object[] getElements() {
		return fProjectList.toArray();
	}

	@Override
	public String getIdentifier() {
		return getClass().getName();
	}

	@Override
	public String getProcessorName() {
		return PDEUIMessages.OrganizeManifestsWizardPage_title;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
			throws CoreException {
		return new RefactoringParticipant[0];
	}

	public void setAddMissing(boolean addMissing) {
		fAddMissing = addMissing;
	}

	public void setMarkInternal(boolean markInternal) {
		fMarkInternal = markInternal;
	}

	public void setPackageFilter(String packageFilter) {
		fPackageFilter = packageFilter;
	}

	public void setRemoveUnresolved(boolean removeUnresolved) {
		fRemoveUnresolved = removeUnresolved;
	}

	public void setCalculateUses(boolean calculateUses) {
		fCalculateUses = calculateUses;
	}

	public void setModifyDep(boolean modifyDep) {
		fModifyDep = modifyDep;
	}

	public void setRemoveDependencies(boolean removeDependencies) {
		fRemoveDependencies = removeDependencies;
	}

	public void setUnusedDependencies(boolean unusedDependencies) {
		fUnusedDependencies = unusedDependencies;
	}

	public void setRemoveLazy(boolean removeLazy) {
		fRemoveLazy = removeLazy;
	}

	public void setRemoveUselessFiles(boolean removeUselessFiles) {
		fRemoveUselessFiles = removeUselessFiles;
	}

	public void setPrefixIconNL(boolean prefixIconNL) {
		fPrefixIconNL = prefixIconNL;
	}

	public void setUnusedKeys(boolean unusedKeys) {
		fUnusedKeys = unusedKeys;
	}

	public void setAddDependencies(boolean addDependencies) {
		fAddDependencies = addDependencies;
	}
}
