/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.shared.target.*;
import org.eclipse.pde.internal.ui.util.LocaleUtil;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.UIJob;

/**
 * Wizard page for editing the content of a target platform using a tab layout
 * 
 * @see NewTargetDefinitionWizard2
 * @see EditTargetDefinitionWizard
 */
public class TargetDefinitionContentPage extends TargetDefinitionPage {

	private Text fNameText;
	private BundleContainerTable fTable;

	// Environment pull-downs
	private Combo fOSCombo;
	private Combo fWSCombo;
	private Combo fArchCombo;
	private Combo fNLCombo;

	// Choices for each pull-down
	private TreeSet fNLChoices;
	private TreeSet fOSChoices;
	private TreeSet fWSChoices;
	private TreeSet fArchChoices;

	// JRE section
	private Button fDefaultJREButton;
	private Button fNamedJREButton;
	private Button fExecEnvButton;
	private Combo fNamedJREsCombo;
	private Combo fExecEnvsCombo;
	private TreeSet fExecEnvChoices;

	// argument controls
	private Text fProgramArgs;
	private Text fVMArgs;

	// implicit dependencies tab
	private TableViewer fElementViewer;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;

	/**
	 * @param pageName
	 */
	public TargetDefinitionContentPage(ITargetDefinition target) {
		super("targetContent", target); //$NON-NLS-1$
		setTitle(PDEUIMessages.TargetDefinitionContentPage_1);
		setDescription(PDEUIMessages.TargetDefinitionContentPage_2);
		setImageDescriptor(PDEPluginImages.DESC_TARGET_WIZ);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);

		Composite nameComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_HORIZONTAL, 0, 0);

		SWTFactory.createLabel(nameComp, PDEUIMessages.TargetDefinitionContentPage_4, 1);

		fNameText = SWTFactory.createSingleText(nameComp, 1);
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setName(fNameText.getText().trim());
			}
		});

		TabFolder tabs = new TabFolder(comp, SWT.NONE);
		tabs.setLayoutData(new GridData(GridData.FILL_BOTH));

		TabItem pluginsTab = new TabItem(tabs, SWT.NONE);
		pluginsTab.setText(PDEUIMessages.TargetDefinitionContentPage_6);
		Composite pluginTabContainer = new Composite(tabs, SWT.NONE);
		pluginTabContainer.setLayout(new GridLayout());
		pluginTabContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = SWTFactory.createWrapLabel(pluginTabContainer, PDEUIMessages.ContentSection_1, 2);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		label.setLayoutData(gd);

		fTable = BundleContainerTable.createTableInDialog(pluginTabContainer, new IBundleContainerTableReporter() {
			public void runResolveOperation(final IRunnableWithProgress operation) {
				if (isControlCreated()) {
					try {
						getContainer().run(true, false, operation);
					} catch (InvocationTargetException e) {
						PDEPlugin.log(e);
					} catch (InterruptedException e) {
						// TODO Cancel the wizard?
					}
				} else {
					// If the page isn't open yet, try running a UI job so the dialog has time to finish opening
					new UIJob(PDEUIMessages.TargetDefinitionContentPage_0) {
						public IStatus runInUIThread(IProgressMonitor monitor) {
							try {
								getContainer().run(true, false, operation);
								return Status.OK_STATUS;
							} catch (InvocationTargetException e) {
								return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), PDEUIMessages.TargetDefinitionContentPage_5, e);
							} catch (InterruptedException e) {
								return Status.CANCEL_STATUS;
							}
						}
					}.schedule();
				}
			}

			public void contentsChanged() {
				// Do nothing, as wizard will always save when finish is pressed
			}
		});
		pluginsTab.setControl(pluginTabContainer);

		TabItem envTab = new TabItem(tabs, SWT.NONE);
		envTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_3);
		Composite envTabContainer = new Composite(tabs, SWT.NONE);
		envTabContainer.setLayout(new GridLayout());
		envTabContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		createTargetEnvironmentGroup(envTabContainer);
		createJREGroup(envTabContainer);
		envTab.setControl(envTabContainer);

		TabItem argsTab = new TabItem(tabs, SWT.NONE);
		argsTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_4);
		argsTab.setControl(createArgumentsGroup(tabs));

		TabItem depTab = new TabItem(tabs, SWT.NONE);
		depTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_5);
		depTab.setControl(createImplicitTabContents(tabs));

		targetChanged(getTargetDefinition());
		setControl(comp);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionPage#targetChanged()
	 */
	protected void targetChanged(ITargetDefinition definition) {
		super.targetChanged(definition);
		if (definition != null) {
			String name = definition.getName();
			if (name == null) {
				name = ""; //$NON-NLS-1$
			}
			fNameText.setText(name);
			fTable.setInput(definition);

			String presetValue = (definition.getOS() == null) ? "" : definition.getOS(); //$NON-NLS-1$
			fOSCombo.setText(presetValue);
			presetValue = (definition.getWS() == null) ? "" : definition.getWS(); //$NON-NLS-1$
			fWSCombo.setText(presetValue);
			presetValue = (definition.getArch() == null) ? "" : definition.getArch(); //$NON-NLS-1$
			fArchCombo.setText(presetValue);
			presetValue = (definition.getNL() == null) ? "" : LocaleUtil.expandLocaleName(definition.getNL()); //$NON-NLS-1$
			fNLCombo.setText(presetValue);

			IPath jrePath = definition.getJREContainer();
			if (jrePath == null || jrePath.equals(JavaRuntime.newDefaultJREContainerPath())) {
				fDefaultJREButton.setSelection(true);
			} else {
				String ee = JavaRuntime.getExecutionEnvironmentId(jrePath);
				if (ee != null) {
					fExecEnvButton.setSelection(true);
					fExecEnvsCombo.select(fExecEnvsCombo.indexOf(ee));
				} else {
					String vm = JavaRuntime.getVMInstallName(jrePath);
					if (vm != null) {
						fNamedJREButton.setSelection(true);
						fNamedJREsCombo.select(fNamedJREsCombo.indexOf(vm));
					}
				}
			}

			if (fExecEnvsCombo.getSelectionIndex() == -1)
				fExecEnvsCombo.setText(fExecEnvChoices.first().toString());

			if (fNamedJREsCombo.getSelectionIndex() == -1)
				fNamedJREsCombo.setText(VMUtil.getDefaultVMInstallName());

			updateJREWidgets();

			presetValue = (definition.getProgramArguments() == null) ? "" : definition.getProgramArguments(); //$NON-NLS-1$
			fProgramArgs.setText(presetValue);
			presetValue = (definition.getVMArguments() == null) ? "" : definition.getVMArguments(); //$NON-NLS-1$
			fVMArgs.setText(presetValue);

			fElementViewer.refresh();
		}
	}

	private void createTargetEnvironmentGroup(Composite container) {
		Group group = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.EnvironmentBlock_targetEnv);

		initializeChoices();

		Label label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_os);

		fOSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fOSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fOSCombo.setItems((String[]) fOSChoices.toArray(new String[fOSChoices.size()]));
		fOSCombo.setVisibleItemCount(30);
		fOSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setOS(getModelValue(fOSCombo.getText()));
			}
		});

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_ws);

		fWSCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fWSCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWSCombo.setItems((String[]) fWSChoices.toArray(new String[fWSChoices.size()]));
		fWSCombo.setVisibleItemCount(30);
		fWSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setWS(getModelValue(fWSCombo.getText()));
			}
		});

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_arch);

		fArchCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fArchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArchCombo.setItems((String[]) fArchChoices.toArray(new String[fArchChoices.size()]));
		fArchCombo.setVisibleItemCount(30);
		fArchCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setArch(getModelValue(fArchCombo.getText()));
			}
		});

		label = new Label(group, SWT.NULL);
		label.setText(PDEUIMessages.Preferences_TargetEnvironmentPage_nl);

		fNLCombo = new Combo(group, SWT.SINGLE | SWT.BORDER);
		fNLCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fNLCombo.setItems((String[]) fNLChoices.toArray(new String[fNLChoices.size()]));
		fNLCombo.setVisibleItemCount(30);
		fNLCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String value = fNLCombo.getText();
				int index = value.indexOf("-"); //$NON-NLS-1$
				if (index > 0)
					value = value.substring(0, index);
				getTargetDefinition().setNL(getModelValue(value));
			}
		});

	}

	/**
	 * Returns the given string or <code>null</code> if the string is empty.
	 * Used when setting a value in the target definition.
	 * 
	 * @param value
	 * @return given string or <code>null</code>
	 */
	private String getModelValue(String value) {
		if (value != null) {
			value = value.trim();
			if (value.length() == 0) {
				return null;
			}
		}
		return value;
	}

	private void addExtraChoices(Set set, String preference) {
		StringTokenizer tokenizer = new StringTokenizer(preference, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			set.add(tokenizer.nextToken().trim());
		}
	}

	private void initializeChoices() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();

		fOSChoices = new TreeSet();
		String[] os = Platform.knownOSValues();
		for (int i = 0; i < os.length; i++)
			fOSChoices.add(os[i]);
		addExtraChoices(fOSChoices, preferences.getString(ICoreConstants.OS_EXTRA));

		fWSChoices = new TreeSet();
		String[] ws = Platform.knownWSValues();
		for (int i = 0; i < ws.length; i++)
			fWSChoices.add(ws[i]);
		addExtraChoices(fWSChoices, preferences.getString(ICoreConstants.WS_EXTRA));

		fArchChoices = new TreeSet();
		String[] arch = Platform.knownOSArchValues();
		for (int i = 0; i < arch.length; i++)
			fArchChoices.add(arch[i]);
		addExtraChoices(fArchChoices, preferences.getString(ICoreConstants.ARCH_EXTRA));

		fNLChoices = new TreeSet();
		initializeAllLocales();
	}

	private void initializeAllLocales() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String[] nl = LocaleUtil.getLocales();
		for (int i = 0; i < nl.length; i++)
			fNLChoices.add(nl[i]);
		addExtraChoices(fNLChoices, preferences.getString(ICoreConstants.NL_EXTRA));
	}

	private void createJREGroup(Composite container) {
		Group group = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setText(PDEUIMessages.EnvironmentBlock_jreTitle);

		initializeJREValues();

		Label label = new Label(group, SWT.WRAP);
		label.setText(PDEUIMessages.JRESection_description);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalAlignment = SWT.TOP;
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		fDefaultJREButton = new Button(group, SWT.RADIO);
		fDefaultJREButton.setText(PDEUIMessages.JRESection_defaultJRE);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fDefaultJREButton.setLayoutData(gd);
		fDefaultJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newDefaultJREContainerPath());
			}
		});

		fNamedJREButton = new Button(group, SWT.RADIO);
		fNamedJREButton.setText(PDEUIMessages.JRESection_JREName);
		fNamedJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getText())));
			}
		});

		fNamedJREsCombo = new Combo(group, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fNamedJREsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String[] installs = VMUtil.getVMInstallNames();
		fNamedJREsCombo.setItems(installs);
		fNamedJREsCombo.setVisibleItemCount(30);
		fNamedJREsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getText())));
			}
		});

		fExecEnvButton = new Button(group, SWT.RADIO);
		fExecEnvButton.setText(PDEUIMessages.JRESection_ExecutionEnv);
		fExecEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getText())));
			}
		});

		fExecEnvsCombo = new Combo(group, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		fExecEnvsCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fExecEnvsCombo.setItems((String[]) fExecEnvChoices.toArray(new String[fExecEnvChoices.size()]));
		fExecEnvsCombo.setVisibleItemCount(30);
		fExecEnvsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getText())));
			}
		});

	}

	/**
	 * Initializes the combo with possible execution enviroments
	 */
	protected void initializeJREValues() {
		fExecEnvChoices = new TreeSet();
		IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
		IExecutionEnvironment[] envs = manager.getExecutionEnvironments();
		for (int i = 0; i < envs.length; i++)
			fExecEnvChoices.add(envs[i].getId());
	}

	protected void updateJREWidgets() {
		fNamedJREsCombo.setEnabled(fNamedJREButton.getSelection());
		fExecEnvsCombo.setEnabled(fExecEnvButton.getSelection());
	}

	private Control createArgumentsGroup(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		SWTFactory.createWrapLabel(container, PDEUIMessages.JavaArgumentsTab_description, 1);

		Group programGroup = new Group(container, SWT.NONE);
		programGroup.setLayout(new GridLayout());
		programGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		programGroup.setText(PDEUIMessages.JavaArgumentsTab_progamArgsGroup);

		fProgramArgs = new Text(programGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		gd.heightHint = 60;
		fProgramArgs.setLayoutData(gd);
		fProgramArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setProgramArguments(fProgramArgs.getText().trim());
			}
		});

		Button programVars = new Button(programGroup, SWT.NONE);
		programVars.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		programVars.setText(PDEUIMessages.JavaArgumentsTab_programVariables);
		programVars.addSelectionListener(getVariablesListener(fProgramArgs));

		Group vmGroup = new Group(container, SWT.NONE);
		vmGroup.setLayout(new GridLayout(1, false));
		vmGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		vmGroup.setText(PDEUIMessages.JavaArgumentsTab_vmArgsGroup);

		fVMArgs = new Text(vmGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		gd.heightHint = 60;
		fVMArgs.setLayoutData(gd);
		fVMArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setVMArguments(fVMArgs.getText().trim());
			}
		});

		Composite buttons = new Composite(vmGroup, SWT.NONE);
		buttons.setLayout(new GridLayout(2, false));
		buttons.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		Button vmArgs = SWTFactory.createPushButton(buttons, PDEUIMessages.JavaArgumentsTab_addVMArgs, null, GridData.HORIZONTAL_ALIGN_END);
		vmArgs.addSelectionListener(getVMArgsListener(fVMArgs));

		Button vmVars = SWTFactory.createPushButton(buttons, PDEUIMessages.JavaArgumentsTab_vmVariables, null, GridData.HORIZONTAL_ALIGN_END);
		vmVars.addSelectionListener(getVariablesListener(fVMArgs));

		return container;
	}

	/**
	 * Provide a listener for the Add VM Arguments button.
	 * The listener invokes the <code>VMArgumentsSelectionDialog</code> and 
	 * updates the selected VM Arguments back in the VM Arguments Text Box
	 * 
	 * @param textControl
	 * @return	<code>SelectionListener</code> for the Add VM Arguments button
	 */
	private SelectionListener getVMArgsListener(final Text textControl) {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ArgumentsFromContainerSelectionDialog dialog = new ArgumentsFromContainerSelectionDialog(getShell(), getTargetDefinition());
				if (dialog.open() == Dialog.OK) {
					String[] args = dialog.getSelectedArguments();
					if (args != null && args.length > 0) {
						StringBuffer resultBuffer = new StringBuffer();
						for (int index = 0; index < args.length; ++index) {
							resultBuffer.append(args[index] + " "); //$NON-NLS-1$
						}
						fVMArgs.insert(resultBuffer.toString());
					}
				}
			}
		};
	}

	/**
	 * Provide a listener for the Variables button.
	 * The listener invokes the <code>StringVariableSelectionDialog</code> and 
	 * updates the selected Variables back in the VM Arguments Text Box
	 * 
	 * @param textControl
	 * @return	<code>SelectionListener</code> for the Variables button
	 */
	private SelectionListener getVariablesListener(final Text textControl) {
		return new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					textControl.insert(variable);
				}
			}
		};
	}

	private Control createImplicitTabContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createImpLabel(container);
		createImpTable(container);
		createImpButtons(container);
		// TODO: PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.IMPLICIT_PLUGINS_PREFERENCE_PAGE);
		return container;
	}

	private void createImpLabel(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.TargetImplicitPluginsTab_desc);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
	}

	private void createImpTable(Composite container) {
		fElementViewer = new TableViewer(container, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fElementViewer.getControl().setLayoutData(gd);
		fElementViewer.setContentProvider(new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				ITargetDefinition target = getTargetDefinition();
				if (target != null) {
					BundleInfo[] bundles = target.getImplicitDependencies();
					if (bundles != null) {
						return bundles;
					}
				}
				return new BundleInfo[0];
			}
		});
		fElementViewer.setLabelProvider(new BundleInfoLabelProvider(false));
		fElementViewer.setInput(PDEPlugin.getDefault());
		fElementViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				BundleInfo bundle1 = (BundleInfo) e1;
				BundleInfo bundle2 = (BundleInfo) e2;
				return super.compare(viewer, bundle1.getSymbolicName(), bundle2.getSymbolicName());
			}
		});
		fElementViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateImpButtons();
			}
		});
		fElementViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL && e.stateMask == 0) {
					handleRemove();
				}
			}
		});

	}

	private void createImpButtons(Composite container) {
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = new Button(buttonContainer, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.SourceBlock_add);
		fAddButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		fRemoveButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.SourceBlock_remove);
		fRemoveButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});

		fRemoveAllButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveAllButton.setText(PDEUIMessages.TargetImplicitPluginsTab_removeAll3);
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveAllButton);
		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		updateImpButtons();
	}

	protected void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new BundleInfoLabelProvider(false));

		try {
			dialog.setElements(getValidBundles());
		} catch (CoreException e) {
			dialog.setMessage(e.getMessage());
		}

		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title);
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {

			Object[] models = dialog.getResult();
			ArrayList pluginsToAdd = new ArrayList();
			for (int i = 0; i < models.length; i++) {
				BundleInfo desc = ((BundleInfo) models[i]);
				pluginsToAdd.add(new BundleInfo(desc.getSymbolicName(), null, null, BundleInfo.NO_LEVEL, false));
			}
			Set allDependencies = new HashSet();
			allDependencies.addAll(pluginsToAdd);
			BundleInfo[] currentBundles = getTargetDefinition().getImplicitDependencies();
			if (currentBundles != null) {
				allDependencies.addAll(Arrays.asList(currentBundles));
			}
			getTargetDefinition().setImplicitDependencies((BundleInfo[]) allDependencies.toArray(new BundleInfo[allDependencies.size()]));
			fElementViewer.refresh();
		}
	}

	/**
	 * Gets a list of all the bundles that can be added as implicit dependencies
	 * @return list of possible dependencies
	 */
	protected BundleInfo[] getValidBundles() throws CoreException {
		BundleInfo[] current = getTargetDefinition().getImplicitDependencies();
		Set currentBundles = new HashSet();
		if (current != null) {
			for (int i = 0; i < current.length; i++) {
				currentBundles.add(current[i].getSymbolicName());
			}
		}

		List targetBundles = new ArrayList();
		IResolvedBundle[] allTargetBundles = getTargetDefinition().getBundles();
		if (allTargetBundles == null || allTargetBundles.length == 0) {
			throw new CoreException(new Status(IStatus.WARNING, PDEPlugin.getPluginId(), PDEUIMessages.ImplicitDependenciesSection_0));
		}
		for (int i = 0; i < allTargetBundles.length; i++) {
			if (!currentBundles.contains(allTargetBundles[i].getBundleInfo().getSymbolicName())) {
				targetBundles.add(allTargetBundles[i].getBundleInfo());
			}
		}

		return (BundleInfo[]) targetBundles.toArray(new BundleInfo[targetBundles.size()]);
	}

	private void handleRemove() {
		LinkedList bundles = new LinkedList();
		bundles.addAll(Arrays.asList(getTargetDefinition().getImplicitDependencies()));
		Object[] removeBundles = ((IStructuredSelection) fElementViewer.getSelection()).toArray();
		if (removeBundles.length > 0) {
			for (int i = 0; i < removeBundles.length; i++) {
				if (removeBundles[i] instanceof BundleInfo) {
					bundles.remove(removeBundles[i]);
				}
			}
			getTargetDefinition().setImplicitDependencies((BundleInfo[]) bundles.toArray((new BundleInfo[bundles.size()])));
			fElementViewer.refresh();
		}
	}

	private void handleRemoveAll() {
		getTargetDefinition().setImplicitDependencies(null);
		fElementViewer.refresh();
	}

	private void updateImpButtons() {
		boolean empty = fElementViewer.getSelection().isEmpty();
		fRemoveButton.setEnabled(!empty);
		boolean hasElements = fElementViewer.getTable().getItemCount() > 0;
		fRemoveAllButton.setEnabled(hasElements);
	}
}
