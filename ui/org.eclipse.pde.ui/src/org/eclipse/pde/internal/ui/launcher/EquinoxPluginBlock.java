/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.ui.launcher.EquinoxPluginsTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

public class EquinoxPluginBlock extends AbstractPluginBlock {
	
	class EquinoxLabelProvider extends PDELabelProvider {
		
		public Image getColumnImage(Object obj, int index) {
			return index == 0 ? super.getColumnImage(obj, index) : null;
		}
		public String getColumnText(Object obj, int index) {
			switch (index) {
			case 0:
				return super.getColumnText(obj, index);
			default:
				return ""; //$NON-NLS-1$
			}
		}
	}

	public EquinoxPluginBlock(EquinoxPluginsTab tab) {
		super(tab);
	}
	
	protected void createPluginViewer(Composite composite) {
		super.createPluginViewer(composite);
    	Tree tree = fPluginTreeViewer.getTree();
 
    	TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
    	column1.setText(PDEUIMessages.EquinoxPluginBlock_pluginsColumn); 
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

		final TreeEditor editor1 = new TreeEditor(tree);
		editor1.horizontalAlignment = SWT.CENTER;
		editor1.minimumWidth = 60;

		final TreeEditor editor2 = new TreeEditor(tree);
		editor2.horizontalAlignment = SWT.CENTER;
		editor2.grabHorizontal = true;
		editor2.minimumWidth = 60;

		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Clean up any previous editor control
				Control oldEditor = editor1.getEditor();
				if (oldEditor != null)
					oldEditor.dispose();

				oldEditor = editor2.getEditor();
				if (oldEditor != null)
					oldEditor.dispose();

				// Identify the selected row
				final TreeItem item = (TreeItem) e.item;
				if (!isEditable(item))
					return;

				final Spinner spinner = new Spinner(tree, SWT.BORDER);
				spinner.setMinimum(1);
				final int defaultLevel = ((EquinoxPluginsTab)fTab).getDefaultStartLevel();
				int level = "default".equals(item.getText(1))
							? defaultLevel
							: Integer.parseInt(item.getText(1));
				spinner.setSelection(level);
				spinner.addModifyListener(new ModifyListener() {
					public void modifyText(ModifyEvent e) {
						if (item.getChecked()) {
							int selection = spinner.getSelection();
							item.setText(1, defaultLevel == selection 
												? "default" 
												: Integer.toString(selection));
							fTab.updateLaunchConfigurationDialog();
						}
					}
				});
				editor1.setEditor(spinner, item, 1);

				final CCombo combo = new CCombo(tree, SWT.BORDER | SWT.READ_ONLY);
				combo.setItems(new String[] { "default", Boolean.toString(true), Boolean.toString(false) });
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
				editor2.setEditor(combo, item, 2);

			}
		});			
	}
	
	private boolean isEditable(TreeItem item) {
		Object obj = item.getData();
		if (obj instanceof IPluginModelBase) {
			IPluginModelBase model = (IPluginModelBase)obj;
			if (!"org.eclipse.osgi".equals(model.getPluginBase().getId())) //$NON-NLS-1$
				return fPluginTreeViewer.getChecked(model);
		}
		return false;
	}
	
	protected ILabelProvider getLabelProvider() {
		return new EquinoxLabelProvider();
	}
	
	protected void savePluginState(ILaunchConfigurationWorkingCopy config) {		
		Object[] selected = fPluginTreeViewer.getCheckedElements();
		StringBuffer wBuffer = new StringBuffer();
		StringBuffer tBuffer = new StringBuffer();
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase)selected[i];
				String id = model.getPluginBase().getId();
				TreeItem item = (TreeItem)fPluginTreeViewer.testFindItem(model);
				if (model.getUnderlyingResource() == null) {
					appendToBuffer(tBuffer, id, item);
				} else {
					appendToBuffer(wBuffer, id, item);
				}
			}
		}		
		config.setAttribute(IPDELauncherConstants.WORKSPACE_BUNDLES, 
							wBuffer.length() == 0 ? (String)null : wBuffer.toString());		
		config.setAttribute(IPDELauncherConstants.TARGET_BUNDLES, 
							tBuffer.length() == 0 ? (String)null : tBuffer.toString());
		
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
		config.setAttribute(IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS, 
							buffer.length() > 0 ? buffer.toString() : (String)null);
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
	
	private static Map retrieveMap(ILaunchConfiguration configuration, String attribute) {
		Map map = new TreeMap();
		try {
			String value = configuration.getAttribute(attribute, ""); //$NON-NLS-1$
			StringTokenizer tok = new StringTokenizer(value, ","); //$NON-NLS-1$
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int index = token.indexOf('@');
				map.put(token.substring(0, index), token.substring(index + 1));
			}
		} catch (CoreException e) {
		}	
		return map;
	}
	
	public static Map retrieveTargetMap(ILaunchConfiguration configuration) {
		return retrieveMap(configuration, IPDELauncherConstants.TARGET_BUNDLES);
	}
	
	public static Map retrieveWorkspaceMap(ILaunchConfiguration configuration) throws CoreException {
		Map map = retrieveMap(configuration, IPDELauncherConstants.WORKSPACE_BUNDLES);
		if (configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true)) {
			TreeSet deselectedPlugins = LaunchPluginValidator.parsePlugins(configuration, IPDELauncherConstants.DESELECTED_WORKSPACE_PLUGINS);
			IPluginModelBase[] models = PDECore.getDefault().getModelManager().getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				String id = models[i].getPluginBase().getId();
				if (!map.containsKey(id) && !deselectedPlugins.contains(id)) {
					map.put(id, "default:default"); //$NON-NLS-1$
				}
			}
		}
		return map;
	}

	public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
		super.initializeFrom(configuration);
		initWorkspacePluginsState(configuration);
		initExternalPluginsState(configuration);
		updateCounter();
	}
		
	private void initExternalPluginsState(ILaunchConfiguration configuration)
			throws CoreException {
		fNumExternalChecked = 0;
		fPluginTreeViewer.setSubtreeChecked(fExternalPlugins, false);
		
		Map map = retrieveTargetMap(configuration);
		for (int i = 0; i < fExternalModels.length; i++) {
			IPluginModelBase model = fExternalModels[i];
			if (map.containsKey(model.getPluginBase().getId())) {
				if (fPluginTreeViewer.setChecked(model, true)) {
					fNumExternalChecked += 1;
					setText(model, (String)map.get(model.getPluginBase().getId()));
				}
			}
		}
		
		resetGroup(fExternalPlugins);
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0
				&& fNumExternalChecked < fExternalModels.length);
	}

	private void resetGroup(NamedElement group) {
		Widget widget = fPluginTreeViewer.testFindItem(group);
		if (widget instanceof TreeItem) {
			TreeItem[] items = ((TreeItem)widget).getItems();
			for (int i = 0; i < items.length; i++) {
				if (!items[i].getChecked()) {
					resetText(items[i]);
				}
			}
		}
	}

	private void initWorkspacePluginsState(ILaunchConfiguration configuration)
			throws CoreException {
		fNumWorkspaceChecked = 0;
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, false);
		
		Map map = retrieveWorkspaceMap(configuration);
		for (int i = 0; i < fWorkspaceModels.length; i++) {
			IPluginModelBase model = fWorkspaceModels[i];
			if (map.containsKey(model.getPluginBase().getId())) {
				if (fPluginTreeViewer.setChecked(model, true)) {
					fNumWorkspaceChecked += 1;
					setText(model, (String)map.get(model.getPluginBase().getId()));
				}
			}
		}
		
		resetGroup(fWorkspacePlugins);

		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}
	
	protected void handleGroupStateChanged(Object group, boolean checked) {
		super.handleGroupStateChanged(group, checked);
		Widget item = fPluginTreeViewer.testFindItem(group);
		if (item instanceof TreeItem) {
			TreeItem[] items = ((TreeItem)item).getItems();
			for (int i = 0; i < items.length; i++) {
				TreeItem child = items[i];
				if (child.getChecked() == (child.getText(1).length() == 0))
					resetText(items[i]);
			}
		}		
	}
	
	protected void handleCheckStateChanged(CheckStateChangedEvent event) {
		super.handleCheckStateChanged(event);
		resetText((IPluginModelBase)event.getElement());
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
			TreeItem[] items = ((TreeItem)item).getItems();
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
			TreeItem item = (TreeItem)widget;
			int index = value == null ? -1 : value.indexOf(':');
			item.setText(1, index == -1 ? "" : value.substring(0, index)); //$NON-NLS-1$
			item.setText(2, index == -1 ? "" : value.substring(index + 1)); //$NON-NLS-1$
		}
	}
	
	private void resetText(IPluginModelBase model) {
		Widget widget = fPluginTreeViewer.testFindItem(model);
		if (widget instanceof TreeItem) {
			resetText((TreeItem)widget);
		}
	}
	
	private void resetText(TreeItem item) {
		if (item.getChecked()) {
			IPluginModelBase model = (IPluginModelBase)item.getData();
			boolean isSystemBundle = "org.eclipse.osgi".equals(model.getPluginBase().getId()); //$NON-NLS-1$
			if (!"default".equals(item.getText(1)))
				item.setText(1, isSystemBundle ? "" : "default"); //$NON-NLS-1$
			if (!"default".equals(item.getText(2)))
				item.setText(2, isSystemBundle ? "" : "default"); //$NON-NLS-1$
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
				resetText((IPluginModelBase)selected[i]);
			}
		}
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		super.setDefaults(config);
		EquinoxLaunchShortcut.initializePluginState(config, 
				PDECore.getDefault().getModelManager().getWorkspaceModels());
	}

}
