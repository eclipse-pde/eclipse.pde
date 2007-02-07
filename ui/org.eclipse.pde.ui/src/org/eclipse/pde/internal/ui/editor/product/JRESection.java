/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.TreeSet;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.IJREInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.launcher.VMHelper;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class JRESection extends PDESection {

	private Button fJRERadioButton;
	private Button fEERadioButton;
	private Button fInstalledJREsButton;
	private Button fExecutionEnvironmentsButton;
	private ComboPart fJREsCombo;
	private ComboPart fEEsCombo;
	private TreeSet fEEChoices;
	private boolean fBlockChanges;

	private static final String[] TAB_LABELS = new String[4];
	static {
		TAB_LABELS[IJREInfo.LINUX] = "linux"; //$NON-NLS-1$
		TAB_LABELS[IJREInfo.MACOS] = "macosx"; //$NON-NLS-1$
		TAB_LABELS[IJREInfo.SOLAR] = "solaris"; //$NON-NLS-1$
		TAB_LABELS[IJREInfo.WIN32] = "win32"; //$NON-NLS-1$
	}

	private CTabFolder fTabFolder;
	private int fLastTab;

	public JRESection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		section.setLayoutData(data);		
		
		section.setText(PDEUIMessages.ProductJRESection_title); 
		section.setDescription(PDEUIMessages.ProductJRESection_desc); 

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		initializeValues();
		
		fTabFolder = new CTabFolder(client, SWT.FLAT | SWT.TOP);
		toolkit.adapt(fTabFolder, true, true);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		fTabFolder.setLayoutData(gd);
		gd.heightHint = 2;
		gd.horizontalSpan = 3;
		gd.grabExcessHorizontalSpace = true;
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] { selectedColor,
				toolkit.getColors().getBackground() },
				new int[] { 100 }, true);

		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {	
				refresh();
			}
		});
		fTabFolder.setUnselectedImageVisible(false);

		fJRERadioButton = toolkit.createButton(client, PDEUIMessages.ProductJRESection_jreName, SWT.RADIO);
		fJRERadioButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateWidgets();
				getJVMLocations().setJVM(fJREsCombo.getSelection(), fLastTab, IJREInfo.TYPE_JRE);
			}
		});

		fJREsCombo = new ComboPart();
		fJREsCombo.createControl(client, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fJREsCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String[] installs = VMHelper.getVMInstallNames();
		fJREsCombo.setItems(installs);
		fJREsCombo.add("", 0); //$NON-NLS-1$
		fJREsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if(!fBlockChanges)
					getJVMLocations().setJVM(fJREsCombo.getSelection(), fLastTab, IJREInfo.TYPE_JRE);
			}
		});

		fInstalledJREsButton = toolkit.createButton(client, PDEUIMessages.ProductJRESection_browseJREs, SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(fInstalledJREsButton);
		fInstalledJREsButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				PreferencesUtil.createPreferenceDialogOn(
						getSection().getShell(), 
						"org.eclipse.jdt.debug.ui.preferences.VMPreferencePage", //$NON-NLS-1$
						new String[] { "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage" }, null).open(); //$NON-NLS-1$ 
			}
		});

		fEERadioButton = toolkit.createButton(client, PDEUIMessages.ProductJRESection_eeName, SWT.RADIO);
		fEERadioButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateWidgets();
				getJVMLocations().setJVM(fEEsCombo.getSelection(), fLastTab, IJREInfo.TYPE_EE);
			}
		});

		fEEsCombo = new ComboPart();
		fEEsCombo.createControl(client, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fEEsCombo.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fEEsCombo.setItems((String[])fEEChoices.toArray(new String[fEEChoices.size()]));

		fEEsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if(!fBlockChanges)
					getJVMLocations().setJVM(fEEsCombo.getSelection(), fLastTab, IJREInfo.TYPE_EE);
			}
		});
		
		fExecutionEnvironmentsButton = toolkit.createButton(client, PDEUIMessages.ProductJRESection_browseEEs, SWT.PUSH);
		GridDataFactory.fillDefaults().applyTo(fExecutionEnvironmentsButton);
		fExecutionEnvironmentsButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				PreferencesUtil.createPreferenceDialogOn(
						getSection().getShell(), 
						"org.eclipse.jdt.debug.ui.jreProfiles", //$NON-NLS-1$
						new String[] { "org.eclipse.jdt.debug.ui.jreProfiles" }, null).open(); //$NON-NLS-1$ 
			}
		});

		createTabs();
		toolkit.paintBordersFor(client);
		section.setClient(client);	
	}

	private void createTabs() {
		for (int i = 0; i < TAB_LABELS.length; i++) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(TAB_LABELS[i]);
			item.setImage(PDEPlugin.getDefault().getLabelProvider().get(
					PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ));
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);

		String currentTarget = TargetPlatform.getOS();

		if (Platform.OS_WIN32.equals(currentTarget)) {
			fTabFolder.setSelection(3);
		} else if (Platform.OS_MACOSX.equals(currentTarget)) {
			fTabFolder.setSelection(1);
		} else if (Platform.OS_SOLARIS.equals(currentTarget)) {
			fTabFolder.setSelection(2);
		} 
	}

	public void refresh() {
		fBlockChanges = true;
		fLastTab = fTabFolder.getSelectionIndex();
		int type = getJVMLocations().getJVMType(fLastTab);
		String name = getJVMLocations().getJVM(fLastTab);
		switch(type) {
		case IJREInfo.TYPE_JRE:
			if (fJREsCombo.indexOf(name) < 0)
				fJREsCombo.add(name);
			fJREsCombo.setText(name);
			fJRERadioButton.setSelection(true);
			fEERadioButton.setSelection(false);
			break;
		case IJREInfo.TYPE_EE:
			if (fEEsCombo.indexOf(name) < 0)
				fEEsCombo.add(name);
			fEEsCombo.setText(name);
			fEERadioButton.setSelection(true);
			fJRERadioButton.setSelection(false);
			break;
		}
		updateWidgets();
		super.refresh();
		fBlockChanges = false;
	}

	private IJREInfo getJVMLocations() {
		IJREInfo info = getProduct().getJREInfo();
		if (info == null) {
			info = getModel().getFactory().createJVMInfo();
			getProduct().setJREInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		return d.getFocusControl() instanceof Text;
	}

	protected void updateWidgets() {
		fJREsCombo.setEnabled(fJRERadioButton.getSelection());
		fEEsCombo.setEnabled(fEERadioButton.getSelection());
	}

	protected void initializeValues() {
		fEEChoices = new TreeSet();
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		for (int i = 0; i < envs.length; i++)
			fEEChoices.add(envs[i].getId()); 
	}

}
