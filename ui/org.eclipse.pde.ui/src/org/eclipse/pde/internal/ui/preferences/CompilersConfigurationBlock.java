/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.builders.CompilerFlags;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

/**
 */
public class CompilersConfigurationBlock {

	private Set fBuilders = new HashSet();

	private Set fChangedControls = new HashSet();

	private Composite fFeaturePage;

	private List fFlagControls;

	private Composite fPluginPage;

	private Composite fSchemaPage;

	private Shell fShell;
	
	// The size of label array must match CompilerFlag.fFlags
	private static final String[][] fLabels = {
			{ PDEUIMessages.compilers_p_unresolved_import,
					PDEUIMessages.compilers_p_unresolved_ex_points,
					PDEUIMessages.compilers_p_no_required_att,
					PDEUIMessages.compilers_p_unknown_element,
					PDEUIMessages.compilers_p_unknown_attribute,
					PDEUIMessages.compilers_p_deprecated,
					PDEUIMessages.compilers_p_unknown_class,
					PDEUIMessages.compilers_p_unknown_resource,
					PDEUIMessages.compilers_p_not_externalized_att },
			{ PDEUIMessages.compilers_s_create_docs,
					PDEUIMessages.compilers_s_doc_folder,
					PDEUIMessages.compilers_s_open_tags },
			{ PDEUIMessages.compilers_f_unresolved_plugins,
					PDEUIMessages.compilers_f_unresolved_features }, {} };

	/**
	 * @param project nNot null in property page
	 */
	private IProject project;

	public CompilersConfigurationBlock(IProject project) {
		this.project = project;
	}

	private void addChangedConrol(Control control) {
		String flagId = (String) control.getData();
		boolean doAdd = false;
		if (control instanceof Combo) {
			int newIndex = ((Combo) control).getSelectionIndex();
			int oldIndex = CompilerFlags.getFlag(project, flagId);
			doAdd = (newIndex != oldIndex);
		} else if (control instanceof Button) {
			boolean newValue = ((Button) control).getSelection();
			boolean oldValue = CompilerFlags.getBoolean(project, flagId);
			doAdd = oldValue != newValue;
		} else if (control instanceof Text) {
			String newValue = ((Text) control).getText();
			String oldValue = CompilerFlags.getString(project, flagId);
			doAdd = !newValue.equals(oldValue);
		}
		if (doAdd)
			fChangedControls.add(control);
		else if (fChangedControls.contains(control))
			fChangedControls.remove(control);
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(Composite)
	 */
	public Control createContents(Composite parent) {
		setShell(parent.getShell());

		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		fFlagControls = new ArrayList();
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addChangedConrol((Control) e.widget);
			}
		};

		ModifyListener mlistener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				addChangedConrol((Control) e.widget);
			}
		};

		String[] choices = new String[] {
				PDEUIMessages.CompilersConfigurationBlock_error, PDEUIMessages.CompilersConfigurationBlock_warning, PDEUIMessages.CompilersConfigurationBlock_ignore }; //  
		
		if (project != null) { // property page
			try {
				if (project.hasNature(PDE.PLUGIN_NATURE)) {
					fPluginPage = createPage(
							container,
							PDEUIMessages.CompilersConfigurationBlock_plugins, CompilerFlags.PLUGIN_FLAGS, choices); 

				}
			} catch (CoreException ce) {
				// does not exist or is closed
			}
		} else { // preference page
			TabFolder folder = new TabFolder(container, SWT.NONE);
			GridData gd = new GridData(GridData.FILL_BOTH);
			folder.setLayoutData(gd);

			fPluginPage = createPage(
					folder,
					PDEUIMessages.CompilersConfigurationBlock_plugins, CompilerFlags.PLUGIN_FLAGS, choices); 
			fSchemaPage = createPage(
					folder,
					PDEUIMessages.CompilersConfigurationBlock_schemas, CompilerFlags.SCHEMA_FLAGS, choices); 
			fFeaturePage = createPage(
					folder,
					PDEUIMessages.CompilersConfigurationBlock_features, CompilerFlags.FEATURE_FLAGS, choices); 
			// createPage(folder,
			// PDEPlugin.getResourceString("CompilersConfigurationBlock.sites"),
			// CompilerFlags.SITE_FLAGS, choices); //$NON-NLS-1$
		}

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
		return container;
	}

	private Control createFlag(Composite page, String flagId, String[] choices) {
		Control control = null;
		if (CompilerFlags.getFlagType(flagId) == CompilerFlags.MARKER) {
			Label label = new Label(page, SWT.NULL);
			label.setText(getFlagLabel(flagId));
			Combo combo = new Combo(page, SWT.READ_ONLY);
			combo.setItems(choices);
			combo.select(CompilerFlags.getFlag(project, flagId));
			control = combo;
		} else if (CompilerFlags.getFlagType(flagId) == CompilerFlags.BOOLEAN) {
			Button button = new Button(page, SWT.CHECK);
			button.setText(getFlagLabel(flagId));
			button.setSelection(CompilerFlags.getBoolean(project, flagId));
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			button.setLayoutData(gd);
			control = button;
		} else if (CompilerFlags.getFlagType(flagId) == CompilerFlags.STRING) {
			Label label = new Label(page, SWT.NULL);
			label.setText(getFlagLabel(flagId));
			Text text = new Text(page, SWT.SINGLE | SWT.BORDER);
			text.setText(CompilerFlags.getString(project, flagId));
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint = 50;
			text.setLayoutData(gd);

			new Label(page, SWT.NULL).setLayoutData(new GridData());
			GridData sgd = new GridData();
			Label slabel = new Label(page, SWT.NULL);
			slabel.setText(PDEUIMessages.CompilersConfigurationBlock_label); 
			sgd.horizontalSpan = 2;
			slabel.setLayoutData(sgd);

			control = text;
		}
		control.setData(flagId);
		return control;
	}
	
	private String getFlagLabel(String flagId) {
		for (int i = 0; i < fLabels.length; i++) {
			String[] flags = CompilerFlags.getFlags(i);
			for (int j = 0; j < flags.length; j++) {
				if (flags[j].equals(flagId)) {
					return fLabels[i][j];
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

	private Composite createPage(Composite parent, String name, int index,
			String[] choices) {
		Group group = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		group.setLayout(layout);

		String labelText;
		if (index == CompilerFlags.SCHEMA_FLAGS)
			labelText = PDEUIMessages.CompilersConfigurationBlock_altlabel; 
		else
			labelText = PDEUIMessages.CompilersConfigurationBlock_label; 
		group.setText(labelText);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.grabExcessHorizontalSpace = true;
		group.setLayoutData(gd);
		
		String[] flagIds = CompilerFlags.getFlags(index);
		for (int i = 0; i < flagIds.length; i++) {
			Control control = createFlag(group, flagIds[i], choices);
			fFlagControls.add(control);
		}
		return group;
	}
	private Composite createPage(TabFolder folder, String name, int index,
			String[] choices) {
		Composite page = new Composite(folder, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		page.setLayout(layout);

		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(name);
		tab.setControl(page);

		Label label = new Label(page, SWT.NULL);
		String labelText;
		if (index == CompilerFlags.SCHEMA_FLAGS)
			labelText = PDEUIMessages.CompilersConfigurationBlock_altlabel; 
		else
			labelText = PDEUIMessages.CompilersConfigurationBlock_label; 
		label.setText(labelText);
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

	private void doFullBuild() {
		Job buildJob = new Job(PDEUIMessages.CompilersConfigurationBlock_building) { 
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
			 */
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}

			//$NON-NLS-1$
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject[] projects = null;
					if (project == null) {
						projects = PDE.getWorkspace().getRoot().getProjects();
					} else {
						projects = new IProject[] { project };
					}
					monitor.beginTask("", projects.length * 2); //$NON-NLS-1$
					for (int i = 0; i < projects.length; i++) {
						IProject projectToBuild = projects[i];
						if (!projectToBuild.isOpen())
							continue;
						if (projectToBuild.hasNature(PDE.PLUGIN_NATURE)) {
							if (fBuilders.contains(PDE.MANIFEST_BUILDER_ID))
								projectToBuild.build(
										IncrementalProjectBuilder.FULL_BUILD,
										PDE.MANIFEST_BUILDER_ID, null,
										new SubProgressMonitor(monitor, 1));
							else
								monitor.worked(1);
							if (fBuilders.contains(PDE.SCHEMA_BUILDER_ID))
								projectToBuild.build(
										IncrementalProjectBuilder.FULL_BUILD,
										PDE.SCHEMA_BUILDER_ID, null,
										new SubProgressMonitor(monitor, 1));
							else
								monitor.worked(1);
						} else if (projectToBuild.hasNature(PDE.FEATURE_NATURE)) {
							if (fBuilders.contains(PDE.FEATURE_BUILDER_ID))
								projectToBuild.build(
										IncrementalProjectBuilder.FULL_BUILD,
										PDE.FEATURE_BUILDER_ID, null,
										new SubProgressMonitor(monitor, 2));
						} else {
							monitor.worked(2);
						}
					}
				} catch (CoreException e) {
					return e.getStatus();
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory()
				.buildRule());
		buildJob.setUser(true);
		buildJob.schedule();
	}

	protected Shell getShell() {
		return fShell;
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	public void performDefaults() {
		fChangedControls.clear();
		for (int i = 0; i < fFlagControls.size(); i++) {
			boolean hasChange = false;
			Control control = (Control) fFlagControls.get(i);
			String flagId = (String) control.getData();
			if (control instanceof Combo) {
				if (project != null)
					hasChange = CompilerFlags.getFlag(project, flagId) != CompilerFlags
							.getDefaultFlag(flagId);
				else
					hasChange = ((Combo) control).getSelectionIndex() != CompilerFlags
							.getDefaultFlag(flagId);
				((Combo) control).select(CompilerFlags.getDefaultFlag(flagId));
			} else if (control instanceof Button) {
				if (project != null)
					hasChange = CompilerFlags.getBoolean(project, flagId) != CompilerFlags
							.getDefaultBoolean(flagId);
				else
					hasChange = ((Button) control).getSelection() != CompilerFlags
							.getDefaultBoolean(flagId);
				((Button) control).setSelection(CompilerFlags
						.getDefaultBoolean(flagId));
			} else if (control instanceof Text) {
				if (project != null)
					hasChange = !CompilerFlags.getString(project, flagId)
							.equals(CompilerFlags.getDefaultString(flagId));
				else
					hasChange = ((Text) control).getText() != CompilerFlags
							.getDefaultString(flagId);
				((Text) control)
						.setText(CompilerFlags.getDefaultString(flagId));
			}
			if (hasChange)
				fChangedControls.add(control);
		}
	}

	public boolean performOk(boolean enabled) {
		Set changedControls = fChangedControls;
		if (!enabled) {
			// fChangedControls is not a valid change.
			// The change is the difference between values in
			// PROJECT,INSTANCE,DEFAULD
			// and INSTANCE,DEFAULT scopes.
			changedControls = new HashSet();
			for (Iterator iter = fFlagControls.iterator(); iter.hasNext();) {
				Control control = (Control) iter.next();
				String flagId = (String) control.getData();
				if (!CompilerFlags.getString(project, flagId).equals(
						CompilerFlags.getString(null, flagId))) {
					changedControls.add(control);
					break;
				}
			}
		}
		boolean build = false;
		if (changedControls.size() > 0) {
			String title;
			String message;
			if (project != null) {
				title = PDEUIMessages.CompilersConfigurationBlock_rebuild_title; 
				message = PDEUIMessages.CompilersConfigurationBlock_rebuild_message; 
			} else {
				title = PDEUIMessages.CompilersConfigurationBlock_rebuild_many_title; 
				message = PDEUIMessages.CompilersConfigurationBlock_rebuild_many_message; 

			}

			MessageDialog dialog = new MessageDialog(getShell(), title, null,
					message, MessageDialog.QUESTION, new String[] {
							IDialogConstants.YES_LABEL,
							IDialogConstants.NO_LABEL,
							IDialogConstants.CANCEL_LABEL }, 2);
			int res = dialog.open();

			if (res == 2) {
				return false;
			} else if (res == 0) {
				build = true;
			}
		}
		if (project != null
				&& enabled != CompilerFlags.getBoolean(project,
						CompilerFlags.USE_PROJECT_PREF)) {
			if (enabled) {
				CompilerFlags.setBoolean(project,
						CompilerFlags.USE_PROJECT_PREF, true);
			} else {
				CompilerFlags.clear(project, CompilerFlags.USE_PROJECT_PREF);
			}
		}
		if (changedControls.size() > 0) {
			fBuilders = new HashSet();
			for (Iterator iter = changedControls.iterator(); iter.hasNext();) {
				Control control = (Control) iter.next();
				String flagId = (String) control.getData();
				if (control instanceof Combo) {
					int index = ((Combo) control).getSelectionIndex();
					if (project == null) {
						CompilerFlags.setFlag(flagId, index);
					}
				} else if (control instanceof Button) {
					boolean value = ((Button) control).getSelection();
					if (project == null) {
						CompilerFlags.setBoolean(flagId, value);
					}
				} else if (control instanceof Text) {
					String value = ((Text) control).getText();
					if (project == null) {
						CompilerFlags.setString(flagId, value);
					}
				}
				if (control.getParent().equals(fPluginPage))
					fBuilders.add(PDE.MANIFEST_BUILDER_ID);
				else if (control.getParent().equals(fSchemaPage))
					fBuilders.add(PDE.SCHEMA_BUILDER_ID);
				else if (control.getParent().equals(fFeaturePage)) {
					fBuilders.add(PDE.FEATURE_BUILDER_ID);
					fBuilders.add(PDE.SITE_BUILDER_ID);
				}		
			}
			if (project == null) {
				CompilerFlags.save();
			}
		}
		if (project != null) {
			for (Iterator iter = fFlagControls.iterator(); iter.hasNext();) {
				Control control = (Control) iter.next();
				String flagId = (String) control.getData();
				if (control instanceof Combo) {
					int index = ((Combo) control).getSelectionIndex();
					if (enabled) {
						CompilerFlags.setFlag(project, flagId, index);
					} else {
						CompilerFlags.clear(project, flagId);
					}
				} else if (control instanceof Button) {
					boolean value = ((Button) control).getSelection();
					if (enabled) {
						CompilerFlags.setBoolean(project, flagId, value);
					} else {
						CompilerFlags.clear(project, flagId);
					}
				} else if (control instanceof Text) {
					String value = ((Text) control).getText();
					if (enabled) {
						CompilerFlags.setString(project, flagId, value);
					} else {
						CompilerFlags.clear(project, flagId);
					}
				}
			}
		}

		if (build && fBuilders.size() > 0) {
			doFullBuild();
		}

		fChangedControls.clear();
		return true;
	}

	protected void setShell(Shell shell) {
		fShell = shell;
	}
}
