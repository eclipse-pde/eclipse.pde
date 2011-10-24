/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.pde.core.target.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.shared.target.*;
import org.eclipse.pde.internal.ui.util.LocaleUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.progress.UIJob;

/**
 * Wizard page for editing the content of a target platform using a tab layout
 * 
 * @see NewTargetDefinitionWizard2
 * @see EditTargetDefinitionWizard
 */
public class TargetDefinitionContentPage extends TargetDefinitionPage {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	private Text fNameText;
	private TabItem fLocationTab;
	private TargetLocationsGroup fLocationTree;
	private TargetContentsGroup fContentTree;

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
	 * Wrappers the default progress monitor to avoid opening a dialog if the
	 * operation is blocked.  Instead the blocked message is set as a subtask. 
	 * See bug 276904 [Progress] WizardDialog opens second dialog when blocked
	 */
	class ResolutionProgressMonitor extends ProgressMonitorWrapper {

		ResolutionProgressMonitor(IProgressMonitor monitor) {
			super(monitor);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#setBlocked(org.eclipse.core.runtime.IStatus)
		 */
		public void setBlocked(IStatus reason) {
			subTask(reason.getMessage());
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.ProgressMonitorWrapper#clearBlocked()
		 */
		public void clearBlocked() {
			subTask(""); //$NON-NLS-1$
		}

	}

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
				String name = fNameText.getText().trim();
				if (name.length() == 0) {
					setErrorMessage(PDEUIMessages.TargetDefinitionContentPage_7);
				} else {
					setErrorMessage(null);
					setMessage(PDEUIMessages.TargetDefinitionContentPage_2);
				}
				getTargetDefinition().setName(name);
				setPageComplete(isPageComplete());
			}
		});

		TabFolder tabs = new TabFolder(comp, SWT.NONE);
		tabs.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabs.setFont(comp.getFont());

		fLocationTab = new TabItem(tabs, SWT.NONE);
		fLocationTab.setText(PDEUIMessages.LocationSection_0);

		Composite pluginTabContainer = SWTFactory.createComposite(tabs, 1, 1, GridData.FILL_BOTH);
		SWTFactory.createWrapLabel(pluginTabContainer, PDEUIMessages.TargetDefinitionContentPage_LocationDescription, 2, 400);
		fLocationTree = TargetLocationsGroup.createInDialog(pluginTabContainer);
		fLocationTab.setControl(pluginTabContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(pluginTabContainer, IHelpContextIds.EDIT_TARGET_WIZARD_LOCATIONS_TAB);

		TabItem contentTab = new TabItem(tabs, SWT.NONE);
		contentTab.setText(PDEUIMessages.TargetDefinitionContentPage_6);
		Composite contentTabContainer = SWTFactory.createComposite(tabs, 1, 1, GridData.FILL_BOTH);
		SWTFactory.createWrapLabel(contentTabContainer, PDEUIMessages.ContentSection_1, 2, 400);
		fContentTree = TargetContentsGroup.createInDialog(contentTabContainer);
		contentTab.setControl(contentTabContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(contentTabContainer, IHelpContextIds.EDIT_TARGET_WIZARD_CONTENT_TAB);

		TabItem envTab = new TabItem(tabs, SWT.NONE);
		envTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_3);
		Composite envTabContainer = SWTFactory.createComposite(tabs, 1, 1, GridData.FILL_BOTH);
		createTargetEnvironmentGroup(envTabContainer);
		createJREGroup(envTabContainer);
		envTab.setControl(envTabContainer);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(envTabContainer, IHelpContextIds.EDIT_TARGET_WIZARD_ENVIRONMENT_TAB);

		TabItem argsTab = new TabItem(tabs, SWT.NONE);
		argsTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_4);
		argsTab.setControl(createArgumentsGroup(tabs));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(argsTab.getControl(), IHelpContextIds.EDIT_TARGET_WIZARD_ARGUMENT_TAB);

		TabItem depTab = new TabItem(tabs, SWT.NONE);
		depTab.setText(PDEUIMessages.TargetDefinitionEnvironmentPage_5);
		depTab.setControl(createImplicitTabContents(tabs));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(depTab.getControl(), IHelpContextIds.EDIT_TARGET_WIZARD_IMPLICIT_TAB);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IHelpContextIds.EDIT_TARGET_WIZARD);
		initializeListeners();
		targetChanged(getTargetDefinition());
		setControl(comp);
	}

	private void initializeListeners() {
		ITargetChangedListener listener = new ITargetChangedListener() {
			public void contentsChanged(ITargetDefinition definition, Object source, boolean resolve, boolean forceResolve) {
				boolean setCancelled = false;
				if (forceResolve || (resolve && !definition.isResolved())) {
					try {
						getContainer().run(true, true, new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								getTargetDefinition().resolve(new ResolutionProgressMonitor(monitor));
								if (monitor.isCanceled()) {
									throw new InterruptedException();
								}
							}
						});
					} catch (InvocationTargetException e) {
						PDECore.log(e);
					} catch (InterruptedException e) {
						setCancelled = true;
					}
				}
				if (fContentTree != source) {
					if (setCancelled) {
						fContentTree.setCancelled(); // If the user cancelled the resolve, change the text to say it was cancelled
					} else {
						fContentTree.setInput(definition);
					}
				}
				if (fLocationTree != source) {
					fLocationTree.setInput(definition);
				}
				if (definition.isResolved() && definition.getStatus().getSeverity() == IStatus.ERROR) {
					fLocationTab.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
				} else {
					fLocationTab.setImage(null);
				}
			}
		};
		fContentTree.addTargetChangedListener(listener);
		fLocationTree.addTargetChangedListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionPage#targetChanged()
	 */
	protected void targetChanged(ITargetDefinition definition) {
		super.targetChanged(definition);
		if (definition != null) {
			// When  If the page isn't open yet, try running a UI job so the dialog has time to finish opening
			new UIJob(PDEUIMessages.TargetDefinitionContentPage_0) {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					ITargetDefinition definition = getTargetDefinition();
					if (!definition.isResolved()) {
						try {
							getContainer().run(true, true, new IRunnableWithProgress() {
								public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
									getTargetDefinition().resolve(new ResolutionProgressMonitor(monitor));
									if (monitor.isCanceled()) {
										throw new InterruptedException();
									}
								}
							});
						} catch (InvocationTargetException e) {
							PDECore.log(e);
						} catch (InterruptedException e) {
							fContentTree.setCancelled();
							return Status.CANCEL_STATUS;
						}
					}
					fContentTree.setInput(definition);
					fLocationTree.setInput(definition);
					if (definition.isResolved() && definition.getStatus().getSeverity() == IStatus.ERROR) {
						fLocationTab.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
					} else {
						fLocationTab.setImage(null);
					}
					return Status.OK_STATUS;
				}
			}.schedule();
			String name = definition.getName();
			if (name == null) {
				name = EMPTY_STRING;
			}

			if (name.trim().length() > 0)
				fNameText.setText(name);
			else
				setMessage(PDEUIMessages.TargetDefinitionContentPage_8);

			fLocationTree.setInput(definition);
			fContentTree.setInput(definition);

			String presetValue = (definition.getOS() == null) ? EMPTY_STRING : definition.getOS();
			fOSCombo.setText(presetValue);
			presetValue = (definition.getWS() == null) ? EMPTY_STRING : definition.getWS();
			fWSCombo.setText(presetValue);
			presetValue = (definition.getArch() == null) ? EMPTY_STRING : definition.getArch();
			fArchCombo.setText(presetValue);
			presetValue = (definition.getNL() == null) ? EMPTY_STRING : LocaleUtil.expandLocaleName(definition.getNL());
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

			presetValue = (definition.getProgramArguments() == null) ? EMPTY_STRING : definition.getProgramArguments();
			fProgramArgs.setText(presetValue);
			presetValue = (definition.getVMArguments() == null) ? EMPTY_STRING : definition.getVMArguments();
			fVMArgs.setText(presetValue);

			fElementViewer.refresh();
		}
	}

	private void createTargetEnvironmentGroup(Composite container) {
		Group group = SWTFactory.createGroup(container, PDEUIMessages.EnvironmentBlock_targetEnv, 2, 1, GridData.FILL_HORIZONTAL);

		initializeChoices();

		SWTFactory.createWrapLabel(group, PDEUIMessages.EnvironmentSection_description, 2);

		SWTFactory.createLabel(group, PDEUIMessages.Preferences_TargetEnvironmentPage_os, 1);

		fOSCombo = SWTFactory.createCombo(group, SWT.SINGLE | SWT.BORDER, 1, (String[]) fOSChoices.toArray(new String[fOSChoices.size()]));
		fOSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setOS(getModelValue(fOSCombo.getText()));
			}
		});

		SWTFactory.createLabel(group, PDEUIMessages.Preferences_TargetEnvironmentPage_ws, 1);

		fWSCombo = SWTFactory.createCombo(group, SWT.SINGLE | SWT.BORDER, 1, (String[]) fWSChoices.toArray(new String[fWSChoices.size()]));
		fWSCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setWS(getModelValue(fWSCombo.getText()));
			}
		});

		SWTFactory.createLabel(group, PDEUIMessages.Preferences_TargetEnvironmentPage_arch, 1);

		fArchCombo = SWTFactory.createCombo(group, SWT.SINGLE | SWT.BORDER, 1, (String[]) fArchChoices.toArray(new String[fArchChoices.size()]));
		fArchCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setArch(getModelValue(fArchCombo.getText()));
			}
		});

		SWTFactory.createLabel(group, PDEUIMessages.Preferences_TargetEnvironmentPage_nl, 1);

		fNLCombo = SWTFactory.createCombo(group, SWT.SINGLE | SWT.BORDER, 1, (String[]) fNLChoices.toArray(new String[fNLChoices.size()]));
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
	 * @return trimmed value or <code>null</code>
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

	/**
	* Delimits a comma separated preference and add the items to the given set
	* @param set
	* @param preference
	*/
	private void addExtraChoices(Set set, String preference) {
		StringTokenizer tokenizer = new StringTokenizer(preference, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			set.add(tokenizer.nextToken().trim());
		}
	}

	/**
	 * Loads combo choices fromt he platform and from PDE core preferences
	 */
	private void initializeChoices() {
		IEclipsePreferences node = new InstanceScope().getNode(PDECore.PLUGIN_ID);

		fOSChoices = new TreeSet();
		String[] os = Platform.knownOSValues();
		for (int i = 0; i < os.length; i++) {
			fOSChoices.add(os[i]);
		}
		String pref = node.get(ICoreConstants.OS_EXTRA, EMPTY_STRING);
		if (!EMPTY_STRING.equals(pref)) {
			addExtraChoices(fOSChoices, pref);
		}

		fWSChoices = new TreeSet();
		String[] ws = Platform.knownWSValues();
		for (int i = 0; i < ws.length; i++) {
			fWSChoices.add(ws[i]);
		}
		pref = node.get(ICoreConstants.WS_EXTRA, EMPTY_STRING);
		if (!EMPTY_STRING.equals(pref)) {
			addExtraChoices(fWSChoices, pref);
		}

		fArchChoices = new TreeSet();
		String[] arch = Platform.knownOSArchValues();
		for (int i = 0; i < arch.length; i++) {
			fArchChoices.add(arch[i]);
		}
		pref = node.get(ICoreConstants.ARCH_EXTRA, EMPTY_STRING);
		if (!EMPTY_STRING.equals(pref)) {
			addExtraChoices(fArchChoices, pref);
		}

		fNLChoices = new TreeSet();
		String[] nl = LocaleUtil.getLocales();
		for (int i = 0; i < nl.length; i++) {
			fNLChoices.add(nl[i]);
		}
		pref = node.get(ICoreConstants.NL_EXTRA, EMPTY_STRING);
		if (!EMPTY_STRING.equals(pref)) {
			addExtraChoices(fNLChoices, pref);
		}
	}

	private void createJREGroup(Composite container) {
		Group group = SWTFactory.createGroup(container, PDEUIMessages.EnvironmentBlock_jreTitle, 2, 1, GridData.FILL_HORIZONTAL);

		initializeJREValues();

		SWTFactory.createWrapLabel(group, PDEUIMessages.JRESection_description, 2);

		fDefaultJREButton = SWTFactory.createRadioButton(group, PDEUIMessages.JRESection_defaultJRE, 2);
		fDefaultJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newDefaultJREContainerPath());
			}
		});

		fNamedJREButton = SWTFactory.createRadioButton(group, PDEUIMessages.JRESection_JREName);
		fNamedJREButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getText())));
			}
		});

		fNamedJREsCombo = SWTFactory.createCombo(group, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY, 1, VMUtil.getVMInstallNames());
		fNamedJREsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getVMInstall(fNamedJREsCombo.getText())));
			}
		});

		fExecEnvButton = SWTFactory.createRadioButton(group, PDEUIMessages.JRESection_ExecutionEnv);
		fExecEnvButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateJREWidgets();
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getText())));
			}
		});

		fExecEnvsCombo = SWTFactory.createCombo(group, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY, 1, (String[]) fExecEnvChoices.toArray(new String[fExecEnvChoices.size()]));
		fExecEnvsCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setJREContainer(JavaRuntime.newJREContainerPath(VMUtil.getExecutionEnvironment(fExecEnvsCombo.getText())));
			}
		});

	}

	/**
	 * Initializes the combo with possible execution environments
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
		Composite container = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);

		SWTFactory.createWrapLabel(container, PDEUIMessages.JavaArgumentsTab_description, 1);

		Group programGroup = SWTFactory.createGroup(container, PDEUIMessages.JavaArgumentsTab_progamArgsGroup, 1, 1, GridData.FILL_HORIZONTAL);

		fProgramArgs = SWTFactory.createText(programGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL, 1, 200, 60, GridData.FILL_BOTH);
		fProgramArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setProgramArguments(fProgramArgs.getText().trim());
			}
		});

		Composite programButtons = SWTFactory.createComposite(programGroup, 1, 1, GridData.HORIZONTAL_ALIGN_END, 0, 0);

		Button programVars = SWTFactory.createPushButton(programButtons, PDEUIMessages.JavaArgumentsTab_programVariables, null, GridData.HORIZONTAL_ALIGN_END);
		programVars.addSelectionListener(getVariablesListener(fProgramArgs));

		Group vmGroup = new Group(container, SWT.NONE);
		vmGroup.setLayout(new GridLayout(1, false));
		vmGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		vmGroup.setText(PDEUIMessages.JavaArgumentsTab_vmArgsGroup);
		vmGroup.setFont(container.getFont());

		fVMArgs = SWTFactory.createText(vmGroup, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL, 1, 200, 60, GridData.FILL_BOTH);
		fVMArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				getTargetDefinition().setVMArguments(fVMArgs.getText().trim());
			}
		});

		Composite buttons = SWTFactory.createComposite(vmGroup, 2, 1, GridData.HORIZONTAL_ALIGN_END, 0, 0);

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
				if (dialog.open() == Window.OK) {
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
		container.setFont(parent.getFont());

		createImpLabel(container);
		createImpTable(container);
		createImpButtons(container);
		return container;
	}

	private void createImpLabel(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.TargetImplicitPluginsTab_desc);
		label.setFont(container.getFont());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
	}

	private void createImpTable(Composite container) {
		fElementViewer = new TableViewer(container, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		fElementViewer.getControl().setLayoutData(gd);
		fElementViewer.getControl().setFont(container.getFont());
		fElementViewer.setContentProvider(new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				ITargetDefinition target = getTargetDefinition();
				if (target != null) {
					NameVersionDescriptor[] bundles = target.getImplicitDependencies();
					if (bundles != null) {
						return bundles;
					}
				}
				return new NameVersionDescriptor[0];
			}
		});
		fElementViewer.setLabelProvider(new StyledBundleLabelProvider(false, false));
		fElementViewer.setInput(PDEPlugin.getDefault());
		fElementViewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				NameVersionDescriptor bundle1 = (NameVersionDescriptor) e1;
				NameVersionDescriptor bundle2 = (NameVersionDescriptor) e2;
				return super.compare(viewer, bundle1.getId(), bundle2.getId());
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
		Composite buttonContainer = SWTFactory.createComposite(container, 1, 1, GridData.FILL_VERTICAL, 0, 0);

		fAddButton = SWTFactory.createPushButton(buttonContainer, PDEUIMessages.SourceBlock_add, null);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		fRemoveButton = SWTFactory.createPushButton(buttonContainer, PDEUIMessages.SourceBlock_remove, null);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});

		fRemoveAllButton = SWTFactory.createPushButton(buttonContainer, PDEUIMessages.TargetImplicitPluginsTab_removeAll3, null);
		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});

		updateImpButtons();
	}

	protected void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new StyledBundleLabelProvider(false, false));

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
				pluginsToAdd.add(new NameVersionDescriptor(desc.getSymbolicName(), null));
			}
			Set allDependencies = new HashSet();
			allDependencies.addAll(pluginsToAdd);
			NameVersionDescriptor[] currentBundles = getTargetDefinition().getImplicitDependencies();
			if (currentBundles != null) {
				allDependencies.addAll(Arrays.asList(currentBundles));
			}
			getTargetDefinition().setImplicitDependencies((NameVersionDescriptor[]) allDependencies.toArray(new NameVersionDescriptor[allDependencies.size()]));
			fElementViewer.refresh();
			updateImpButtons();
		}
	}

	/**
	 * Gets a list of all the bundles that can be added as implicit dependencies
	 * @return list of possible dependencies
	 */
	protected BundleInfo[] getValidBundles() throws CoreException {
		NameVersionDescriptor[] current = getTargetDefinition().getImplicitDependencies();
		Set currentBundles = new HashSet();
		if (current != null) {
			for (int i = 0; i < current.length; i++) {
				if (!currentBundles.contains(current[i].getId())) {
					currentBundles.add(current[i].getId());
				}
			}
		}

		List targetBundles = new ArrayList();
		TargetBundle[] allTargetBundles = getTargetDefinition().getAllBundles();
		if (allTargetBundles == null || allTargetBundles.length == 0) {
			throw new CoreException(new Status(IStatus.WARNING, PDEPlugin.getPluginId(), PDEUIMessages.ImplicitDependenciesSection_0));
		}
		for (int i = 0; i < allTargetBundles.length; i++) {
			BundleInfo bundleInfo = allTargetBundles[i].getBundleInfo();
			if (!currentBundles.contains(bundleInfo.getSymbolicName())) {
				currentBundles.add(bundleInfo.getSymbolicName()); // to avoid duplicate entries
				targetBundles.add(bundleInfo);
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
				if (removeBundles[i] instanceof NameVersionDescriptor) {
					bundles.remove(removeBundles[i]);
				}
			}
			getTargetDefinition().setImplicitDependencies((NameVersionDescriptor[]) bundles.toArray((new NameVersionDescriptor[bundles.size()])));
			fElementViewer.refresh();
			updateImpButtons();
		}
	}

	private void handleRemoveAll() {
		getTargetDefinition().setImplicitDependencies(null);
		fElementViewer.refresh();
		updateImpButtons();
	}

	private void updateImpButtons() {
		boolean empty = fElementViewer.getSelection().isEmpty();
		fRemoveButton.setEnabled(!empty);
		boolean hasElements = fElementViewer.getTable().getItemCount() > 0;
		fRemoveAllButton.setEnabled(hasElements);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		if (fNameText.getText().trim().length() == 0)
			return false;
		return true;
	}
}
