/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.builders.CompilerFlags;
import org.eclipse.pde.internal.core.IEnvironmentVariables;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 */
public class CompilersPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage, IEnvironmentVariables {
	private ArrayList fFlagControls;
	private HashSet fChangedControls = new HashSet();
	private Composite fPluginPage;
	private Composite fSchemaPage;
	private Composite fFeaturePage;
	private HashSet fBuilders = new HashSet();

	public CompilersPreferencePage() {
		setDescription(PDEPlugin.getResourceString("CompilersPreferencePage.desc")); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		TabFolder folder = new TabFolder(container, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		folder.setLayoutData(gd);

		fFlagControls = new ArrayList();
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addChangedConrol((Control)e.widget);
			}
		};

		ModifyListener mlistener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				addChangedConrol((Control)e.widget);
			}
		};
		
		String[] choices = new String[] { PDEPlugin.getResourceString("CompilersPreferencePage.error"), PDEPlugin.getResourceString("CompilersPreferencePage.warning"), PDEPlugin.getResourceString("CompilersPreferencePage.ignore")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		fPluginPage = createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.plugins"), CompilerFlags.PLUGIN_FLAGS, choices); //$NON-NLS-1$
		fSchemaPage = createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.schemas"), CompilerFlags.SCHEMA_FLAGS, choices); //$NON-NLS-1$
		fFeaturePage = createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.features"), CompilerFlags.FEATURE_FLAGS, choices); //$NON-NLS-1$
		//createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.sites"), CompilerFlags.SITE_FLAGS, choices); //$NON-NLS-1$

		for (int i = 0; i < fFlagControls.size(); i++) {
			Control control = (Control) fFlagControls.get(i);
			if (control instanceof Combo)
				 ((Combo) control).addSelectionListener(listener);
			else if (control instanceof Button)
				 ((Button) control).addSelectionListener(listener);
			else if (control instanceof Text)
				 ((Text) control).addModifyListener(mlistener);
		}
		Dialog.applyDialogFont(parent);
		WorkbenchHelp.setHelp(container, IHelpContextIds.COMPILERS_PREFERENCE_PAGE);
		return container;
	}
	
	private void addChangedConrol(Control control) {
		String flagId = (String) control.getData();
		boolean doAdd = false;
		if (control instanceof Combo) {
			int newIndex = ((Combo) control).getSelectionIndex();
			int oldIndex = CompilerFlags.getFlag(flagId);
			doAdd = (newIndex != oldIndex);
		} else if (control instanceof Button) {
			boolean newValue = ((Button) control).getSelection();
			boolean oldValue = CompilerFlags.getBoolean(flagId);
			doAdd = oldValue != newValue;
		} else if (control instanceof Text) {
			String newValue = ((Text) control).getText();
			String oldValue = CompilerFlags.getString(flagId);
			doAdd = !newValue.equals(oldValue);
		}
		if (doAdd)
			fChangedControls.add(control);
		else if (fChangedControls.contains(control))
			fChangedControls.remove(control);
	}

	private Composite createPage(
		TabFolder folder,
		String name,
		int index,
		String[] choices) {
		Composite page = new Composite(folder, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		page.setLayout(layout);

		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(name);
		tab.setControl(page);

		Label label = new Label(page, SWT.NULL);
		String textKey;
		if (index == CompilerFlags.SCHEMA_FLAGS)
			textKey = "CompilersPreferencePage.altlabel"; //$NON-NLS-1$
		else
			textKey = "CompilersPreferencePage.label"; //$NON-NLS-1$
		label.setText(PDEPlugin.getResourceString(textKey)); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		String[] flagIds = CompilerFlags.getFlags(index);
		for (int i = 0; i < flagIds.length; i++) {
			Control control = createFlag(page, flagIds[i], choices);
			fFlagControls.add(control);
		}
		return page;
	}

	private Control createFlag(
		Composite page,
		String flagId,
		String[] choices) {
		Control control = null;
		if (CompilerFlags.getFlagType(flagId) == CompilerFlags.MARKER) {
			Label label = new Label(page, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(flagId));
			Combo combo = new Combo(page, SWT.READ_ONLY);
			combo.setItems(choices);
			combo.select(CompilerFlags.getFlag(flagId));
			control = combo;
		} else if (
			CompilerFlags.getFlagType(flagId) == CompilerFlags.BOOLEAN) {
			Button button = new Button(page, SWT.CHECK);
			button.setText(PDEPlugin.getResourceString(flagId));
			button.setSelection(CompilerFlags.getBoolean(flagId));
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			button.setLayoutData(gd);
			control = button;
		} else if (CompilerFlags.getFlagType(flagId) == CompilerFlags.STRING) {
			Label label = new Label(page, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(flagId));
			Text text = new Text(page, SWT.SINGLE | SWT.BORDER);
			text.setText(CompilerFlags.getString(flagId));
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint = 50;
			text.setLayoutData(gd);
			
			new Label(page, SWT.NULL).setLayoutData(new GridData());
			GridData sgd = new GridData();
			Label slabel = new Label(page, SWT.NULL);
			slabel.setText(PDEPlugin.getResourceString("CompilersPreferencePage.label")); //$NON-NLS-1$
			sgd.horizontalSpan = 2;
			slabel.setLayoutData(sgd);
			
			control = text;
		}
		control.setData(flagId);
		return control;
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fChangedControls.clear();
		for (int i = 0; i < fFlagControls.size(); i++) {
			boolean hasChange = false;
			Control control = (Control) fFlagControls.get(i);
			String flagId = (String) control.getData();
			if (control instanceof Combo) {
				hasChange = ((Combo)control).getSelectionIndex() != CompilerFlags.getDefaultFlag(flagId);
				((Combo) control).select(CompilerFlags.getDefaultFlag(flagId));
			} else if (control instanceof Button) {
				hasChange = ((Button) control).getSelection() != CompilerFlags.getDefaultBoolean(flagId);
				((Button) control).setSelection(
					CompilerFlags.getDefaultBoolean(flagId));
			} else if (control instanceof Text) {
				hasChange = ((Text) control).getText() != CompilerFlags.getDefaultString(flagId);
				((Text) control).setText(
					CompilerFlags.getDefaultString(flagId));
			}
			if (hasChange)
				fChangedControls.add(control);
		}
	}

	public boolean performOk() {
		if (fChangedControls.size() > 0) {

			String title = PDEPlugin.getResourceString("CompilersPreferencePage.rebuild.title"); //$NON-NLS-1$
			String message = PDEPlugin.getResourceString("CompilersPreferencePage.rebuild.message"); //$NON-NLS-1$

			MessageDialog dialog =
				new MessageDialog(
					getShell(),
					title,
					null,
					message,
					MessageDialog.QUESTION,
					new String[] {
						IDialogConstants.YES_LABEL,
						IDialogConstants.NO_LABEL,
						IDialogConstants.CANCEL_LABEL },
					2);
			int res = dialog.open();

			if (res == 2) {
				return false;
			}
			fBuilders = new HashSet();
			for (Iterator iter = fChangedControls.iterator(); iter.hasNext();) {
				Control control = (Control) iter.next();
				String flagId = (String) control.getData();
				if (control instanceof Combo) {
					int index = ((Combo) control).getSelectionIndex();
					CompilerFlags.setFlag(flagId, index);
				} else if (control instanceof Button) {
					boolean value = ((Button) control).getSelection();
					CompilerFlags.setBoolean(flagId, value);
				} else if (control instanceof Text) {
					String value = ((Text) control).getText();
					CompilerFlags.setString(flagId, value);
				}
				if (control.getParent().equals(fPluginPage))
					fBuilders.add(PDE.MANIFEST_BUILDER_ID);
				else if (control.getParent().equals(fSchemaPage))
					fBuilders.add(PDE.SCHEMA_BUILDER_ID);
				else if (control.getParent().equals(fFeaturePage))
					fBuilders.add(PDE.FEATURE_BUILDER_ID);
			}
			CompilerFlags.save();

			if (res == 0) {
				doFullBuild();
			}
			fChangedControls.clear();
		}

		return super.performOk();
	}

	/**
	 * Initializes this preference page using the passed desktop.
	 *
	 * @param desktop the current desktop
	 */
	public void init(IWorkbench workbench) {
	}

	private void doFullBuild() {
		Job buildJob = new Job(PDEPlugin.getResourceString("CompilersPreferencePage.building")) { //$NON-NLS-1$
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject[] projects = PDE.getWorkspace().getRoot().getProjects();
					monitor.beginTask("", projects.length*2); //$NON-NLS-1$
					for (int i = 0; i < projects.length; i++) {
						IProject project = projects[i];
						if (!project.isOpen())
							continue;
						if (project.hasNature(PDE.PLUGIN_NATURE)) {
							if (fBuilders.contains(PDE.MANIFEST_BUILDER_ID))
								project.build(IncrementalProjectBuilder.FULL_BUILD, PDE.MANIFEST_BUILDER_ID, null, new SubProgressMonitor(monitor,1));
							else
								monitor.worked(1);
							if (fBuilders.contains(PDE.SCHEMA_BUILDER_ID))
								project.build(IncrementalProjectBuilder.FULL_BUILD, PDE.SCHEMA_BUILDER_ID, null, new SubProgressMonitor(monitor,1));
							else
								monitor.worked(1);
						} else if (project.hasNature(PDE.FEATURE_NATURE)) {
							if (fBuilders.contains(PDE.FEATURE_BUILDER_ID))
								project.build(IncrementalProjectBuilder.FULL_BUILD, PDE.FEATURE_BUILDER_ID, null, new SubProgressMonitor(monitor,2));
						} else {
							monitor.worked(2);
						}
					}
				} catch (CoreException e) {
					return e.getStatus();
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
			 */
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}
		};
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true); 
		buildJob.schedule();
	}
}
