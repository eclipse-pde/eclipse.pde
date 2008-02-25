/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513
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

	// if operation is executed without setting operations, these defaults will be used
	protected boolean fAddMissing = true; // add all packages to export-package
	protected boolean fMarkInternal = true; // mark export-package as internal
	protected String fPackageFilter = VALUE_DEFAULT_FILTER;
	protected boolean fRemoveUnresolved = true; // remove unresolved export-package
	protected boolean fCalculateUses = false; // calculate the 'uses' directive for exported packages
	protected boolean fModifyDep = true; // modify import-package / require-bundle
	protected boolean fRemoveDependencies = true; // if true: remove, else mark optional
	protected boolean fUnusedDependencies = false; // find/remove unused dependencies - long running op
	protected boolean fRemoveLazy = true; // remove lazy/auto start if no activator
	protected boolean fRemoveUselessFiles = false; // remove fragment/plugin.xml if no extension/extension point defined
	protected boolean fPrefixIconNL = false; // prefix icon paths with $nl$
	protected boolean fUnusedKeys = false; // remove unused <bundle-localization>.properties keys
	protected boolean fAddDependencies = false;

	ArrayList fProjectList;
	private IProject fCurrentProject;

	public OrganizeManifestsProcessor(ArrayList projects) {
		fProjectList = projects;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		for (Iterator i = fProjectList.iterator(); i.hasNext();) {
			if (!(i.next() instanceof IProject))
				status.addFatalError(PDEUIMessages.OrganizeManifestsProcessor_invalidParam);
		}
		return status;
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange(""); //$NON-NLS-1$
		change.markAsSynthetic();
		pm.beginTask(PDEUIMessages.OrganizeManifestJob_taskName, fProjectList.size());
		for (Iterator i = fProjectList.iterator(); i.hasNext() && !pm.isCanceled();) {
			CompositeChange projectChange = cleanProject((IProject) i.next(), new SubProgressMonitor(pm, 1));
			if (projectChange.getChildren().length > 0)
				change.add(projectChange);
		}
		return change;
	}

	private CompositeChange cleanProject(IProject project, IProgressMonitor monitor) {
		fCurrentProject = project;
		CompositeChange change = new CompositeChange(NLS.bind(PDEUIMessages.OrganizeManifestsProcessor_rootMessage, new String[] {fCurrentProject.getName()}));
		monitor.beginTask(NLS.bind(PDEUIMessages.OrganizeManifestsProcessor_rootMessage, new String[] {fCurrentProject.getName()}), getTotalTicksPerProject());

		final Change[] result = {null, null};
		final Exception[] ee = new Exception[1];
		ModelModification modification = new ModelModification(fCurrentProject) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase)
					try {
						runCleanup(monitor, (IBundlePluginModelBase) model, result);
					} catch (InvocationTargetException e) {
						ee[0] = e;
					} catch (InterruptedException e) {
						ee[0] = e;
					}
			}
		};
		Change[] changes = PDEModelUtility.changesForModelModication(modification, monitor);
		for (int i = 0; i < changes.length; i++)
			change.add(changes[i]);
		if (result[0] != null)
			change.add(result[0]);
		if (result[1] != null)
			change.add(result[1]);
		if (ee[0] != null)
			PDEPlugin.log(ee[0]);
		return change;
	}

	private void runCleanup(IProgressMonitor monitor, IBundlePluginModelBase modelBase, Change[] result) throws InvocationTargetException, InterruptedException {

		IBundle currentBundle = modelBase.getBundleModel().getBundle();
		ISharedExtensionsModel sharedExtensionsModel = modelBase.getExtensionsModel();
		IPluginModelBase currentExtensionsModel = null;
		if (sharedExtensionsModel instanceof IPluginModelBase)
			currentExtensionsModel = (IPluginModelBase) sharedExtensionsModel;

		String projectName = fCurrentProject.getName();

		if (fAddMissing || fRemoveUnresolved) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_export, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.organizeExportPackages(currentBundle, fCurrentProject, fAddMissing, fRemoveUnresolved);
			if (fAddMissing)
				monitor.worked(1);
			if (fRemoveUnresolved)
				monitor.worked(1);
		}

		if (fMarkInternal) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_filterInternal, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.markPackagesInternal(currentBundle, fPackageFilter);
			monitor.worked(1);
		}

		if (fModifyDep) {
			String message = fRemoveDependencies ? NLS.bind(PDEUIMessages.OrganizeManifestsOperation_removeUnresolved, projectName) : NLS.bind(PDEUIMessages.OrganizeManifestsOperation_markOptionalUnresolved, projectName);
			monitor.subTask(message);
			if (!monitor.isCanceled())
				OrganizeManifest.organizeImportPackages(currentBundle, fRemoveDependencies);
			monitor.worked(1);

			if (!monitor.isCanceled())
				OrganizeManifest.organizeRequireBundles(currentBundle, fRemoveDependencies);
			monitor.worked(1);
		}

		if (fCalculateUses) {
			// we don't set the subTask because it is done in the CalculateUsesOperation, for each package it scans
			if (!monitor.isCanceled()) {
				CalculateUsesOperation op = new CalculateUsesOperation(fCurrentProject, modelBase);
				op.run(new SubProgressMonitor(monitor, 2));
			}
		}

		if (fAddDependencies) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_additionalDeps, projectName));
			if (!monitor.isCanceled()) {
				AddNewDependenciesOperation op = new AddNewDependenciesOperation(fCurrentProject, modelBase);
				op.run(new SubProgressMonitor(monitor, 4));
			}
		}

		if (fUnusedDependencies) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedDeps, projectName));
			if (!monitor.isCanceled()) {
				SubProgressMonitor submon = new SubProgressMonitor(monitor, 4);
				GatherUnusedDependenciesOperation udo = new GatherUnusedDependenciesOperation(modelBase);
				udo.run(submon);
				GatherUnusedDependenciesOperation.removeDependencies(modelBase, udo.getList().toArray());
				submon.done();
			}
		}

		if (fRemoveLazy) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_lazyStart, fCurrentProject.getName()));
			if (!monitor.isCanceled())
				OrganizeManifest.removeUnneededLazyStart(currentBundle);
			monitor.worked(1);
		}

		if (fRemoveUselessFiles) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_uselessPluginFile, fCurrentProject.getName()));
			if (!monitor.isCanceled()) {
				result[1] = OrganizeManifest.deleteUselessPluginFile(fCurrentProject, currentExtensionsModel);
			}
			monitor.worked(1);
		}

		if (fPrefixIconNL) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_nlIconPath, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.prefixIconPaths(currentExtensionsModel);
			monitor.worked(1);
		}

		if (fUnusedKeys) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedKeys, projectName));
			if (!monitor.isCanceled()) {
				Change[] results = OrganizeManifest.removeUnusedKeys(fCurrentProject, currentBundle, currentExtensionsModel);
				if (results.length > 0)
					result[0] = results[0];
			}
			monitor.worked(1);
		}
	}

	public Object[] getElements() {
		return fProjectList.toArray();
	}

	public String getIdentifier() {
		return getClass().getName();
	}

	public String getProcessorName() {
		return PDEUIMessages.OrganizeManifestsWizardPage_title;
	}

	public boolean isApplicable() throws CoreException {
		return true;
	}

	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	private int getTotalTicksPerProject() {
		int ticks = 0;
		if (fAddMissing)
			ticks += 1;
		if (fMarkInternal)
			ticks += 1;
		if (fRemoveUnresolved)
			ticks += 1;
		if (fCalculateUses)
			ticks += 4;
		if (fModifyDep)
			ticks += 2;
		if (fUnusedDependencies)
			ticks += 4;
		if (fAddDependencies)
			ticks += 4;
		if (fRemoveLazy)
			ticks += 1;
		if (fRemoveUselessFiles)
			ticks += 1;
		if (fPrefixIconNL)
			ticks += 1;
		if (fUnusedKeys)
			ticks += 1;
		return ticks;
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
