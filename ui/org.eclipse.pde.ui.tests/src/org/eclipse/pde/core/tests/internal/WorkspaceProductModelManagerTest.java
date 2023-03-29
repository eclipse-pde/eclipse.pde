/*******************************************************************************
 *  Copyright (c) 2022, 2022 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.views.features.model.WorkspaceProductModelManager;
import org.eclipse.pde.internal.ui.wizards.product.BaseProductCreationOperation;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.junit.*;
import org.junit.rules.TestRule;

public class WorkspaceProductModelManagerTest {

	@ClassRule
	public static final TestRule CLEAR_WORKSPACE = ProjectUtils.DELETE_ALL_WORKSPACE_PROJECTS_BEFORE_AND_AFTER;

	@Rule
	public final TestRule deleteCreatedTestProjectsAfter = ProjectUtils.DELETE_CREATED_WORKSPACE_PROJECTS_AFTER;

	private static final Set<WorkspaceModelManager<?>> openManagers = ConcurrentHashMap.newKeySet();

	@After
	public void tearDown() {
		openManagers.forEach(WorkspaceModelManager::shutdown);
	}

	@Test
	public void testProducts_productAddedAndRemoved() throws Exception {
		TestWorkspaceProductModelManager mm = createWorkspaceModelManager();
		IProject project = createProject();

		createProduct(project, "aProduct");

		Collection<IProductModel> products = getProductModel(project, mm);
		assertSingleProductWithId("aProduct", products);

		project.getFile("aProduct.product").delete(true, null);
		assertNull(getProductModel(project, mm));
	}

	@Test
	public void testProducts_productAddedToDerivedFolder() throws Exception {
		TestWorkspaceProductModelManager mm = createWorkspaceModelManager();
		IProject project = createProject();

		IFolder folder = project.getFolder("folder1");
		folder.create(true, true, null);
		folder.setDerived(true, null);

		createProduct(project, "folder1/aProduct");

		assertNull(getProductModel(project, mm));
	}

	@Test
	public void testProducts_productAddedToDerivedSourceFolder() throws Exception {
		TestWorkspaceProductModelManager mm = createWorkspaceModelManager();
		IProject project = createPluginProject("plugin.a");

		IFolder srcFolder = project.getFolder("src");
		srcFolder.setDerived(true, null);
		createProduct(project, "src/aProduct");

		Collection<IProductModel> products = getProductModel(project, mm);
		assertSingleProductWithId("aProduct", products);
	}

	@Test
	public void testProducts_productAddedToDeriveFolderNestedInSrcFolder() throws Exception {
		TestWorkspaceProductModelManager mm = createWorkspaceModelManager();
		IProject project = createPluginProject("plugin.a");

		IFolder folder = project.getFolder("src/subFolder");
		folder.create(true, true, null);
		folder.setDerived(true, null);
		createProduct(project, "src/subFolder/aProduct");

		Collection<IProductModel> products = getProductModel(project, mm);
		assertSingleProductWithId("aProduct", products);
	}

	// --- utilities ---

	private class TestWorkspaceProductModelManager extends WorkspaceProductModelManager {
		@Override // Make protected methods visible to tests
		public Collection<IProductModel> getModel(IProject project) {
			return super.getModel(project);
		}

		@Override
		public IProductModel[] getProductModels() {
			return super.getProductModels();
		}
	}

	private TestWorkspaceProductModelManager createWorkspaceModelManager() {
		TestWorkspaceProductModelManager mm = new TestWorkspaceProductModelManager();
		openManagers.add(mm);
		// ensure manager is initialized, otherwise events can be missed
		assertEquals(0, mm.getProductModels().length);
		return mm;
	}

	private static IProject createProject() throws CoreException {
		IProject project = WorkspaceModelManagerTest.getWorkspaceProject("products");
		project.create(null);
		project.open(null);
		return project;
	}

	private static void createProduct(IProject project, String pathName) throws Exception {
		IFile f = project.getFile(pathName + ".product");
		new BaseProductCreationOperation(f).run(null);
	}

	private static IProject createPluginProject(String symbolicName) throws CoreException {
		IProject project = ProjectUtils.createPluginProject(symbolicName, symbolicName, "1.0.0", (d, s) -> {
			d.setBundleClasspath(null);
		});
		IFolder srcFolder = project.getFolder("src");
		srcFolder.create(true, true, null);
		IClasspathEntry cpEntry = JavaCore.newSourceEntry(srcFolder.getFullPath());
		JavaCore.create(project).setRawClasspath(new IClasspathEntry[] { cpEntry }, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		WorkspaceModelManagerTest.awaitJobs();
		return project;
	}

	private static void assertSingleProductWithId(String expectedId, Collection<IProductModel> products) {
		assertEquals(1, products.size());
		IProductModel product = products.iterator().next();
		assertEquals(expectedId, product.getProduct().getId());
	}

	private Collection<IProductModel> getProductModel(IProject project, TestWorkspaceProductModelManager manager) {
		WorkspaceModelManagerTest.awaitJobs();
		return manager.getModel(project);
	}

}
