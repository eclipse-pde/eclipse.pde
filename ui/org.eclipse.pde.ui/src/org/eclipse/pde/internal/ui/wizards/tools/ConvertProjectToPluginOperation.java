/*******************************************************************************
 * Copyright (c) 2007, 2024 IBM Corporation and others.
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
 *     Code 9 Corporation - ongoing enhancements
 *     Les Jones <lesojones@gamil.com> - bug 205361
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.internal.core.ClasspathComputer;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.pde.internal.ui.wizards.plugin.NewProjectCreationOperation;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.osgi.framework.Constants;

/**
 * Operation to convert a normal workspace project into a plug-in project. This
 * code has, in the main, been refactored (copied with little or no amendment)
 * from org.eclipse.pde.internal.ui.wizards.tool.ConvertedProjectsPage.
 */
public class ConvertProjectToPluginOperation extends WorkspaceModifyOperation {

	private final IProject[] projectsToConvert;
	private final boolean enableApiAnalysis;

	private String fLibraryName;
	private String[] fSrcEntries;
	private String[] fLibEntries;
	private final static String NO_EXECUTION_ENVIRONMENT = PDEUIMessages.PluginContentPage_noEE;

	/**
	 * Workspace operation to convert the specified project into a plug-in
	 * project.
	 *
	 * @param theProjectsToConvert The project to be converted.
	 * @param enableApiAnalysis Whether to set up API Tooling analysis on the projects
	 */
	public ConvertProjectToPluginOperation(IProject[] theProjectsToConvert, boolean enableApiAnalysis) {
		this.projectsToConvert = theProjectsToConvert;
		this.enableApiAnalysis = enableApiAnalysis;
	}

	/**
	 * Convert a normal java project into a plug-in project.
	 *
	 * @param monitor
	 *            Progress monitor
	 */
	@Override
	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {

		try {
			monitor.beginTask(PDEUIMessages.ConvertedProjectWizard_converting, projectsToConvert.length);

			for (IProject projectToConvert : projectsToConvert) {
				convertProject(projectToConvert, monitor);
				monitor.worked(1);
			}

		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} finally {
			monitor.done();
		}
	}

	private void convertProject(IProject projectToConvert, IProgressMonitor monitor) throws CoreException {

		// Do early checks to make sure we can get out fast if we're not setup
		// properly
		if (projectToConvert == null || !projectToConvert.exists()) {
			return;
		}

		// Nature check - do we need to do anything at all?
		if (PluginProject.isPluginProject(projectToConvert)) {
			return;
		}

		CoreUtility.addNatureToProject(projectToConvert, PluginProject.NATURE, monitor);
		// Setup API Tooling, which requires the java nature
		if (enableApiAnalysis) {
			if (!projectToConvert.hasNature(JavaCore.NATURE_ID)) {
				CoreUtility.addNatureToProject(projectToConvert, JavaCore.NATURE_ID, monitor);
			}
			CoreUtility.addNatureToProject(projectToConvert, "org.eclipse.pde.api.tools.apiAnalysisNature", monitor); //$NON-NLS-1$
		}

		loadClasspathEntries(projectToConvert, monitor);
		loadLibraryName(projectToConvert);

		createManifestFile(PDEProject.getManifest(projectToConvert), monitor);

		IFile buildFile = PDEProject.getBuildProperties(projectToConvert);
		if (!buildFile.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildFile);
			IBuild build = model.getBuild(true);
			IBuildEntry entry = model.getFactory().createEntry(IBuildEntry.BIN_INCLUDES);
			if (PDEProject.getPluginXml(projectToConvert).exists()) {
				entry.addToken(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			}
			if (PDEProject.getManifest(projectToConvert).exists()) {
				entry.addToken(ICoreConstants.MANIFEST_FOLDER_NAME);
			}
			for (String libEntry : fLibEntries) {
				entry.addToken(libEntry);
			}

			if (fSrcEntries.length > 0) {
				entry.addToken(fLibraryName);
				IBuildEntry source = model.getFactory().createEntry(IBuildEntry.JAR_PREFIX + fLibraryName);
				for (String srcEntry : fSrcEntries) {
					source.addToken(srcEntry);
				}
				build.add(source);
			}
			if (entry.getTokens().length > 0) {
				build.add(entry);
			}

			model.save();
		}
	}

	private void loadClasspathEntries(IProject project, IProgressMonitor monitor) {
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] currentClassPath = new IClasspathEntry[0];
		ArrayList<String> sources = new ArrayList<>();
		ArrayList<String> libraries = new ArrayList<>();
		try {
			currentClassPath = javaProject.getRawClasspath();
		} catch (JavaModelException e) {
		}
		for (IClasspathEntry element : currentClassPath) {
			int contentType = element.getEntryKind();
			if (contentType == IClasspathEntry.CPE_SOURCE) {
				String relativePath = getRelativePath(element, project);
				if (relativePath.equals("")) { //$NON-NLS-1$
					sources.add("."); //$NON-NLS-1$
				} else {
					sources.add(relativePath + "/"); //$NON-NLS-1$
				}
			} else if (contentType == IClasspathEntry.CPE_LIBRARY) {
				String path = getRelativePath(element, project);
				if (path.length() > 0) {
					libraries.add(path);
				} else {
					libraries.add("."); //$NON-NLS-1$
				}
			}
		}
		fSrcEntries = sources.toArray(new String[sources.size()]);
		fLibEntries = libraries.toArray(new String[libraries.size()]);

		IClasspathEntry[] classPath = new IClasspathEntry[currentClassPath.length + 1];
		System.arraycopy(currentClassPath, 0, classPath, 0, currentClassPath.length);
		classPath[classPath.length - 1] = ClasspathComputer.createContainerEntry();
		try {
			javaProject.setRawClasspath(classPath, monitor);
		} catch (JavaModelException e) {
		}
	}

	private String getRelativePath(IClasspathEntry cpe, IProject project) {
		IPath path = project.getFile(cpe.getPath()).getProjectRelativePath();
		return path.removeFirstSegments(1).toString();
	}

	private void loadLibraryName(IProject project) {
		if (isOldTarget() || (fLibEntries.length > 0 && fSrcEntries.length > 0)) {
			String libName = project.getName();
			int i = libName.lastIndexOf("."); //$NON-NLS-1$
			if (i != -1) {
				libName = libName.substring(i + 1);
			}
			fLibraryName = libName + ".jar"; //$NON-NLS-1$
		} else {
			fLibraryName = "."; //$NON-NLS-1$
		}
	}

	private void organizeExports(final IProject project) {
		PDEModelUtility.modifyModel(new ModelModification(PDEProject.getManifest(project)) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IBundlePluginModelBase)) {
					return;
				}
				OrganizeManifest.organizeExportPackages(((IBundlePluginModelBase) model).getBundleModel().getBundle(), project, true, true);
			}
		}, null);
	}

	private String createInitialName(String id) {
		int loc = id.lastIndexOf('.');
		if (loc == -1) {
			return id;
		}
		StringBuilder buf = new StringBuilder(id.substring(loc + 1));
		buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
		return buf.toString();
	}

	private void createManifestFile(IFile file, IProgressMonitor monitor) throws CoreException {
		WorkspaceBundlePluginModel model = new WorkspaceBundlePluginModel(file, null);
		model.load();
		IBundle pluginBundle = model.getBundleModel().getBundle();

		String pluginId = pluginBundle.getHeader(Constants.BUNDLE_SYMBOLICNAME);
		String pluginName = pluginBundle.getHeader(Constants.BUNDLE_NAME);
		String pluginVersion = pluginBundle.getHeader(Constants.BUNDLE_VERSION);

		boolean missingInfo = (pluginId == null || pluginName == null || pluginVersion == null);

		// If no ID exists, create one
		if (pluginId == null) {
			pluginId = IdUtil.getValidId(file.getProject().getName());
		}
		// At this point, the plug-in ID is not null

		// If no version number exists, create one
		if (pluginVersion == null) {
			pluginVersion = "1.0.0.qualifier"; //$NON-NLS-1$
		}

		// If no name exists, create one using the non-null pluginID
		if (pluginName == null) {
			pluginName = createInitialName(pluginId);
		}

		pluginBundle.setHeader(Constants.BUNDLE_SYMBOLICNAME, pluginId);
		pluginBundle.setHeader(Constants.BUNDLE_VERSION, pluginVersion);
		pluginBundle.setHeader(Constants.BUNDLE_NAME, pluginName);
		IJavaProject jp = JavaCore.create(file.getProject());
		IModuleDescription moduleDescription = null;
		if (jp != null) {
			try {
				moduleDescription = jp.getModuleDescription();
			} catch (JavaModelException e) {
			}
		}
		if (moduleDescription == null) {
			pluginBundle.setHeader(ICoreConstants.AUTOMATIC_MODULE_NAME,
					NewProjectCreationOperation.determineAutomaticModuleNameFromBSN(pluginId));
		}
		IExecutionEnvironment[] exeEnvs = VMUtil.getExecutionEnvironments();
		IExecutionEnvironment ee = null;
		IVMInstall defaultVM = JavaRuntime.getDefaultVMInstall();
		for (int i = 0; i < exeEnvs.length; i++) {
			if (!(exeEnvs[i].getId().equals(NO_EXECUTION_ENVIRONMENT))) {
				if (VMUtil.getExecutionEnvironment(exeEnvs[i].getId()).isStrictlyCompatible(defaultVM)) {
					ee = exeEnvs[i];
					break;
				}
			}
		}
		if (ee == null) {
			for (int i = exeEnvs.length - 1; i >= 0; i--) {
				if (!(exeEnvs[i].getId().equals(NO_EXECUTION_ENVIRONMENT))) {
					IVMInstall[] vm = VMUtil.getExecutionEnvironment(exeEnvs[i].getId()).getCompatibleVMs();
					if (vm == null || vm.length == 0) {
						continue;
					}
					java.util.List<IVMInstall> vmList = Arrays.asList(vm);
					if (vmList.contains(defaultVM)) {
						ee = exeEnvs[i];
						break;
					}
				}
			}
		}
		if (ee != null) {
			pluginBundle.setHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, ee.getId());
		}

		if (missingInfo) {
			IPluginModelFactory factory = model.getPluginFactory();
			IPluginBase base = model.getPluginBase();
			if (fLibraryName != null && !fLibraryName.equals(".")) { //$NON-NLS-1$
				IPluginLibrary library = factory.createLibrary();
				library.setName(fLibraryName);
				library.setExported(true);
				base.add(library);
			}
			for (String libEntry : fLibEntries) {
				IPluginLibrary library = factory.createLibrary();
				library.setName(libEntry);
				library.setExported(true);
				base.add(library);
			}
			if (TargetPlatformHelper.getTargetVersion() >= 3.1) {
				pluginBundle.setHeader(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
			}
		}

		model.save();
		monitor.done();
		organizeExports(file.getProject());
	}

	private boolean isOldTarget() {
		return TargetPlatformHelper.getTargetVersion() < 3.1;
	}

}
