/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - 507861
 *******************************************************************************/
package org.eclipse.pde.ui.templates.tests;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.launching.*;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.internal.ui.wizards.plugin.*;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.ui.PlatformUI;
import org.junit.*;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;

@RunWith(Parameterized.class)
public class TestPDETemplates {

	private static class NewProjectCreationOperationExtension extends NewProjectCreationOperation {
		private NewProjectCreationOperationExtension(IFieldData data, IProjectProvider provider, IPluginContentWizard template) {
			super(data, provider, template);
		}

		@Override
		public void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
			super.execute(monitor);
		}
	}

	@BeforeClass
	public static void setTargetPlatform() throws CoreException, InterruptedException {
		ITargetPlatformService tpService = TargetPlatformService.getDefault();
		tpService.getWorkspaceTargetDefinition().resolve(new NullProgressMonitor());
		// workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=343156
		ITargetDefinition targetDef = tpService.newTarget();
		targetDef.setName("Tycho platform");
		Bundle[] bundles = Platform.getBundle("org.eclipse.core.runtime").getBundleContext().getBundles();
		List<ITargetLocation> bundleContainers = new ArrayList<>();
		Set<File> dirs = new HashSet<>();
		for (Bundle bundle : bundles) {
			EquinoxBundle bundleImpl = (EquinoxBundle) bundle;
			Generation generation = (Generation) bundleImpl.getModule().getCurrentRevision().getRevisionInfo();
			File file = generation.getBundleFile().getBaseFile();
			File folder = file.getParentFile();
			if (!dirs.contains(folder)) {
				dirs.add(folder);
				bundleContainers.add(tpService.newDirectoryLocation(folder.getAbsolutePath()));
			}
		}
		targetDef.setTargetLocations(bundleContainers.toArray(new ITargetLocation[bundleContainers.size()]));
		targetDef.setArch(Platform.getOSArch());
		targetDef.setOS(Platform.getOS());
		targetDef.setWS(Platform.getWS());
		targetDef.setNL(Platform.getNL());
		// targetDef.setJREContainer()
		tpService.saveTargetDefinition(targetDef);

		Job job = new LoadTargetDefinitionJob(targetDef);
		job.schedule();
		job.join();

		// }
	}

	@Parameter
	public static WizardElement template;

	@Parameters
	public static Collection<WizardElement> allTemplateWizards() {
		return Arrays.asList(new NewPluginProjectWizard().getAvailableCodegenWizards().getChildren()).stream()
				.filter(o -> (o instanceof WizardElement))
				.map(o -> (WizardElement)o)
				.collect(Collectors.toList());
	}

	private IProject project;

	@Before
	public void createProject() throws CoreException {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
		String id = getClass().getSimpleName() + '_' + template.getID() + '_' + System.currentTimeMillis();
		this.project = ResourcesPlugin.getWorkspace().getRoot().getProject(id);
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
	}

	@Test
	public void configureProjectAndCheckMarkers() throws CoreException, InvocationTargetException, InterruptedException {
		PluginFieldData data = new PluginFieldData();
		data.setId(project.getName());
		data.setVersion("0.0.1.qualifier");
		data.setHasBundleStructure(true);
		data.setSourceFolderName("src");
		data.setOutputFolderName("bin");
		data.setExecutionEnvironment("JavaSE-1.8");
		IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
		System.out.println("Bug 530132 - 24 pde.ui.templates.tests");
		if (defaultVMInstall != null) {
			System.out.println("Default Install found");
			if (JavaRuntime.isModularJava(defaultVMInstall) && defaultVMInstall instanceof AbstractVMInstall) {
				System.out.println("Modular Java found");
				String vmver = ((AbstractVMInstall) defaultVMInstall).getJavaVersion();
				if (vmver != null) {
					System.out.println("Version String found=" + vmver);
					data.setExecutionEnvironment("JavaSE-" + vmver);
				}
			}
		}

		data.setTargetVersion(ICoreConstants.TARGET_VERSION_LATEST);
		data.setDoGenerateClass(true);
		String pureOSGi = template.getConfigurationElement().getAttribute("pureOSGi");
		if ("true".equals(pureOSGi)) {
			data.setOSGiFramework("Equinox");
		}
		data.setClassname(project.getName().toLowerCase() + ".Activator");
		IProjectProvider projectProvider = new IProjectProvider() {
			@Override
			public IProject getProject() {
				return TestPDETemplates.this.project;
			}

			@Override
			public String getProjectName() {
				return getProject().getName();
			}

			@Override
			public IPath getLocationPath() {
				return getProject().getLocation();
			}
		};
		IPluginContentWizard pluginContentWizard = (IPluginContentWizard) template.createExecutableExtension();
		pluginContentWizard.init(data);
		NewProjectCreationOperationExtension op = new NewProjectCreationOperationExtension(data, projectProvider, pluginContentWizard);
		op.execute(new NullProgressMonitor());
		this.project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		IMarker[] markers = this.project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		if (markers.length > 0) {
			System.out.println("Template '" + template.getLabel() + "' generates errors.");
			for (IMarker marker : markers) {
				System.out.println(marker);
			}
			System.out.println("--------------------------------------------------------");
		}
		Assert.assertArrayEquals("Template '" + template.getLabel() + "' generates errors.", new IMarker[0], markers);
	}

	@After
	public void deleteProject() throws CoreException {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
		this.project.delete(true, new NullProgressMonitor());
	}
}
