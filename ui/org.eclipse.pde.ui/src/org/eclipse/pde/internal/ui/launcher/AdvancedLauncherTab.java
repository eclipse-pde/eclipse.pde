/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class AdvancedLauncherTab
	extends AbstractLauncherTab
	implements ILaunchConfigurationTab, ILauncherSettings {

	private Button fUseDefaultRadio;
	private Button fUseFeaturesRadio;
	private Button fUseListRadio;
	private CheckboxTreeViewer fPluginTreeViewer;
	private Label fVisibleLabel;
	private NamedElement fWorkspacePlugins;
	private NamedElement fExternalPlugins;
	private IPluginModelBase[] fExternalModels;
	private IPluginModelBase[] fWorkspaceModels;
	private Button fDefaultsButton;
	private Button fPluginPathButton;
	private int fNumExternalChecked = 0;
	private int fNumWorkspaceChecked = 0;
	private Image fImage;
	private boolean fShowFeatures = true;

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			return !(parent instanceof IPluginModelBase);
		}
		public Object[] getChildren(Object parent) {
			if (parent == fExternalPlugins)
				return fExternalModels;
			if (parent == fWorkspacePlugins)
				return fWorkspaceModels;
			return new Object[0];
		}
		public Object getParent(Object child) {
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { fWorkspacePlugins, fExternalPlugins };
		}
	}

	public AdvancedLauncherTab() {
		this(true);
	}
	
	public AdvancedLauncherTab(boolean showFeatures) {
		this.fShowFeatures = showFeatures;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
		fExternalModels = PDECore.getDefault().getExternalModelManager().getAllModels();
		fWorkspaceModels = PDECore.getDefault().getWorkspaceModelManager().getAllModels();
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		fImage.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		createStartingSpace(composite, 1);

		fUseDefaultRadio = new Button(composite, SWT.RADIO);
		fUseDefaultRadio.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.useDefault"));

		if (fShowFeatures) {
			fUseFeaturesRadio = new Button(composite, SWT.RADIO);
			fUseFeaturesRadio.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.useFeatures"));
		}

		fUseListRadio = new Button(composite, SWT.RADIO);
		fUseListRadio.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.useList"));

		fVisibleLabel = new Label(composite, SWT.NULL);
		fVisibleLabel.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.visibleList"));

		Control list = createPluginList(composite);
		list.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite buttonContainer = new Composite(composite, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = 10;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		fDefaultsButton = new Button(buttonContainer, SWT.PUSH);
		fDefaultsButton.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.defaults"));
		fDefaultsButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fDefaultsButton);

		fPluginPathButton = new Button(buttonContainer, SWT.PUSH);
		fPluginPathButton.setText(PDEPlugin.getResourceString("AdvancedLauncherTab.pluginPath"));
		fPluginPathButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fPluginPathButton);

		hookListeners();
		setControl(composite);

		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	private void hookListeners() {
		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaultChanged();
			}
		};
		fUseDefaultRadio.addSelectionListener(adapter);
		if (fShowFeatures)
			fUseFeaturesRadio.addSelectionListener(adapter);
		fDefaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				computeInitialCheckState();
				updateStatus();
			}
		});
		fPluginPathButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showPluginPaths();
			}
		});
	}

	private void useDefaultChanged() {
		adjustCustomControlEnableState(fUseListRadio.getSelection());
		if (fShowFeatures)
			fPluginPathButton.setEnabled(!fUseFeaturesRadio.getSelection());
		updateStatus();
	}

	private void adjustCustomControlEnableState(boolean enable) {
		fVisibleLabel.setVisible(enable);
		fPluginTreeViewer.getTree().setVisible(enable);
		fDefaultsButton.setVisible(enable);
	}

	protected Control createPluginList(final Composite parent) {
			fPluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
			fPluginTreeViewer.setContentProvider(new PluginContentProvider());
			fPluginTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
			fPluginTreeViewer.setAutoExpandLevel(2);
			fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
				public void checkStateChanged(final CheckStateChangedEvent event) {
					Object element = event.getElement();
					if (element instanceof IPluginModelBase) {
						handleCheckStateChanged(
							(IPluginModelBase) element,
							event.getChecked());
					} else {
						handleGroupStateChanged(element, event.getChecked());
					}
					updateLaunchConfigurationDialog();
				}
			});
			fPluginTreeViewer.setSorter(new ListUtil.PluginSorter() {
				public int category(Object obj) {
					if (obj == fWorkspacePlugins)
						return -1;
					return 0;
				}
			});
	
			Image pluginsImage =
				PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
	
			fWorkspacePlugins =
				new NamedElement(
					PDEPlugin.getResourceString("AdvancedLauncherTab.workspacePlugins"),
					pluginsImage);
			fExternalPlugins =
				new NamedElement(
					PDEPlugin.getResourceString("AdvancedLauncherTab.externalPlugins"),
					pluginsImage);
			return fPluginTreeViewer.getTree();
		}


	private void initWorkspacePluginsState(ILaunchConfiguration config)
		throws CoreException {
		fNumWorkspaceChecked = fWorkspaceModels.length;
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, true);

		TreeSet deselected = LauncherUtils.parseDeselectedWSIds(config);
		for (int i = 0; i < fWorkspaceModels.length; i++) {
			if (deselected.contains(fWorkspaceModels[i].getPluginBase().getId())) {
				if (fPluginTreeViewer.setChecked(fWorkspaceModels[i], false))
					fNumWorkspaceChecked -= 1;
			}
		}

		if (fNumWorkspaceChecked == 0)
			fPluginTreeViewer.setChecked(fWorkspacePlugins, false);
		fPluginTreeViewer.setGrayed(
			fWorkspacePlugins,
			fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	private void initExternalPluginsState(ILaunchConfiguration config)
		throws CoreException {
		fNumExternalChecked = 0;

		TreeSet selected = LauncherUtils.parseSelectedExtIds(config);
		for (int i = 0; i < fExternalModels.length; i++) {
			if (selected.contains(fExternalModels[i].getPluginBase().getId())) {
				if (fPluginTreeViewer.setChecked(fExternalModels[i], true))
					fNumExternalChecked += 1;
			}
		}

		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(
			fExternalPlugins,
			fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration config) {
		try {
			fUseDefaultRadio.setSelection(config.getAttribute(USECUSTOM, true));
			if (fShowFeatures) {
				fUseFeaturesRadio.setSelection(config.getAttribute(USEFEATURES, false));
				fUseListRadio.setSelection(
					!fUseDefaultRadio.getSelection() && !fUseFeaturesRadio.getSelection());
			} else {
				fUseListRadio.setSelection(!fUseDefaultRadio.getSelection());
			}
			if (fPluginTreeViewer.getInput() == null) {
				fPluginTreeViewer.setUseHashlookup(true);
				fPluginTreeViewer.setInput(PDEPlugin.getDefault());
				fPluginTreeViewer.reveal(fWorkspacePlugins);
			}

			if (fUseDefaultRadio.getSelection()) {
				computeInitialCheckState();
			} else if (fUseListRadio.getSelection()) {
				initWorkspacePluginsState(config);
				initExternalPluginsState(config);
			} else {
				fPluginPathButton.setEnabled(false);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		adjustCustomControlEnableState(fUseListRadio.getSelection());
		updateStatus();
	}

	private void computeInitialCheckState() {
		TreeSet wtable = new TreeSet();
		fNumWorkspaceChecked = 0;
		fNumExternalChecked = 0;

		for (int i = 0; i < fWorkspaceModels.length; i++) {
			IPluginModelBase model = fWorkspaceModels[i];
			fNumWorkspaceChecked += 1;
			String id = model.getPluginBase().getId();
			if (id != null)
				wtable.add(model.getPluginBase().getId());
		}

		if (fNumWorkspaceChecked > 0) {
			fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, true);
		}

		fNumExternalChecked = 0;
		for (int i = 0; i < fExternalModels.length; i++) {
			IPluginModelBase model = fExternalModels[i];
			boolean masked = wtable.contains(model.getPluginBase().getId());
			if (!masked && model.isEnabled()) {
				fPluginTreeViewer.setChecked(model, true);
				fNumExternalChecked += 1;
			}
		}

		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(
			fExternalPlugins,
			fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
	}

	private void handleCheckStateChanged(IPluginModelBase model, boolean checked) {
		if (model.getUnderlyingResource() == null) {
			if (checked) {
				fNumExternalChecked += 1;
			} else {
				fNumExternalChecked -= 1;
			}
			fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
			fPluginTreeViewer.setGrayed(
				fExternalPlugins,
				fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
		} else {
			if (checked) {
				fNumWorkspaceChecked += 1;
			} else {
				fNumWorkspaceChecked -= 1;
			}
			fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
			fPluginTreeViewer.setGrayed(
				fWorkspacePlugins,
				fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
		}
	}

	private void handleGroupStateChanged(Object group, boolean checked) {
		fPluginTreeViewer.setSubtreeChecked(group, checked);
		fPluginTreeViewer.setGrayed(group, false);

		if (group == fWorkspacePlugins)
			fNumWorkspaceChecked = checked ? fWorkspaceModels.length : 0;
		else if (group == fExternalPlugins)
			fNumExternalChecked = checked ? fExternalModels.length : 0;

	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(USECUSTOM, true);
		if (fShowFeatures)
			config.setAttribute(USEFEATURES, false);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		final ILaunchConfigurationWorkingCopy config = configuration;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				config.setAttribute(USECUSTOM, fUseDefaultRadio.getSelection());
				if (fShowFeatures)
					config.setAttribute(USEFEATURES, fUseFeaturesRadio.getSelection());
				if (fUseListRadio.getSelection()) {
					// store deselected projects
					StringBuffer wbuf = new StringBuffer();
					for (int i = 0; i < fWorkspaceModels.length; i++) {
						IPluginModelBase model = (IPluginModelBase) fWorkspaceModels[i];
						if (!fPluginTreeViewer.getChecked(model))
							wbuf.append(
								model.getPluginBase().getId() + File.pathSeparatorChar);
					}
					config.setAttribute(WSPROJECT, wbuf.toString());

					// Store selected external models
					StringBuffer exbuf = new StringBuffer();
					Object[] checked = fPluginTreeViewer.getCheckedElements();
					for (int i = 0; i < checked.length; i++) {
						if (checked[i] instanceof ExternalPluginModelBase) {
							IPluginModelBase model = (IPluginModelBase) checked[i];
							exbuf.append(
								model.getPluginBase().getId() + File.pathSeparatorChar);
						}
					}
					config.setAttribute(EXTPLUGINS, exbuf.toString());
				} else {
					config.setAttribute(WSPROJECT, (String)null);
					config.setAttribute(EXTPLUGINS, (String)null);
				}
			}
		});
	}

	private void showPluginPaths() {
		try {
			URL[] urls = TargetPlatform.createPluginPath(getPlugins());
			PluginPathDialog dialog =
				new PluginPathDialog(fPluginPathButton.getShell(), urls);
			dialog.create();
			dialog.getShell().setText(PDEPlugin.getResourceString("AdvancedLauncherTab.pluginPath.title"));
			dialog.getShell().setSize(500, 500);
			dialog.open();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void updateStatus() {
		updateStatus(validatePlugins());
	}

	private IStatus validatePlugins() {
		if (fShowFeatures && fUseFeaturesRadio.getSelection()) {
			IPath workspacePath = PDEPlugin.getWorkspace().getRoot().getLocation();
			IPath featurePath = workspacePath.removeLastSegments(1).append("features");
			if (!workspacePath.lastSegment().equalsIgnoreCase("plugins")
				|| !featurePath.toFile().exists())
				return createStatus(
					IStatus.ERROR,
					PDEPlugin.getResourceString("AdvancedLauncherTab.error.featureSetup"));
		} 
		return createStatus(IStatus.OK, "");
	}

	private IPluginModelBase[] getPlugins() {
		if (fUseDefaultRadio.getSelection()) {
			TreeMap map = new TreeMap();
			for (int i = 0; i < fWorkspaceModels.length; i++) {
				// check for null is to accomodate previous unclean exits (e.g. workspace crashes)
				String id = fWorkspaceModels[i].getPluginBase().getId();
				if (id != null)
					map.put(id, fWorkspaceModels[i]);
			}
			for (int i = 0; i < fExternalModels.length; i++) {
				String id = fExternalModels[i].getPluginBase().getId();
				if (id != null && !map.containsKey(id) && fExternalModels[i].isEnabled())
					map.put(id, fExternalModels[i]);
			}
			return (IPluginModelBase[]) map.values().toArray(
				new IPluginModelBase[map.size()]);
		}

		ArrayList result = new ArrayList();
		Object[] elements = fPluginTreeViewer.getCheckedElements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IPluginModelBase)
				result.add(elements[i]);
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public String getName() {
		return PDEPlugin.getResourceString("AdvancedLauncherTab.name");
	}
	
	public Image getImage() {
			return fImage;
	}
}
