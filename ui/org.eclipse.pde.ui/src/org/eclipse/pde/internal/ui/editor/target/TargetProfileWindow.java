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
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.IArgumentsInfo;
import org.eclipse.pde.internal.core.itarget.IEnvironmentInfo;
import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetJRE;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class TargetProfileWindow extends ApplicationWindow {

	protected ITargetModel fTargetModel;

	public TargetProfileWindow(Shell parentShell, ITargetModel model) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.APPLICATION_MODAL);
		fTargetModel = model;
 	}
	
	public void create() {
		super.create();
		getShell().setText(PDEUIMessages.TargetProfileWindow_title);
		getShell().setSize(500, 300);
	}
	
	protected Control createContents(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		CTabFolder folder = new CTabFolder(parent, SWT.NONE);
		folder.setLayout(new GridLayout());
		toolkit.adapt(folder, true, true);
		toolkit.adapt(parent);
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor1 = toolkit.getColors().getColor(FormColors.TB_BG);
		Color selectedColor2 = toolkit.getColors().getColor(FormColors.TB_GBG);
		folder.setSelectionBackground(new Color[] { selectedColor1,
				selectedColor2, toolkit.getColors().getBackground() },
				new int[] { 50, 100 }, true);
		
		CTabItem item = new CTabItem(folder, SWT.NONE);
		item.setControl(createDefinitionTab(folder, toolkit));
		item.setText(PDEUIMessages.TargetProfileWindow_definition);
		
		ITarget target = fTargetModel.getTarget();

		ITargetPlugin[] plugins = target.getPlugins();
		if (!target.useAllPlugins() && plugins.length > 0) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createTabularTab(folder, toolkit, plugins));
			item.setText(PDEUIMessages.TargetProfileWindow_plugins);
		}
		
		ITargetFeature[] features = target.getFeatures();
		if (!target.useAllPlugins() && features.length > 0) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createTabularTab(folder, toolkit, features));
			item.setText(PDEUIMessages.TargetProfileWindow_features);
		}
		
		item = new CTabItem(folder, SWT.NONE);
		item.setControl(createEnvironmentTab(folder, toolkit));
		item.setText(PDEUIMessages.TargetProfileWindow_environment);
		
		IArgumentsInfo argInfo = target.getArguments();
		if (argInfo != null) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createArgumentsTab(folder, toolkit, argInfo));
			item.setText(PDEUIMessages.TargetProfileWindow_launching);
		}
		
		IImplicitDependenciesInfo info = target.getImplicitPluginsInfo();
		if (info != null) {
			item = new CTabItem(folder, SWT.NONE);
			item.setControl(createTabularTab(folder, toolkit, info.getPlugins()));
			item.setText(PDEUIMessages.TargetProfileWindow_implicit);
		}
		
		return folder;
	}
	
	private Control createDefinitionTab(Composite parent, FormToolkit toolkit) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);
		
		ITarget target = fTargetModel.getTarget();
		createEntry(body, toolkit, PDEUIMessages.TargetDefinitionSection_name, target.getName());
		createEntry(body, toolkit, PDEUIMessages.TargetDefinitionSection_targetLocation, getLocation(target));
		
		IAdditionalLocation[] locs = target.getAdditionalDirectories();
		if (locs.length > 0) {
			Label label = toolkit.createLabel(body, PDEUIMessages.TargetProfileWindow_additionalLocations);
			label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
			label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
			createTable(body, toolkit, locs);
		}
		
		toolkit.paintBordersFor(form.getBody());
		return form;	
	}

	private Control createEnvironmentTab(Composite parent, FormToolkit toolkit) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);
		
		ITarget target = fTargetModel.getTarget();
		IEnvironmentInfo info = target.getEnvironment();
		
		String os = info == null ? Platform.getOS() : info.getDisplayOS();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_operationSystem, os);
		
		String ws = info == null ? Platform.getWS() : info.getDisplayWS();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_windowingSystem, ws);
		
		String arch = info == null ? Platform.getOSArch() : info.getDisplayArch();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_architecture, arch);
		
		String nl = info == null ? Platform.getNL() : info.getDisplayNL();
		createEntry(body, toolkit, PDEUIMessages.EnvironmentSection_locale, nl);
		
		ITargetJRE jreInfo = target.getTargetJREInfo();
		String jre = jreInfo == null ? JavaRuntime.getDefaultVMInstall().getName() : jreInfo.getCompatibleJRE();
		createEntry(body, toolkit, PDEUIMessages.TargetProfileWindow_jre, jre);
		
		toolkit.paintBordersFor(form.getBody());
		return form;	
	}
	
	private Control createTabularTab(Composite parent, FormToolkit toolkit, Object[] objects) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);

		createTable(body, toolkit, objects);
		
		toolkit.paintBordersFor(form.getBody());
		return form;	
	}
	
	private Control createTable(Composite parent, FormToolkit toolkit, Object[] objects) {
		int style = SWT.H_SCROLL | SWT.V_SCROLL | toolkit.getBorderStyle();

		TableViewer	tableViewer = new TableViewer(parent, style);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		tableViewer.setInput(objects);
		tableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		return tableViewer.getControl();
	}

	private Control createArgumentsTab(Composite parent, FormToolkit toolkit, IArgumentsInfo info) {
		ScrolledForm form = toolkit.createScrolledForm(parent);
		Composite body = form.getBody();
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 5;
		body.setLayout(layout);
		
		FormEntry entry = createEntry(body, toolkit, PDEUIMessages.TargetProfileWindow_program, info.getProgramArguments());
		entry.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		entry = createEntry(body, toolkit, PDEUIMessages.TargetProfileWindow_vm, info.getVMArguments());
		entry.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		toolkit.paintBordersFor(form.getBody());
		return form;	
	}
	
	private FormEntry createEntry(Composite client, FormToolkit toolkit, String text, String value) {
		FormEntry entry = new FormEntry(client, toolkit, text, SWT.NONE); 
		entry.setValue(value);
		entry.setEditable(false);
		return entry;
	}
	
	private String getLocation(ITarget target) {
		ILocationInfo info = target.getLocationInfo();
		if (info == null || info.useDefault())
			return ExternalModelManager.computeDefaultPlatformPath();
		try {
			return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(info.getPath());
		} catch (CoreException e) {
		}
		return ""; //$NON-NLS-1$
	}

}
