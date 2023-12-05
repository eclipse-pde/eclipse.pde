/*******************************************************************************
 * Copyright (c) 2017, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Elias N Vasylenko <eliasvasylenko@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.internal.ui.bndtools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import aQute.bnd.build.Container;
import aQute.bnd.build.Workspace;
import biz.aQute.resolve.Bndrun;

public class RunDescriptorTargetLocationPage extends BndTargetLocationPage {
	private static final String			FILE_EXTENSION	= "bndrun"; //$NON-NLS-1$

	private RunDescriptorTargetLocation	targetLocation;

	private IFile						runDescriptorFile;
	private TreeViewer					bundleList;

	public RunDescriptorTargetLocationPage(ITargetDefinition targetDefinition,
		RunDescriptorTargetLocation targetLocation) {
		super(BndToolsMessages.RunDescriptorTargetLocationPage_AddBndRunDescriptorContainer, BndToolsMessages.RunDescriptorTargetLocationPage_AddBndRunDescriptor,
			BndToolsMessages.RunDescriptorTargetLocationPage_Select, targetDefinition);

		if (targetLocation != null) {
			this.targetLocation = targetLocation;
			this.runDescriptorFile = targetLocation.getFile();
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);

		SWTFactory.createLabel(composite, BndToolsMessages.RunDescriptorTargetLocationPage_RunDescriptor, 1);
		TreeViewer projectTree = createRunDescriptorSelectionArea(composite);
		SWTFactory.createLabel(composite, BndToolsMessages.RunDescriptorTargetLocationPage_RunBundles, 1);
		bundleList = createBundleListArea(composite, 1);

		setControl(composite);
		setPageComplete(false);

		updateTarget();
		selectTargetInTree(projectTree);

		if (projectTree.getTree()
			.getItems().length == 0)
			logError("No run descriptors found in workspace", null); //$NON-NLS-1$
	}

	private TreeViewer createRunDescriptorSelectionArea(Composite composite) {
		final TreeViewer projectTree = new TreeViewer(
			new Tree(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER));
		projectTree.getTree()
			.setLayoutData(fillGridData(1));

		projectTree.setContentProvider(new WorkbenchContentProvider());
		projectTree.setLabelProvider(new WorkbenchLabelProvider());
		projectTree.setInput(ResourcesPlugin.getWorkspace());
		projectTree.setFilters(getProjectTreeFilters());
		projectTree.addSelectionChangedListener(event -> {
			Object selection = ((ITreeSelection) event.getSelection()).getFirstElement();

			if (selection != null && selection instanceof IFile) {
				runDescriptorFile = (IFile) selection;
			} else {
				runDescriptorFile = null;
			}

			updateTarget();
		});
		projectTree.expandAll();

		return projectTree;
	}

	private ViewerFilter[] getProjectTreeFilters() {
		PatternFilter bndrunFilter = new PatternFilter() {
			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				return element instanceof IFile && FILE_EXTENSION.equals(((IFile) element).getFileExtension());
			}

			@Override
			public boolean isElementSelectable(Object element) {
				return element instanceof IFile;
			}
		};
		bndrunFilter.setPattern("*"); //$NON-NLS-1$
		return new ViewerFilter[] {
			bndrunFilter
		};
	}

	private void updateTarget() {
		if (runDescriptorFile == null) {
			clearTarget();
		} else {
			try {
				File file = runDescriptorFile.getRawLocation()
					.makeAbsolute()
					.toFile();
				Workspace workspace = BndTargetLocation.getWorkspace();
				List<String> bundles = new ArrayList<>();

				try (Bndrun bndRun = new Bndrun(workspace, file)) {
					for (Container bundle : bndRun.getRunbundles()) {
						bundles.add(bundle.getBundleSymbolicName() + " - " + bundle.getVersion()); //$NON-NLS-1$
					}
					bundleList.setInput(bundles);
				}

				setPageComplete(true);

				if (bundles.isEmpty())
					logWarning("Run descriptor is empty: " + runDescriptorFile.getFullPath(), null); //$NON-NLS-1$
				else
					resetMessage();
			} catch (Exception e) {
				clearTarget();
				logError(e.getMessage(), e);
			}
		}
	}

	private void clearTarget() {
		runDescriptorFile = null;
		bundleList.setInput(Collections.emptySet());
		setPageComplete(false);
		resetMessage();
	}

	private boolean selectTargetInTree(TreeViewer projectTree) {
		if (runDescriptorFile == null)
			return false;

		for (TreeItem item : projectTree.getTree()
			.getItems()) {
			if (setSelectedFileInTree(projectTree, item)) {
				return true;
			}
		}
		return false;
	}

	private boolean setSelectedFileInTree(TreeViewer projectTree, TreeItem item) {
		if (item.getData() instanceof IFile) {
			IFile file = (IFile) item.getData();
			if (file.equals(runDescriptorFile)) {
				projectTree.getTree()
					.select(item);
				return true;
			}
		}

		for (TreeItem child : item.getItems()) {
			if (setSelectedFileInTree(projectTree, child)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public RunDescriptorTargetLocation getBundleContainer() {
		if (targetLocation == null) {
			targetLocation = new RunDescriptorTargetLocation();
		}

		return targetLocation.setRunDescriptor(runDescriptorFile);
	}
}
