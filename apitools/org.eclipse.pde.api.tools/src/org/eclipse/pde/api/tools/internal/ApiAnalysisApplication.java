/*******************************************************************************
 * Copyright (c) 2019, 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import static java.util.function.Predicate.not;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.api.tools.internal.model.ApiBaseline;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

public class ApiAnalysisApplication implements IApplication {

	private static class Request {
		private static final String FAIL_ON_ERROR_ARG = "failOnError"; //$NON-NLS-1$
		private static final String PROJECT_ARG = "project"; //$NON-NLS-1$
		private static final String BASELINE_ARG = "baseline"; //$NON-NLS-1$
		private static final String BASELINE_DEFAULT_VALUE = "default"; //$NON-NLS-1$
		private static final String BASELINE_REPO_URI_ARG = "baselineRepositoryURI"; //$NON-NLS-1$
		private static final String DEPENDENCY_LIST_ARG = "dependencyList"; //$NON-NLS-1$

		static Request readApplicationArguments(IApplicationContext context) throws URISyntaxException {
			String[] params = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
			Request res = new Request();
			String currentKey = null;
			for (String param : params) {
				if (param.charAt(0) == '-') {
					if (FAIL_ON_ERROR_ARG.equals(currentKey)) {
						res.failOnError = true;
					}
					currentKey = param.substring(1);
				} else if (PROJECT_ARG.equals(currentKey)) {
					res.project = new File(param).getAbsoluteFile();
				} else if (BASELINE_ARG.equals(currentKey) && !BASELINE_DEFAULT_VALUE.equals(param)) {
					res.baselinePath = new File(param).getAbsoluteFile();
				} else if (BASELINE_REPO_URI_ARG.equals(currentKey)) {
					res.baselineRepoURI = URIUtil.fromString(param);
				} else if (FAIL_ON_ERROR_ARG.equals(currentKey)) {
					res.failOnError = Boolean.parseBoolean(param);
				} else if (DEPENDENCY_LIST_ARG.equals(currentKey)) {
					res.tpFile = new File(param).getAbsoluteFile();
				}
			}
			if (FAIL_ON_ERROR_ARG.equals(currentKey)) {
				res.failOnError = true;
			}
			return res;
		}

		private File project;
		private File baselinePath;
		private URI baselineRepoURI;
		private boolean failOnError;
		private File tpFile;
	}

	private ICoreRunnable restoreOriginalProjectState = null;

	@Override
	public Object start(IApplicationContext context) throws Exception {
		restoreOriginalProjectState = null;
		try {
			IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
			desc.setAutoBuilding(false);
			ResourcesPlugin.getWorkspace().setDescription(desc);
			PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.DISABLE_API_ANALYSIS_BUILDER, false);
			PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.RUN_API_ANALYSIS_AS_JOB, false);

			Request args = Request.readApplicationArguments(context);
			IProject project = importProject(args.project);
			if (project == null) {
				printError("Project not loaded."); //$NON-NLS-1$
				return IStatus.ERROR;
			}
			IApiBaseline baseline = setBaseline(args.baselinePath, args.baselineRepoURI, project);
			if (baseline == null) {
				printError("Baseline shouldn't be null."); //$NON-NLS-1$
				return IStatus.ERROR;
			}
			setTargetPlatform(args.tpFile);
			configureSeverity(project);

			return analyseAPI(project, args.failOnError);
		} catch (CoreException e) {
			printError(e.getStatus());
			return IStatus.ERROR;
		} catch (Exception e) {
			e.printStackTrace();
			return IStatus.ERROR;
		} finally {
			if (restoreOriginalProjectState != null) {
				restoreOriginalProjectState.run(new NullProgressMonitor());
			}
		}
	}

	private static final Predicate<IMarker> IS_ERROR = m -> m.getAttribute(IMarker.SEVERITY,
			-1) == IMarker.SEVERITY_ERROR;
	private static final Predicate<IMarker> IS_WARNING = m -> m.getAttribute(IMarker.SEVERITY,
			-1) == IMarker.SEVERITY_WARNING;
	private static final Predicate<IMarker> IS_API_PROBLEM = marker -> {
		try {
			return marker.getType().startsWith(ApiPlugin.PLUGIN_ID);
		} catch (CoreException e) {
			ApiPlugin.log(e);
			return false;
		}
	};

	private int analyseAPI(IProject project, boolean failOnError) throws CoreException {
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		List<IMarker> allProblems = List.of(project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));

		List<IMarker> allAPIProblems = filter(allProblems, IS_API_PROBLEM);
		List<IMarker> allNonAPIErrors = filter(allProblems, IS_ERROR.and(not(IS_API_PROBLEM)));
		if (!allNonAPIErrors.isEmpty()) {
			printError("Some blocking (most likely link/compilation) errors are present:"); //$NON-NLS-1$
			for (IMarker marker : allNonAPIErrors) {
				printMarker(marker, "FATAL", ApiAnalysisApplication::printError); //$NON-NLS-1$
			}
			printError("Some blocking (most likely link/compilation) errors are present ^^^"); //$NON-NLS-1$
			return 10;
		}
		// errors
		List<IMarker> apiErrors = filter(allAPIProblems, IS_ERROR);
		printError(apiErrors.size() + " API ERRORS"); //$NON-NLS-1$
		for (IMarker marker : apiErrors) {
			printMarker(marker, "API ERROR", ApiAnalysisApplication::printError); //$NON-NLS-1$
		}
		// warnings
		List<IMarker> apiWarnings = filter(allAPIProblems, IS_WARNING);
		print(apiWarnings.size() + " API warnings"); //$NON-NLS-1$
		for (IMarker marker : apiWarnings) {
			printMarker(marker, "API WARNING", ApiAnalysisApplication::print); //$NON-NLS-1$
		}
		return !failOnError || apiErrors.isEmpty() ? EXIT_OK : IStatus.ERROR;
	}

	private static <E> List<E> filter(List<E> list, Predicate<E> filter) {
		return list.stream().filter(filter).collect(Collectors.toList());
	}

	private static void printMarker(IMarker marker, String type, Consumer<String> printer) {
		String path = getFullPath(marker);
		String file = marker.getResource().getName();
		int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);
		String message = marker.getAttribute(IMarker.MESSAGE, "").trim(); //$NON-NLS-1$
		String description = marker.getAttribute("description", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
		if (!description.isEmpty()) {
			message = String.format("%s %s", message, description); //$NON-NLS-1$
		}
		printer.accept(String.format("[%s] File %s at line %d: %s (location: %s)", type, file, lineNumber, //$NON-NLS-1$
				message, path));
	}

	private static String getFullPath(IMarker marker) {
		IResource resource = marker.getResource();
		IPath location = resource.getLocation();
		if (location != null) {
			File file = location.toFile();
			if (file != null) {
				return file.getAbsolutePath();
			}
		}
		return resource.getFullPath().toString();
	}

	private void setTargetPlatform(File dependencyList) throws IOException, CoreException, InterruptedException {
		if (dependencyList != null) {
			if (!(dependencyList.isFile() && dependencyList.canRead())) {
				throw new IllegalArgumentException(
						"dependencyList argument points to non readable file: " + dependencyList);//$NON-NLS-1$
			}
			ITargetPlatformService service = TargetPlatformService.getDefault();
			ITargetDefinition target = service.newTarget();
			target.setName("buildpath"); //$NON-NLS-1$
			TargetBundle[] bundles = new BundleJarFiles(dependencyList).list().stream()//
					.map(absoluteFile -> {
						try {
							return new TargetBundle(absoluteFile);
						} catch (CoreException e) {
							ApiPlugin.log(e);
							return null;
						}
					}).filter(Objects::nonNull).toArray(TargetBundle[]::new);
			target.setTargetLocations(new ITargetLocation[] { new BundleListTargetLocation(bundles) });
			service.saveTargetDefinition(target);
			Job job = new LoadTargetDefinitionJob(target);
			job.schedule();
			job.join();
		}
	}

	protected void configureSeverity(IProject project) {
		Map<String, String> enforcedSeverities = new HashMap<>();
		enforcedSeverities.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE,
				ApiPlugin.VALUE_ERROR);
		enforcedSeverities.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE,
				ApiPlugin.VALUE_ERROR);
		IEclipsePreferences projectNode = new ProjectScope(project).getNode(ApiPlugin.PLUGIN_ID);
		enforcedSeverities.forEach((key, value) -> {
			PDECore.getDefault().getPreferencesManager().setValue(key, value);
			projectNode.put(key, value);
		});
	}

	private static final String LATEST_VERSION = "0.0.0"; //$NON-NLS-1$

	private IApiBaseline setBaseline(File baselinePath, URI baselineRepoURI, IProject project) throws CoreException {
		ApiBaseline baseline = new ApiBaseline("pluginAPICheckBaseline"); //$NON-NLS-1$
		if (baselinePath == null && baselineRepoURI == null) {
			for (Bundle bundle : ApiPlugin.getDefault().getBundle().getBundleContext().getBundles()) {
				if (bundle.getBundleId() != Constants.SYSTEM_BUNDLE_ID) {
					File bundleFile = FileLocator.getBundleFileLocation(bundle).orElseThrow();
					addBundleToBaseline(baseline, bundleFile, bundle.getBundleId());
				}
			}

		} else {
			ITargetPlatformService service = TargetPlatformService.getDefault();
			ITargetDefinition target;

			if (baselineRepoURI != null) {
				String[] unitIds = new String[] { project.getName() };
				String[] versions = new String[] { LATEST_VERSION };
				URI[] repositories = new URI[] { baselineRepoURI };
				int resolutionFlags = IUBundleContainer.INCLUDE_REQUIRED; // all others not included
				ITargetLocation location = service.newIULocation(unitIds, versions, repositories, resolutionFlags);

				target = service.newTarget();
				target.setTargetLocations(new ITargetLocation[] { location });

			} else if (baselinePath.isFile() && baselinePath.getName().endsWith(".target")) { //$NON-NLS-1$
				target = service.getTarget(baselinePath.toURI()).getTargetDefinition();

			} else if (baselinePath.isDirectory()) {

				ITargetLocation location = service.newDirectoryLocation(baselinePath.getAbsolutePath());

				target = service.newTarget();
				target.setTargetLocations(new ITargetLocation[] { location });

			} else {
				return ApiBaselineManager.getManager().getDefaultApiBaseline();
			}
			addAllTargetBundlesToBaseline(target, baseline);
		}

		ApiBaselineManager.getManager().addApiBaseline(baseline);
		ApiBaselineManager.getManager().setDefaultApiBaseline(baseline.getName());
		return baseline;
	}

	private void addAllTargetBundlesToBaseline(ITargetDefinition target, ApiBaseline baseline) throws CoreException {
		IStatus resolutionStatus = target.resolve(new NullProgressMonitor());
		if (resolutionStatus.matches(IStatus.WARNING)) {
			print("WARNING while resolving target platform: " + resolutionStatus.getMessage()); //$NON-NLS-1$
		} else if (resolutionStatus.matches(IStatus.ERROR)) {
			throw new CoreException(resolutionStatus);
		}
		for (TargetBundle bundle : target.getAllBundles()) {
			BundleInfo bundleInfo = bundle.getBundleInfo();
			if (bundleInfo.getBundleId() != Constants.SYSTEM_BUNDLE_ID) {
				addBundleToBaseline(baseline, new File(bundleInfo.getLocation()), bundleInfo.getBundleId());
			}
		}
	}

	private void addBundleToBaseline(ApiBaseline baseline, File bundleFile, long bundleId) throws CoreException {
		BundleComponent component = new BundleComponent(baseline, bundleFile.getAbsolutePath(), bundleId);
		baseline.addApiComponents(new IApiComponent[] { component });
	}

	private IProject importProject(File projectPath) throws CoreException, IOException {
		File dotProject = new File(projectPath, IProjectDescription.DESCRIPTION_FILE_NAME);
		if (!dotProject.isFile()) {
			printError("Expected `" + dotProject + "` file doesn't exist."); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		IWorkspace ws = ResourcesPlugin.getWorkspace();
		IProjectDescription projectDescription = ws.loadProjectDescription(Path.fromOSString(dotProject.getPath()));
		projectDescription.setLocation(Path.fromOSString(projectPath.getPath()));
		IProject project = ws.getRoot().getProject(projectDescription.getName());

		ICoreRunnable projectRemover;
		if (project.exists()) {
			projectRemover = !project.isOpen() ? project::close : m -> {
				/* do nothing */ };
			project.open(new NullProgressMonitor());
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

			if (!project.getDescription().getLocationURI().equals(projectDescription.getLocationURI())) {
				printError("Project with same name and different location exists in workspace."); //$NON-NLS-1$
				return null;
			}
		} else {
			projectRemover = m -> project.delete(IResource.NEVER_DELETE_PROJECT_CONTENT | IResource.FORCE, m);
			project.create(projectDescription, new NullProgressMonitor());
			project.open(new NullProgressMonitor());
		}

		projectDescription = project.getDescription();
		ICommand[] buildSpec = projectDescription.getBuildSpec();

		if (Arrays.stream(buildSpec).map(ICommand::getBuilderName).noneMatch(ApiPlugin.BUILDER_ID::equals)) {

			ICommand apiAnalysisBuilderCommand = projectDescription.newCommand();
			apiAnalysisBuilderCommand.setBuilderName(ApiPlugin.BUILDER_ID);

			ICommand[] builders = new ICommand[buildSpec.length + 1];
			System.arraycopy(buildSpec, 0, builders, 0, buildSpec.length);
			builders[builders.length - 1] = apiAnalysisBuilderCommand;
			buildSpec = builders;
		}

		ICommand[] newBuilders = removeManifestAndSchemaBuilders(buildSpec);

		if (!Arrays.equals(newBuilders, projectDescription.getBuildSpec())) {

			IFile projectFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
			byte[] originalContent; // save the raw byte-content to avoid encoding and formatting issues
			try (InputStream contentStream = projectFile.getContents()) {
				originalContent = contentStream.readAllBytes();
			}

			projectDescription.setBuildSpec(newBuilders);
			project.setDescription(projectDescription, IResource.NONE, new NullProgressMonitor());

			restoreOriginalProjectState = m -> {
				projectFile.setContents(new ByteArrayInputStream(originalContent), IResource.FORCE, m);
				projectRemover.run(m);
			};
		} else {
			restoreOriginalProjectState = projectRemover;
		}
		return project;
	}

	private static ICommand[] removeManifestAndSchemaBuilders(ICommand[] buildSpec) {
		// remove manifest and schema builders
		return Arrays.stream(buildSpec).filter(x -> !("org.eclipse.pde.ManifestBuilder".equals(x.getBuilderName()) //$NON-NLS-1$
				|| "org.eclipse.pde.SchemaBuilder".equals(x.getBuilderName())) //$NON-NLS-1$
		).toArray(ICommand[]::new);
	}

	@Override
	public void stop() {
		// Nothing to do
	}

	private static void printError(Object msg) {
		System.err.println(msg);
	}

	private static void print(Object msg) {
		System.out.println(msg);
	}
}
