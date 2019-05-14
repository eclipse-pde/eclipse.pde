/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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

	@Override
	public Object start(IApplicationContext context) throws Exception {
		IWorkspaceDescription desc = ResourcesPlugin.getWorkspace().getDescription();
		desc.setAutoBuilding(false);
		ResourcesPlugin.getWorkspace().setDescription(desc);
		PDECore.getDefault().getPreferencesManager().setValue(ICoreConstants.DISABLE_API_ANALYSIS_BUILDER, false);

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
		configureSeverity(project);

		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		IMarker[] allProblemMarkers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		Predicate<IMarker> isAPIMarker = marker -> {
			try {
				return marker.getType().startsWith(ApiPlugin.PLUGIN_ID);
			} catch (CoreException e) {
				ApiPlugin.log(e);
				return false;
			}
		};
		IMarker[] allAPIProbleMarkers = Arrays.stream(allProblemMarkers).filter(isAPIMarker).toArray(IMarker[]::new);
		IMarker[] allNonAPIErrors = Arrays.stream(allProblemMarkers).filter(isAPIMarker.negate())
				.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR)
				.toArray(IMarker[]::new);
		if (allNonAPIErrors.length > 0) {
			System.err.println("Some blocking (most likely link/compilation) errors are present:"); //$NON-NLS-1$
			for (IMarker marker : allNonAPIErrors) {
				System.err.println("* " + marker); //$NON-NLS-1$
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
			System.err.println("* " + marker); //$NON-NLS-1$
		}
		// warnings
		IMarker[] warningMarkers = Arrays.stream(allAPIProbleMarkers)
				.filter(marker -> marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING)
				.toArray(IMarker[]::new);
		System.out.println(warningMarkers.length + " API warnings"); //$NON-NLS-1$
		for (IMarker marker : warningMarkers) {
			System.out.println("* " + marker); //$NON-NLS-1$
		}
		// fail
		if (args.failOnError && errorMarkers.length > 0) {
			return IStatus.ERROR;
		}
		return IStatus.OK;
	}

	private void setTargetPlatform(File dependencyList) throws IOException, CoreException, InterruptedException {
		if (dependencyList != null) {
			// File is typically the output of `mvn dependnecy:list
			// -DoutputAbsoluteArtifactFilename=true -DoutputScope=false -DoutputFile=...`
			// like
			// ```
			// The following files have been resolved:
			// p2.eclipse-plugin:org.eclipse.equinox.event:jar:1.5.0.v20181008-1938:/home/mistria/.m2/repository/p2/osgi/bundle/org.eclipse.equinox.event/1.5.0.v20181008-1938/org.eclipse.equinox.event-1.5.0.v20181008-1938.jar
			// p2.eclipse-plugin:org.eclipse.equinox.p2.core:jar:2.6.0.v20190215-2242:/home/mistria/.m2/repository/p2/osgi/bundle/org.eclipse.equinox.p2.core/2.6.0.v20190215-2242/org.eclipse.equinox.p2.core-2.6.0.v20190215-2242.jar
			// p2.eclipse-plugin:org.eclipse.osgi.compatibility.state:jar:1.1.400.v20190208-1533:/home/mistria/.m2/repository/p2/osgi/bundle/org.eclipse.osgi.compatibility.state/1.1.400.v20190208-1533/org.eclipse.osgi.compatibility.state-1.1.400.v20190208-1533.jar
			// ```
			ITargetPlatformService service = TargetPlatformService.getDefault();
			ITargetDefinition target = service.newTarget();
			target.setName("buildpath"); //$NON-NLS-1$
			TargetBundle[] bundles = Files.readAllLines(dependencyList.toPath()).stream()//
					.filter(line -> line.contains("jar")) //$NON-NLS-1$
					.flatMap(line -> Arrays.stream(line.split(":"))) //$NON-NLS-1$
					.filter(maybePath -> !maybePath.trim().isEmpty())
					.map(File::new)//
					.filter(File::isAbsolute)//
					.filter(File::isFile)//
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

	private IApiBaseline setBaseline(File baselinePath) throws CoreException, IOException {
		if (baselinePath == null) {
			ApiBaseline baseline = new ApiBaseline("current running application"); //$NON-NLS-1$
			for (Bundle bundle : ApiPlugin.getDefault().getBundle().getBundleContext().getBundles()) {
				if (bundle.getBundleId() != 0) {
					baseline.addApiComponents(
							new IApiComponent[] { new BundleComponent(baseline,
									FileLocator.getBundleFile(bundle).getAbsolutePath(), bundle.getBundleId()) });
				}
			}
			ApiBaselineManager.getManager().addApiBaseline(baseline);
			ApiBaselineManager.getManager().setDefaultApiBaseline(baseline.getName());
			return baseline;
		} else if (baselinePath.isFile() && baselinePath.getName().endsWith(".target")) { //$NON-NLS-1$
			ITargetPlatformService service = TargetPlatformService.getDefault();
			ITargetDefinition definition = service.getTarget(baselinePath.toURI()).getTargetDefinition();
			definition.resolve(new NullProgressMonitor());
			ApiBaseline baseline = new ApiBaseline(baselinePath.getAbsolutePath());
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
		return ApiBaselineManager.getManager().getDefaultApiBaseline();
	}

	private IProject importProject(File projectPath) throws CoreException {
		File dotProject = new File(projectPath, IProjectDescription.DESCRIPTION_FILE_NAME);
		if (!dotProject.isFile()) {
			System.err.println("Expected `" + dotProject.getAbsolutePath() + "` file doesn't exist."); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		IProjectDescription projectDescription = ResourcesPlugin.getWorkspace()
				.loadProjectDescription(Path.fromOSString(dotProject.getAbsolutePath()));
		projectDescription.setLocation(Path.fromOSString(projectPath.getAbsolutePath()));
		IProject res = ResourcesPlugin.getWorkspace().getRoot().getProject(projectDescription.getName());
		if (res.exists()) {
			if (!res.getDescription().getLocationURI().equals(projectDescription.getLocationURI())) {
				System.err.println("Project with same name and different location exists in workspace."); //$NON-NLS-1$
				return null;
			}
			res.open(new NullProgressMonitor());
			res.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		} else {
			res.create(projectDescription, new NullProgressMonitor());
			res.open(new NullProgressMonitor());
		}

		projectDescription = res.getDescription();
		if (Arrays.stream(res.getDescription().getBuildSpec()).map(ICommand::getBuilderName)
				.noneMatch(ApiPlugin.BUILDER_ID::equals)) {
			ICommand[] builders = new ICommand[projectDescription.getBuildSpec().length + 1];
			System.arraycopy(projectDescription.getBuildSpec(), 0, builders, 0,
					projectDescription.getBuildSpec().length);
			ICommand buildCommand = projectDescription.newCommand();
			buildCommand.setBuilderName(ApiPlugin.BUILDER_ID);
			builders[builders.length - 1] = buildCommand;
			projectDescription.setBuildSpec(builders);
			res.setDescription(projectDescription, 0, new NullProgressMonitor());
		}
		return res;
	}

	@Override
	public void stop() {
		// Nothing to do
	}

}
