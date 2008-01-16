/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.ui.launcher.BundlesTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

public class OSGiBundleBlock extends AbstractPluginBlock {

	private HashMap levelColumnCache = null;
	private HashMap autoColumnCache = null;
	private TreeEditor levelColumnEditor = null;
	private TreeEditor autoColumnEditor = null;

	class OSGiLabelProvider extends PDELabelProvider {
		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}

		public String getColumnText(Object obj, int index) {
			switch (index) {
				case 0 :
					return super.getColumnText(obj, index);
				case 1 :
					if (levelColumnCache != null && levelColumnCache.containsKey(obj))
						return (String) levelColumnCache.get(obj);
					return ""; //$NON-NLS-1$
				case 2 :
					if (autoColumnCache != null && autoColumnCache.containsKey(obj))
						return (String) autoColumnCache.get(obj);
					return ""; //$NON-NLS-1$
				default :
					return ""; //$NON-NLS-1$
			}
		}
	}

	private ILaunchConfiguration fLaunchConfiguration;

	public OSGiBundleBlock(BundlesTab tab) {
		super(tab);
	}

	protected void createPluginViewer(Composite composite, int span, int indent) {
		super.createPluginViewer(composite, span, indent);
		Tree tree = fPluginTreeViewer.getTree();

		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText(fTab.getName());
		column1.setWidth(300);

		TreeColumn column2 = new TreeColumn(tree, SWT.CENTER);
		column2.setText(PDEUIMessages.EquinoxPluginBlock_levelColumn);
		column2.setWidth(80);

		TreeColumn column3 = new TreeColumn(tree, SWT.CENTER);
		column3.setText(PDEUIMessages.EquinoxPluginBlock_autoColumn);
		column3.setWidth(80);
		tree.setHeaderVisible(true);

		createEditors();
	}

	private void createEditors() {
		final Tree tree = fPluginTreeViewer.getTree();

		levelColumnEditor = new TreeEditor(tree);
		levelColumnEditor.horizontalAlignment = SWT.CENTER;
		levelColumnEditor.minimumWidth = 60;

		autoColumnEditor = new TreeEditor(tree);
		autoColumnEditor.horizontalAlignment = SWT.CENTER;
		autoColumnEditor.grabHorizontal = true;
		autoColumnEditor.minimumWidth = 60;

		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Clean up any previous editor control
				Control oldEditor = levelColumnEditor.getEditor();
				if (oldEditor != null && !oldEditor.isDisposed())
					oldEditor.dispose();

				oldEditor = autoColumnEditor.getEditor();
				if (oldEditor != null && !oldEditor.isDisposed())
					oldEditor.dispose();

				// Identify the selected row
				final TreeItem item = (TreeItem) e.item;
				if (!isEditable(item))
					return;

				if (!isFragment(item)) { // only display editing controls if we're not a fragment
					final Spinner spinner = new Spinner(tree, SWT.BORDER);
					spinner.setMinimum(0);
					String level = item.getText(1);
					int defaultLevel = level.length() == 0 || "default".equals(level) ? 0 : Integer.parseInt(level); //$NON-NLS-1$
					spinner.setSelection(defaultLevel);
					spinner.addModifyListener(new ModifyListener() {
						public void modifyText(ModifyEvent e) {
							if (item.getChecked()) {
								int selection = spinner.getSelection();
								item.setText(1, selection == 0 ? "default" //$NON-NLS-1$
										: Integer.toString(selection));
								fTab.updateLaunchConfigurationDialog();
							}
						}
					});
					levelColumnEditor.setEditor(spinner, item, 1);

					final CCombo combo = new CCombo(tree, SWT.BORDER | SWT.READ_ONLY);
					combo.setItems(new String[] {"default", Boolean.toString(true), Boolean.toString(false)}); //$NON-NLS-1$
					combo.setText(item.getText(2));
					combo.pack();
					combo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							if (item.getChecked()) {
								item.setText(2, combo.getText());
								fTab.updateLaunchConfigurationDialog();
							}
						}
					});
					autoColumnEditor.setEditor(combo, item, 2);
				}

			}
		});
	}

	private boolean isEditable(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase) obj;
			if (!"org.eclipse.osgi".equals(model.getPluginBase().getId())) //$NON-NLS-1$
				return fPluginTreeViewer.getChecked(model);
		}
		return false;
	}

	private boolean isFragment(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IPluginModelBase) {
			return ((IPluginModelBase) obj).isFragmentModel();
		}
		return false;
	}

	protected ILabelProvider getLabelProvider() {
		return new OSGiLabelProvider();
	}

	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {
		Object[] selected = fPluginTreeViewer.getCheckedElements();
		StringBuffer wBuffer = new StringBuffer();
		StringBuffer tBuffer = new StringBuffer();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) selected[i];
				String id = model.getPluginBase().getId();
				TreeItem item = (TreeItem) fPluginTreeViewer.testFindItem(model);
				if (model.getUnderlyingResource() == null) {
					appendToBuffer(tBuffer, id, item);
				} else {
					appendToBuffer(wBuffer, id, item);
				}
			}
		}
		config.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, wBuffer.length() == 0 ? (String) null : wBuffer.toString());
		config.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, tBuffer.length() == 0 ? (String) null : tBuffer.toString());

		StringBuffer buffer = new StringBuffer();
		if (fAddWorkspaceButton.getSelection()) {
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				if (!fPluginTreeViewer.getChecked(fWorkspaceModels[i])) {
					if (buffer.length() > 0)
						buffer.append(","); //$NON-NLS-1$
					buffer.append(fWorkspaceModels[i].getPluginBase().getId());
				}
			}
		}
		config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, buffer.length() > 0 ? buffer.toString() : (String) null);
	}

	private void appendToBuffer(StringBuffer buffer, String id, TreeItem item) {
		if (buffer.length() > 0)
			buffer.append(","); //$NON-NLS-1$
		buffer.append(id);
		buffer.append("@"); //$NON-NLS-1$
		buffer.append(item.getText(1));
		buffer.append(":"); //$NON-NLS-1$
		buffer.append(item.getText(2));
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		super.initializeFrom(configuration);
		initWorkspacePluginsState(configuration);
		initExternalPluginsState(configuration);
		updateCounter();
		fLaunchConfiguration = configuration;
		handleFilterButton(); // Once the page is initialized, apply any filtering.
	}

	private void initExternalPluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map map = BundleLauncherHelper.getTargetBundleMap(configuration);
		Iterator iter = map.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			if (fPluginTreeViewer.setChecked(model, true)) {
				setText(model, map.get(model).toString());
			}
		}
		fNumExternalChecked = map.size();
		resetGroup(fExternalPlugins);
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
	}

	private void resetGroup(NamedElement group) {
		Widget widget = fPluginTreeViewer.testFindItem(group);
		if (widget instanceof TreeItem) {
			TreeItem[] items = ((TreeItem) widget).getItems();
			for (int i = 0; i < items.length; i++) {
				if (!items[i].getChecked()) {
					resetText(items[i]);
				}
			}
		}
	}

	private void initWorkspacePluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map map = BundleLauncherHelper.getWorkspaceBundleMap(configuration);
		Iterator iter = map.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			if (fPluginTreeViewer.setChecked(model, true)) {
				setText(model, map.get(model).toString());
			}
		}
		fNumWorkspaceChecked = map.size();
		resetGroup(fWorkspacePlugins);

		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(fWorkspacePlugins, fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	protected void handleGroupStateChanged(Object group, boolean checked) {
		super.handleGroupStateChanged(group, checked);
		Widget item = fPluginTreeViewer.testFindItem(group);
		if (item instanceof TreeItem) {
			TreeItem[] items = ((TreeItem) item).getItems();
			for (int i = 0; i < items.length; i++) {
				TreeItem child = items[i];
				if (child.getChecked() == (child.getText(1).length() == 0))
					resetText(items[i]);
			}
		}
	}

	protected void handleCheckStateChanged(CheckStateChangedEvent event) {
		super.handleCheckStateChanged(event);
		resetText((IPluginModelBase) event.getElement());
	}

	protected void setChecked(IPluginModelBase model, boolean checked) {
		super.setChecked(model, checked);
		resetText(model);
	}

	protected void setCheckedElements(Object[] checked) {
		super.setCheckedElements(checked);
		updateGroup(fWorkspacePlugins);
		updateGroup(fExternalPlugins);
	}

	private void updateGroup(Object group) {
		Widget item = fPluginTreeViewer.testFindItem(group);
		if (item instanceof TreeItem) {
			TreeItem[] items = ((TreeItem) item).getItems();
			for (int i = 0; i < items.length; i++) {
				TreeItem child = items[i];
				if (child.getChecked() == (child.getText(1).length() == 0))
					resetText(items[i]);
			}
		}
	}

	private void setText(IPluginModelBase model, String value) {
		Widget widget = fPluginTreeViewer.testFindItem(model);
		if (widget instanceof TreeItem) {
			TreeItem item = (TreeItem) widget;
			int index = value == null ? -1 : value.indexOf(':');
			item.setText(1, index == -1 ? "" : value.substring(0, index)); //$NON-NLS-1$
			if (model.isFragmentModel()) {
				item.setText(2, "false"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				item.setText(2, index == -1 ? "" : value.substring(index + 1)); //$NON-NLS-1$
			}
		}
	}

	private void resetText(IPluginModelBase model) {
		Widget widget = fPluginTreeViewer.testFindItem(model);
		if (widget instanceof TreeItem) {
			resetText((TreeItem) widget);
		}
	}

	private void resetText(TreeItem item) {
		if (item.getChecked()) {
			IPluginModelBase model = (IPluginModelBase) item.getData();
			boolean isSystemBundle = "org.eclipse.osgi".equals(model.getPluginBase().getId()); //$NON-NLS-1$
			if (!"default".equals(item.getText(1))) //$NON-NLS-1$
				item.setText(1, isSystemBundle ? "" : "default"); //$NON-NLS-1$ //$NON-NLS-2$
			if (model.isFragmentModel())
				item.setText(2, "false"); //$NON-NLS-1$
			else if (!"default".equals(item.getText(2))) //$NON-NLS-1$
				item.setText(2, isSystemBundle ? "" : "default"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			if (item.getText(1).length() > 0)
				item.setText(1, ""); //$NON-NLS-1$
			if (item.getText(2).length() > 0)
				item.setText(2, ""); //$NON-NLS-1$
		}
	}

	protected void handleRestoreDefaults() {
		Object[] selected = fPluginTreeViewer.getCheckedElements();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] instanceof IPluginModelBase) {
				resetText((IPluginModelBase) selected[i]);
			}
		}
	}

	protected int getTreeViewerStyle() {
		return super.getTreeViewerStyle() | SWT.FULL_SELECTION;
	}

	protected LaunchValidationOperation createValidationOperation() {
		return new OSGiValidationOperation(fLaunchConfiguration);
	}

	/**
	 * Refreshes the tree viewer.  This caches the values of the 
	 * level and auto column, and it clears any editors on the view.
	 * Finally, it sets the selection to the root node.
	 */
	protected void refreshTreeView(CheckboxTreeViewer treeView) {
		// Remove any selection
		if (treeView.getTree().getItemCount() > 0) {
			treeView.getTree().setSelection(treeView.getTree().getItem(0));
		} else {
			treeView.setSelection(new StructuredSelection(StructuredSelection.EMPTY));
		}

		// Reset any editors on the tree viewer
		if (levelColumnEditor != null && levelColumnEditor.getEditor() != null && !levelColumnEditor.getEditor().isDisposed()) {
			levelColumnEditor.getEditor().dispose();
		}

		if (autoColumnEditor != null && autoColumnEditor.getEditor() != null && !autoColumnEditor.getEditor().isDisposed()) {
			autoColumnEditor.getEditor().dispose();
		}

		// Cache the current text
		levelColumnCache = new HashMap();
		autoColumnCache = new HashMap();
		ArrayList allTreeItems = getAllTreeItems(treeView.getTree().getItems());
		for (Iterator iterator = allTreeItems.iterator(); iterator.hasNext();) {
			TreeItem item = (TreeItem) iterator.next();
			levelColumnCache.put(item.getData(), item.getText(1));
			autoColumnCache.put(item.getData(), item.getText(2));
		}
	}

	/**
	 * This gets all the tree items from a tree.  
	 * 
	 * This method must exist in some SWT util library, so it can probably be 
	 * removed when I find it.
	 * 
	 * @param roots
	 * @return
	 */
	private ArrayList getAllTreeItems(TreeItem[] roots) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < roots.length; i++) {
			TreeItem item = roots[i];
			list.add(item);
			list.addAll(getAllTreeItems(item.getItems()));
		}
		return list;
	}

}
