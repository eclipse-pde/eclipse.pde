/*******************************************************************************
 * Copyright (c) 2017, 2019 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - 507861
 *******************************************************************************/
package org.eclipse.pde.ui.templates.tests;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.ds.internal.annotations.Messages;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.launching.launcher.ProductValidationOperation;
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
import org.junit.runners.Parameterized.*;
import org.osgi.framework.Bundle;

@RunWith(Parameterized.class)
public class TestPDETemplates {

	private static class NewProjectCreationOperationExtension extends NewProjectCreationOperation {
		private NewProjectCreationOperationExtension(IFieldData data, IProjectProvider provider,
				IPluginContentWizard template) {
			super(data, provider, template);
		}

		@Override
		public void execute(IProgressMonitor monitor)
				throws CoreException, InvocationTargetException, InterruptedException {
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

	@Parameters(name = "{index}: {0}")
	public static Collection<WizardElement> allTemplateWizards() {
		return Arrays.asList(new NewPluginProjectWizard().getAvailableCodegenWizards().getChildren()).stream() //
				.filter(o -> (o instanceof WizardElement)) //
				.map(o -> (WizardElement) o) //
				.collect(Collectors.toList());
	}

	private static IProject project;

	@Before
	public void createProject() throws Exception {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
		String id = TestPDETemplates.class.getSimpleName() + '_' + template.getID();
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(id);
		if (!project.exists()) {
			project.create(new NullProgressMonitor());
			project.open(new NullProgressMonitor());
			createProjectWithTemplate();
		}
	}

	private static void createProjectWithTemplate()
			throws CoreException, InvocationTargetException, InterruptedException {
		PluginFieldData data = new PluginFieldData();
		data.setId(project.getName());
		data.setVersion("0.0.1.qualifier");
		data.setHasBundleStructure(true);
		data.setSourceFolderName("src");
		data.setOutputFolderName("bin");
		data.setExecutionEnvironment("JavaSE-1.8");
		String version = System.getProperty("java.specification.version"); //$NON-NLS-1$
		int ver = -1;
		try {
			ver = Integer.parseInt(version);
		} catch (NumberFormatException e) {
			// preJava9
		}
		if (ver >= 9) {
			data.setExecutionEnvironment("JavaSE-" + version);
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
				return project;
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
		NewProjectCreationOperationExtension op = new NewProjectCreationOperationExtension(data, projectProvider,
				pluginContentWizard);
		op.execute(new NullProgressMonitor());
	}

	@Test
	public void configureProjectAndCheckMarkers() throws CoreException {
		project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

		IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

		// ignore missing package export marker
		if (markers.length == 1 && CompilerFlags.P_MISSING_EXPORT_PKGS
				.equals(markers[0].getAttribute(PDEMarkerFactory.compilerKey, ""))) {
			System.out.println("Template '" + template.getLabel() + "' ignored errors.");
			System.out.println(markers[0]);
			System.out.println("--------------------------------------------------------");
			markers = new IMarker[0];
		}
		// ignore "DS Annotations missing from permanent build path"
		if (markers.length == 1 && Messages.DSAnnotationCompilationParticipant_buildpathProblemMarker_message
				.equals(markers[0].getAttribute(IMarker.MESSAGE, ""))) {
			System.out.println("Template '" + template.getLabel() + "' ignored errors.");
			System.out.println(markers[0]);
			System.out.println("--------------------------------------------------------");
			markers = new IMarker[0];
		}

		assertThat("Template '" + template.getLabel() + "' generates errors.", markers, equalTo(new IMarker[0]));
	}

	@Test
	public void validateProduct() throws CoreException {
		IResource productFile = project.findMember(project.getName() + ".product");
		Assume.assumeNotNull(productFile);

		WorkspaceProductModel model = new WorkspaceProductModel((IFile) productFile, false);
		model.load();
		IProduct product = model.getProduct();

		Set<IPluginModelBase> launchPlugins = new HashSet<>();
		for (IProductPlugin plugin : product.getPlugins()) {
			IPluginModelBase pluginModel = PluginRegistry.findModel(plugin.getId());
			if (pluginModel != null && TargetPlatformHelper.matchesCurrentEnvironment(pluginModel))
				launchPlugins.add(pluginModel);
		}

		ProductValidationOperation validationOperation = new ProductValidationOperation(
				launchPlugins.toArray(new IPluginModelBase[0]));
		validationOperation.run(new NullProgressMonitor());

		if (validationOperation.hasErrors()) {
			Object errors = validationOperation.getInput().values().stream() //
					.flatMap(Arrays::stream) //
					.map(Object::toString) //
					.collect(toSet());
			Assert.fail("Generated product fails validation: \n" + errors);
		}
	}

	@AfterParam
	public static void deleteProject() throws CoreException {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
		project.delete(true, new NullProgressMonitor());
	}
}
