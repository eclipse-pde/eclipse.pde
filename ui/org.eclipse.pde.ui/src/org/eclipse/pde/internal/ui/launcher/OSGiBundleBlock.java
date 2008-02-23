/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404 and bug 207064
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
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

	private HashMap levelColumnCache = new HashMap();
	private HashMap autoColumnCache = new HashMap();
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
								levelColumnCache.put(item.getData(), item.getText(1));
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
								autoColumnCache.put(item.getData(), item.getText(2));
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
			String systemBundleId = PDECore.getDefault().getModelManager().getSystemBundleId();
			if (!(systemBundleId.equals(model.getPluginBase().getId())))
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
				if (model.getUnderlyingResource() == null) {
					appendToBuffer(tBuffer, model);
				} else {
					appendToBuffer(wBuffer, model);
				}
			}
		}
		config.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, wBuffer.length() == 0 ? (String) null : wBuffer.toString());
		config.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, tBuffer.length() == 0 ? (String) null : tBuffer.toString());

		StringBuffer buffer = new StringBuffer();
		if (fAddWorkspaceButton.getSelection()) {
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				if (!fPluginTreeViewer.getChecked(fWorkspaceModels[i])) {
					appendToBuffer(buffer, fWorkspaceModels[i]);
				}
			}
		}
		config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, buffer.length() > 0 ? buffer.toString() : (String) null);
	}

	private void appendToBuffer(StringBuffer buffer, IPluginModelBase model) {
		if (buffer.length() > 0)
			buffer.append(","); //$NON-NLS-1$ 

		String startLevel = levelColumnCache.get(model) != null ? levelColumnCache.get(model).toString() : null;
		String autoStart = autoColumnCache.get(model) != null ? autoColumnCache.get(model).toString() : null;
		String value = BundleLauncherHelper.writeBundles(model, startLevel, autoStart);
		buffer.append(value);
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		super.initializeFrom(configuration);
		levelColumnCache = new HashMap();
		autoColumnCache = new HashMap();
		initWorkspacePluginsState(configuration);
		initExternalPluginsState(configuration);
		updateCounter();
		fLaunchConfiguration = configuration;
		handleFilterButton(); // Once the page is initialized, apply any filtering.
	}

	private void initExternalPluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map map = BundleLauncherHelper.getTargetBundleMap(configuration);
		Iterator iter = map.keySet().iterator();
		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
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
		Object[] children = group.getChildren();
		if (children == null)
			return;
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof IPluginModelBase) {
				resetText((IPluginModelBase) child);
			}
		}
	}

	private void initWorkspacePluginsState(ILaunchConfiguration configuration) throws CoreException {
		Map map = BundleLauncherHelper.getWorkspaceBundleMap(configuration);
		Iterator iter = map.keySet().iterator();
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, false);
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

		if (group instanceof NamedElement) {
			NamedElement namedElement = (NamedElement) group;
			TreeItem item = (TreeItem) fPluginTreeViewer.testFindItem(namedElement);
			if (item != null) {
				TreeItem[] children = item.getItems();
				if (children == null)
					return;
				for (int i = 0; i < children.length; i++) {
					TreeItem childItem = children[i];
					Object child = childItem.getData();
					if (child instanceof IPluginModelBase) {
						resetText((IPluginModelBase) child);
					}
				}
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
		if (group instanceof NamedElement) {
			NamedElement namedElement = (NamedElement) group;
			Object[] children = namedElement.getChildren();
			if (children == null) {
				return;
			}
			for (int i = 0; i < children.length; i++) {
				Object child = children[i];
				if (child instanceof IPluginModelBase) {
					resetText((IPluginModelBase) child);
				}
			}
		}
	}

	private void setText(IPluginModelBase model, String value) {
		Widget widget = fPluginTreeViewer.testFindItem(model);
		if (widget instanceof TreeItem) {
			TreeItem item = (TreeItem) widget;
			int index = value == null ? -1 : value.indexOf(':');
			String levelValue = index == -1 ? "" : value.substring(0, index); //$NON-NLS-1$
			String autoValue = null;
			item.setText(1, levelValue);
			if (model.isFragmentModel()) {
				autoValue = "false"; //$NON-NLS-1$
				item.setText(2, autoValue);
			} else {
				autoValue = index == -1 ? "" : value.substring(index + 1); //$NON-NLS-1$
				item.setText(2, autoValue);
			}
			levelColumnCache.put(model, levelValue);
			autoColumnCache.put(model, autoValue);
		}
	}

	private void resetText(IPluginModelBase model) {
		String levelText = null;
		String autoText = null;
		Widget widget = fPluginTreeViewer.testFindItem(model);
		if (fPluginTreeViewer.getChecked(model)) {
			boolean isSystemBundle = "org.eclipse.osgi".equals(model.getPluginBase().getId()); //$NON-NLS-1$
			levelText = isSystemBundle ? "" : "default"; //$NON-NLS-1$ //$NON-NLS-2$
			autoText = isSystemBundle ? "" : "default"; //$NON-NLS-1$ //$NON-NLS-2$
			if (levelColumnCache.containsKey(model) && !isSystemBundle) {
				levelText = (String) levelColumnCache.get(model);
				levelText = levelText.length() > 0 ? levelText : "default"; //$NON-NLS-1$
			}
			if (autoColumnCache.containsKey(model) && !isSystemBundle) {
				autoText = (String) autoColumnCache.get(model);
				autoText = autoText.length() > 0 ? autoText : "default"; //$NON-NLS-1$
			}

			if (model.isFragmentModel()) {
				autoText = "false"; //$NON-NLS-1$
			}
		} else {
			levelText = ""; //$NON-NLS-1$
			autoText = ""; //$NON-NLS-1$
		}
		if (levelText != null) {
			levelColumnCache.put(model, levelText);
			if (widget instanceof TreeItem) {
				((TreeItem) widget).setText(1, levelText);
			}
		}
		if (autoText != null) {
			autoColumnCache.put(model, autoText);
			if (widget instanceof TreeItem) {
				((TreeItem) widget).setText(2, autoText);
			}
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
	 * Refreshes the tree viewer.  It clears any editors on the view.
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
	}
}
