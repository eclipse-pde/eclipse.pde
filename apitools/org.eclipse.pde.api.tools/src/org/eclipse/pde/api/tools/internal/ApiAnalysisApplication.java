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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.api.tools.internal.model.ApiBaseline;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.osgi.framework.Bundle;

public class ApiAnalysisApplication implements IApplication {

	private static class Request {
		private static final String FAIL_ON_ERROR_ARG = "failOnError"; //$NON-NLS-1$
		private static final String PROJECT_ARG = "project"; //$NON-NLS-1$
		private static final String BASELINE_ARG = "baseline"; //$NON-NLS-1$
		private static final String BASELINE_DEFAULT_VALUE = "default"; //$NON-NLS-1$
		private static final String DEPENDENCY_LIST_ARG = "dependencyList"; //$NON-NLS-1$

		private Request() {
		}

		public static Request readFromArgs(String[] params) {
			Request res = new Request();
			String currentKey = null;
			for (String param : params) {
				if (param.charAt(0) == '-') {
					if (FAIL_ON_ERROR_ARG.equals(currentKey)) {
						res.failOnError = true;
					}
					currentKey = param.substring(1);
				} else if (PROJECT_ARG.equals(currentKey)) {
					res.project = new File(param);
				} else if (BASELINE_ARG.equals(currentKey) && !BASELINE_DEFAULT_VALUE.equals(param)) {
					res.baselinePath = new File(param);
				} else if (FAIL_ON_ERROR_ARG.equals(currentKey)) {
					res.failOnError = Boolean.parseBoolean(param);
				} else if (DEPENDENCY_LIST_ARG.equals(currentKey)) {
					res.tpFile = new File(param);
				}
			}
			if (FAIL_ON_ERROR_ARG.equals(currentKey)) {
				res.failOnError = true;
			}
			return res;
		}

		public File project;
		public File baselinePath;
		public boolean failOnError;
		public File tpFile;
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

			Request args = Request
					.readFromArgs((String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS));
			IProject project = importProject(args.project);
			if (project == null) {
				System.err.println("Project not loaded."); //$NON-NLS-1$
				return IStatus.ERROR;
			}
			IApiBaseline baseline = setBaseline(args.baselinePath);
			if (baseline == null) {
				System.err.println("Baseline shouldn't be null."); //$NON-NLS-1$
				return IStatus.ERROR;
			}
			setTargetPlatform(args.tpFile);

			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
			// wait untill all jobs has finished that might be sceduled as part of the
			// build...
			while (!Job.getJobManager().isIdle()) {
				Thread.yield();
			}
			IMarker[] allProblemMarkers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			Predicate<IMarker> isAPIMarker = marker -> {
				try {
					return marker.getType().startsWith(ApiPlugin.PLUGIN_ID);
				} catch (CoreException e) {
					ApiPlugin.log(e);
					return false;
				}
			};
			IMarker[] allAPIProbleMarkers = Arrays.stream(allProblemMarkers) //
					.filter(isAPIMarker) //
					.toArray(IMarker[]::new);
			IMarker[] allNonAPIErrors = Arrays.stream(allProblemMarkers) //
					.filter(isAPIMarker.negate()) //
					.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR) //
					.toArray(IMarker[]::new);
			if (allNonAPIErrors.length > 0) {
				System.err.println("Some blocking (most likely link/compilation) errors are present:"); //$NON-NLS-1$
				for (IMarker marker : allNonAPIErrors) {
					printMarker(marker, "FATAL"); //$NON-NLS-1$
				}
				System.err.println("Some blocking (most likely link/compilation) errors are present ^^^"); //$NON-NLS-1$
				return 10;
			}
			// errors
			IMarker[] errorMarkers = Arrays.stream(allAPIProbleMarkers)
					.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
					.toArray(IMarker[]::new);
			System.err.println(errorMarkers.length + " API ERRORS"); //$NON-NLS-1$
			for (IMarker marker : errorMarkers) {
				printMarker(marker, "API ERROR"); //$NON-NLS-1$
			}
			// warnings
			IMarker[] warningMarkers = Arrays.stream(allAPIProbleMarkers)
					.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING)
					.toArray(IMarker[]::new);
			System.out.println(warningMarkers.length + " API warnings"); //$NON-NLS-1$
			for (IMarker marker : warningMarkers) {
				printMarker(marker, "API WARNING"); //$NON-NLS-1$
			}
			// fail
			if (args.failOnError && errorMarkers.length > 0) {
				return IStatus.ERROR;
			}
			return IStatus.OK;
		} catch (CoreException e) {
			System.err.println(e.getStatus());
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

	private static void printMarker(IMarker marker, String type) {
		String path = getFullPath(marker);
		String file = marker.getResource().getName();
		int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);
		String message = marker.getAttribute(IMarker.MESSAGE, "").trim(); //$NON-NLS-1$
		String description = marker.getAttribute("description", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$
		if (!description.isEmpty()) {
			message = String.format("%s %s", message, description); //$NON-NLS-1$
		}
		System.out.println(String.format("[%s] File %s at line %d: %s (location: %s)", type, file, lineNumber, //$NON-NLS-1$
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
						"dependencyList argument points to non readable file: " + dependencyList.getAbsolutePath());//$NON-NLS-1$
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
					}).filter(Objects::nonNull)//
					.toArray(TargetBundle[]::new);
			target.setTargetLocations(new ITargetLocation[] { new BundleListTargetLocation(bundles) });
			service.saveTargetDefinition(target);
			Job job = new LoadTargetDefinitionJob(target);
			job.schedule();
			job.join();
		}
	}

	private IApiBaseline setBaseline(File baselinePath) throws CoreException {
		if (baselinePath == null) {
			ApiBaseline baseline = new ApiBaseline("current running application"); //$NON-NLS-1$
			for (Bundle bundle : ApiPlugin.getDefault().getBundle().getBundleContext().getBundles()) {
				if (bundle.getBundleId() != 0) {
					String bundleFile = FileLocator.getBundleFileLocation(bundle).orElseThrow().getAbsolutePath();
					baseline.addApiComponents(
							new IApiComponent[] { new BundleComponent(baseline, bundleFile, bundle.getBundleId()) });
				}
			}
			ApiBaselineManager.getManager().addApiBaseline(baseline);
			ApiBaselineManager.getManager().setDefaultApiBaseline(baseline.getName());
			return baseline;
		}
		if (!baselinePath.exists()) {
			System.err
					.println(String.format("Specified baseline %s does not denote a file or directory!", baselinePath)); //$NON-NLS-1$
			return null;
		}
		String baselineFileName = baselinePath.getName();
		if (baselinePath.isFile() && baselineFileName.endsWith(".txt")) { //$NON-NLS-1$
			try {
				String baselineName = baselineFileName.substring(0, baselineFileName.lastIndexOf('.'));
				ApiBaseline baseline = new ApiBaseline(baselineName);
				long bundleId = 1;
				for (String bundleFile : Files.readAllLines(baselinePath.toPath())) {
					baseline.addApiComponents(
							new IApiComponent[] { new BundleComponent(baseline, bundleFile, bundleId++) });
				}
				ApiBaselineManager.getManager().addApiBaseline(baseline);
				ApiBaselineManager.getManager().setDefaultApiBaseline(baseline.getName());
				return baseline;
			} catch (IOException e) {
				throw new CoreException(Status.error("Reading file failed!", e)); //$NON-NLS-1$
			}
		} else if (baselinePath.isFile() && baselineFileName.endsWith(".target")) { //$NON-NLS-1$
			ITargetPlatformService service = TargetPlatformService.getDefault();
			ITargetDefinition definition = service.getTarget(baselinePath.toURI()).getTargetDefinition();
			IStatus resolutionStatus = definition.resolve(new NullProgressMonitor());
			switch (resolutionStatus.getSeverity())
				{
				case IStatus.WARNING ->
					System.out.println("WARNING resolving target platform: " + resolutionStatus.getMessage()); //$NON-NLS-1$
				case IStatus.ERROR ->
					throw new CoreException(resolutionStatus);
				default -> { /*Nothing*/ }
				}
			// remove ".target"
			String baselineName = baselineFileName.substring(0, baselineFileName.lastIndexOf('.'));
			ApiBaseline baseline = new ApiBaseline(baselineName);
			for (TargetBundle bundle : definition.getAllBundles()) {
				BundleInfo bundleInfo = bundle.getBundleInfo();
				if (bundleInfo.getBundleId() != 0) {
					baseline.addApiComponents(new IApiComponent[] { new BundleComponent(baseline,
							new File(bundleInfo.getLocation()).getAbsolutePath(), bundleInfo.getBundleId()) });
				}
			}
			ApiBaselineManager.getManager().addApiBaseline(baseline);
			ApiBaselineManager.getManager().setDefaultApiBaseline(baseline.getName());
			return baseline;
		} else if (baselinePath.isDirectory()) {
			System.err.println(
					"Support for directories not implemented yet, use `default` or a `</path/to/baseline.target>` baseline for currently running application."); //$NON-NLS-1$
			return null;
		}
		System.err.println(String.format("Unsupported file type %s!", baselineFileName)); //$NON-NLS-1$
		return null;
	}

	private IProject importProject(File projectPath) throws CoreException, IOException {
		File dotProject = new File(projectPath, IProjectDescription.DESCRIPTION_FILE_NAME);
		if (!dotProject.isFile()) {
			System.err.println("Expected `" + dotProject.getAbsolutePath() + "` file doesn't exist."); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		IProjectDescription projectDescription = ResourcesPlugin.getWorkspace()
				.loadProjectDescription(IPath.fromOSString(dotProject.getAbsolutePath()));
		projectDescription.setLocation(IPath.fromOSString(projectPath.getAbsolutePath()));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectDescription.getName());

		ICoreRunnable projectRemover;
		if (project.exists()) {
			projectRemover = !project.isOpen() ? project::close : m -> {
				/* do nothing */ };
			project.open(new NullProgressMonitor());
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

			if (!project.getDescription().getLocationURI().equals(projectDescription.getLocationURI())) {
				System.err.println("Project with same name and different location exists in workspace."); //$NON-NLS-1$
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

}
