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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.natures.BndProject;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.tools.change.BndProjectUpdateChange;
import org.eclipse.pde.internal.ui.wizards.tools.change.BuildToBndChange;
import org.eclipse.pde.internal.ui.wizards.tools.change.CreatePackageInfoChange;
import org.eclipse.pde.internal.ui.wizards.tools.change.ManifestToBndChange;
import org.eclipse.pde.internal.ui.wizards.tools.change.PreferenceChange;
import org.osgi.framework.Version;

public class ConvertAutomaticManifestProcessor extends RefactoringProcessor {

	private List<IProject> projects;
	private boolean useProjectRoot;
	private boolean keepRequireBundle;
	private boolean keepImportPackage;
	private boolean keepBREE;
	private boolean keepExportPackage;

	public ConvertAutomaticManifestProcessor(List<IProject> projects) {
		this.projects = projects;
	}

	@Override
	public Object[] getElements() {
		return projects.toArray();
	}

	@Override
	public String getIdentifier() {
		return getClass().getName();
	}

	@Override
	public String getProcessorName() {
		return PDEUIMessages.ConvertAutomaticManifestWizardPage_title;
	}

	@Override
	public boolean isApplicable() {
		return true;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		CompositeChange change = new CompositeChange(PDEUIMessages.ConvertAutomaticManifestWizardPage_title);
		change.markAsSynthetic();
		SubMonitor subMonitor = SubMonitor.convert(pm, PDEUIMessages.ConvertAutomaticManifestJob_taskName,
				projects.size());
		for (IProject project : projects) {
			change.add(convertProject(project, subMonitor.split(1)));
		}
		return change;
	}

	private Change convertProject(IProject project, IProgressMonitor monitor) throws CoreException {
		CompositeChange change = new CompositeChange(
				NLS.bind(PDEUIMessages.ConvertAutomaticManifestsProcessor_changeProject, project.getName()));
		SubMonitor.convert(monitor,
				NLS.bind(PDEUIMessages.ConvertAutomaticManifestsProcessor_rootMessage, project.getName()), 100);
		IFile manifest = PDEProject.getManifest(project);
		IPluginModelBase model = PluginRegistry.findModel(project);
		IFile instructionsFile = project.getFile(BndProject.INSTRUCTIONS_FILE);
		IBuildModel buildModel = PluginRegistry.createBuildModel(model);
		change.add(new BndProjectUpdateChange(project));
		change.add(new ManifestToBndChange(project, manifest, model, instructionsFile, keepRequireBundle,
				keepImportPackage, keepBREE, keepExportPackage));
		if (!keepExportPackage) {
			ExportPackageDescription[] exportPackages = model.getBundleDescription().getExportPackages();
			if (exportPackages.length > 0) {
				IJavaProject javaProject = JavaCore.create(project);
				for (ExportPackageDescription exportPackage : exportPackages) {
					Change packageInfoChange = getPackageInfoChange(exportPackage.getName(), exportPackage.getVersion(),
							javaProject);
					if (packageInfoChange != null) {
						change.add(packageInfoChange);
					}
				}
			}
		}
		change.add(new BuildToBndChange(project, buildModel, instructionsFile));
		IFile buildProperties = PDEProject.getBuildProperties(project);
		if (buildProperties.exists()) {
			change.add(new DeleteResourceChange(buildProperties.getFullPath(), true));
		}
		if (useProjectRoot) {
			ProjectScope scope = new ProjectScope(project);
			IEclipsePreferences node = scope.getNode(PDECore.PLUGIN_ID);
			change.add(new PreferenceChange(node));
		} else if (manifest.exists()) {
			change.add(new DeleteResourceChange(manifest.getFullPath(), true));
		}
		return change;
	}

	private Change getPackageInfoChange(String name, Version version, IJavaProject javaProject) throws CoreException {
		IPackageFragment pkg = findPackage(javaProject, name);
		if (pkg == null) {
			return null;
		}
		ICompilationUnit cu = pkg.getCompilationUnit(CreatePackageInfoChange.PACKAGE_INFO_JAVA);
		if (cu.exists()) {
			// TODO we need to create an update change!
			return null;
		}
		return new CreatePackageInfoChange(pkg, name, version);

	}

	private static IPackageFragment findPackage(IJavaProject javaProject, String name) throws JavaModelException {
		for (IPackageFragment pkg : javaProject.getPackageFragments()) {
			if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE && pkg.getElementName().equals(name)) {
				return pkg;
			}
		}
		return null;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
			throws CoreException {
		return new RefactoringParticipant[0];
	}

	public void setUseProjectRoot(boolean useProjectRoot) {
		this.useProjectRoot = useProjectRoot;
	}

	public void setKeepRequireBundle(boolean keepRequireBundle) {
		this.keepRequireBundle = keepRequireBundle;
	}

	public void setKeepImportPackage(boolean keepImportPackage) {
		this.keepImportPackage = keepImportPackage;
	}

	public void setKeepRequiredExecutionEnvironment(boolean keepBREE) {
		this.keepBREE = keepBREE;
	}

	public void setKeepExportPackage(boolean keepExportPackage) {
		this.keepExportPackage = keepExportPackage;
	}

}
