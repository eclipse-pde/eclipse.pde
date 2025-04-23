/*******************************************************************************
 * Copyright (c) 2025 Patrick Ziegler and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Ziegler - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.project;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.e4.ui.dialogs.filteredtree.FilteredTable;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.views.dependencies.DependenciesView;
import org.eclipse.pde.internal.ui.views.dependencies.DependenciesViewListPage;
import org.eclipse.pde.internal.ui.views.dependencies.DependenciesViewTreePage;
import org.eclipse.pde.ui.tests.util.ProjectUtils;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.FilteredTree;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Constants;

/**
 * Test case for the {@link DependenciesViewListPage} and
 * {@link DependenciesViewTreePage}
 */
public class PluginDependencyTests {
	private static DependenciesView dependencyView;
	private static IAction showTreeAction;
	private static IAction showListAction;

	@BeforeClass
	public static void setUpAll() throws Exception {
		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		dependencyView = (DependenciesView) workbenchPage.showView("org.eclipse.pde.ui.DependenciesView");

		IToolBarManager toolBarManager = dependencyView.getViewSite().getActionBars().getToolBarManager();
		for (IContributionItem contribution : toolBarManager.getItems()) {
			if (contribution instanceof ActionContributionItem actionContribution) {
				IAction action = actionContribution.getAction();
				if (PDEUIMessages.DependenciesView_ShowListAction_label.equals(action.getText())) {
					showListAction = action;
				} else if (PDEUIMessages.DependenciesView_ShowTreeAction_label.equals(action.getText())) {
					showTreeAction = action;
				}
			}
		}

		IProject project = ProjectUtils.createPluginProject("Plugin-A", "1.0.0.qualifier",
				Map.of(Constants.EXPORT_PACKAGE, "plug.a", Constants.IMPORT_PACKAGE, "plug.b"));
		ProjectUtils.createPluginProject("Plugin-B", "1.0.0.qualifier",
				Map.of(Constants.EXPORT_PACKAGE, "plug.b", Constants.IMPORT_PACKAGE, "plug.c"));
		ProjectUtils.createPluginProject("Plugin-C", "1.0.0.qualifier",
				Map.of(Constants.EXPORT_PACKAGE, "plug.c", Constants.IMPORT_PACKAGE, "plug.a"));

		IPluginModelBase projectModel = PDECore.getDefault().getModelManager().findModel(project);
		dependencyView.openTo(projectModel);
	}

	@AfterClass
	public static void tearDownAll() throws Exception {
		IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		workbenchPage.hideView(dependencyView);

		ProjectUtils.deleteAllWorkspaceProjects();
	}

	@Test
	public void testDependencyTableFilter() {
		FilteredTable composite = openListPage();
		Table table = composite.getViewer().getTable();
		assertEquals(3, table.getItemCount());

		// Filter for Plugin-A
		setPatternFilter(composite, "Plugin-A");
		assertEquals(1, table.getItemCount());
		assertEquals("Plugin-A (1.0.0.qualifier)", table.getItem(0).getText());

		setPatternFilter(composite, "A");
		assertEquals(1, table.getItemCount());
		assertEquals("Plugin-A (1.0.0.qualifier)", table.getItem(0).getText());

		// Filter for Plugin-B
		setPatternFilter(composite, "Plugin-B");
		assertEquals(1, table.getItemCount());
		assertEquals("Plugin-B (1.0.0.qualifier)", table.getItem(0).getText());

		setPatternFilter(composite, "B");
		assertEquals(1, table.getItemCount());
		assertEquals("Plugin-B (1.0.0.qualifier)", table.getItem(0).getText());

		// Filter for Plugin-C
		setPatternFilter(composite, "Plugin-C");
		assertEquals(table.getItemCount(), 1);
		assertEquals(table.getItem(0).getText(), "Plugin-C (1.0.0.qualifier)");

		setPatternFilter(composite, "C");
		assertEquals(1, table.getItemCount());
		assertEquals("Plugin-C (1.0.0.qualifier)", table.getItem(0).getText());

		// Filter for common prefix
		setPatternFilter(composite, "Plugin-");
		assertEquals(3, table.getItemCount());
	}

	private void setPatternFilter(FilteredTable composite, String pattern) {
		composite.getPatternFilter().setPattern(pattern);
		composite.getViewer().refresh();
	}

	private FilteredTable openListPage() {
		showListAction.setChecked(true);
		showListAction.run();
		return (FilteredTable) dependencyView.getCurrentPage().getControl();
	}

	@Test
	public void testDependencyTreeFilter() {
		FilteredTree composite = openTreePage();
		TreeViewer treeViewer = composite.getViewer();
		assertEquals(treeViewer.getTree().getItemCount(), 1);

		// Filter for Plugin-A
		setPatternFilter(composite, "Plugin-A", 4);
		assertPath(composite, //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)", //
				"Plugin-C (1.0.0.qualifier)", //
				"Plugin-A (1.0.0.qualifier)");

		setPatternFilter(composite, "A", 4);
		assertPath(composite, //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)", //
				"Plugin-C (1.0.0.qualifier)", //
				"Plugin-A (1.0.0.qualifier)");

		// Filter for Plugin-B
		setPatternFilter(composite, "Plugin-B", 5);
		assertPath(composite, //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)", //
				"Plugin-C (1.0.0.qualifier)", //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)");

		setPatternFilter(composite, "B", 5);
		assertPath(composite, //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)", //
				"Plugin-C (1.0.0.qualifier)", //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)");

		// Filter for Plugin-C
		setPatternFilter(composite, "Plugin-C", 3);
		assertPath(composite, //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)", //
				"Plugin-C (1.0.0.qualifier)");

		setPatternFilter(composite, "C", 3);
		assertPath(composite, //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)", //
				"Plugin-C (1.0.0.qualifier)");

		// Filter for common prefix
		setPatternFilter(composite, "Plugin-", 5);
		assertPath(composite, //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)", //
				"Plugin-C (1.0.0.qualifier)", //
				"Plugin-A (1.0.0.qualifier)", //
				"Plugin-B (1.0.0.qualifier)");
	}

	private void assertPath(FilteredTree composite, String... segments) {
		Tree tree = composite.getViewer().getTree();
		assertEquals(1, tree.getItemCount());

		TreeItem currentItem = tree.getItem(0);
		for (int i = 0; i < segments.length; ++i) {
			assertEquals(segments[i], currentItem.getText());
			if (i < segments.length - 1) {
				currentItem = currentItem.getItem(0);
			}
		}
	}

	private void setPatternFilter(FilteredTree composite, String pattern, int level) {
		composite.getPatternFilter().setPattern(pattern);
		composite.getViewer().expandToLevel(level);
		composite.getViewer().refresh();
	}

	private FilteredTree openTreePage() {
		showTreeAction.setChecked(true);
		showTreeAction.run();
		return (FilteredTree) dependencyView.getCurrentPage().getControl();
	}
}
