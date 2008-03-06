/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import java.util.*;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * A configuration block used to modify PDE compiler settings. Can be used as a preference page (a null project)
 * or as a property page (applicable to plugin projects).
 */
public class CompilersConfigurationTab {

	private Set fBuilders = new HashSet();

	private Set fChangedControls = new HashSet();

	private Composite fFeaturePage;

	private List fFlagControls;

	private Composite fPluginPage;

	private Composite fSchemaPage;

	private Shell fShell;

	// The size of label array must match CompilerFlag.fFlags
	private static final String[][] fLabels = { {PDEUIMessages.compilers_p_unresolved_import, PDEUIMessages.CompilersConfigurationTab_incompatEnv, PDEUIMessages.compilers_p_unresolved_ex_points, PDEUIMessages.compilers_p_no_required_att, PDEUIMessages.compilers_p_unknown_element, PDEUIMessages.compilers_p_unknown_attribute, PDEUIMessages.compilers_p_deprecated, PDEUIMessages.compilers_p_unknown_class, PDEUIMessages.compilers_p_discouraged_class, PDEUIMessages.compilers_p_unknown_resource, PDEUIMessages.compilers_p_not_externalized_att, PDEUIMessages.CompilersConfigurationTab_buildPropertiesErrors, PDEUIMessages.compilers_p_exported_pkgs, PDEUIMessages.CompilersConfigurationTab_missingBundleClasspathEntries},
			{PDEUIMessages.compilers_s_create_docs, PDEUIMessages.compilers_s_doc_folder, PDEUIMessages.compilers_s_open_tags}, {PDEUIMessages.compilers_f_unresolved_plugins, PDEUIMessages.compilers_f_unresolved_features}, {}};

	/**
	 * The backing project may be <code>null</code>
	 */
	private IProject project;

	/**
	 * Constructor
	 * @param project
	 */
	public CompilersConfigurationTab(IProject project) {
		this.project = project;
	}

	/**
	 * Adds the specified control to a listing of controls that have changes
	 * @param control
	 */
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
		fShell = parent.getShell();

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

		String[] choices = new String[] {PDEUIMessages.CompilersConfigurationBlock_error, PDEUIMessages.CompilersConfigurationBlock_warning, PDEUIMessages.CompilersConfigurationBlock_ignore}; //  

		if (project != null) { // property page
			try {
				if (project.hasNature(PDE.PLUGIN_NATURE)) {
					fPluginPage = createPage(container, PDEUIMessages.CompilersConfigurationBlock_plugins, CompilerFlags.PLUGIN_FLAGS, choices);
				}
			} catch (CoreException ce) {
			}
		} else { // preference page
			TabFolder folder = new TabFolder(container, SWT.NONE);
			GridData gd = new GridData(GridData.FILL_BOTH);
			folder.setLayoutData(gd);

			fPluginPage = createPage(folder, PDEUIMessages.CompilersConfigurationBlock_plugins, CompilerFlags.PLUGIN_FLAGS, choices);
			fSchemaPage = createPage(folder, PDEUIMessages.CompilersConfigurationBlock_schemas, CompilerFlags.SCHEMA_FLAGS, choices);
			fFeaturePage = createPage(folder, PDEUIMessages.CompilersConfigurationBlock_features, CompilerFlags.FEATURE_FLAGS, choices);
		}

		for (int i = 0; i < fFlagControls.size(); i++) {
			Control control = (Control) fFlagControls.get(i);
			if (control instanceof Combo) {
				((Combo) control).addSelectionListener(listener);
			} else if (control instanceof Button) {
				((Button) control).addSelectionListener(listener);
			} else if (control instanceof Text) {
				((Text) control).addModifyListener(mlistener);
			}
		}
		Dialog.applyDialogFont(parent);
		return container;
	}

	/**
	 * Creates a preference element for the given flag and choices
	 * @param page
	 * @param flagId
	 * @param choices
	 * @return a new pref control
	 */
	private Control createFlag(Composite page, String flagId, String[] choices) {
		Control control = null;
		if (CompilerFlags.getFlagType(flagId) == CompilerFlags.MARKER) {
			Label label = new Label(page, SWT.NULL);
			label.setText(getFlagLabel(flagId));
			label.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, false));
			Combo combo = new Combo(page, SWT.READ_ONLY);
			combo.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
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

	/**
	 * Returns the corresponding flag label given the id
	 * @param flagId
	 * @return the label for the flag with the given id
	 */
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

	/**
	 * Creates a new page in the parent tab folder
	 * @param folder
	 * @param name the name of the tab
	 * @param index the index of the message to use for the tab
	 * @param choices the listing of choices
	 * @return a new page in the parent tab folder
	 */
	private Composite createPage(Composite parent, String name, int index, String[] choices) {
		Composite page = new Composite(parent, SWT.NONE);
		page.setLayout(new GridLayout(2, false));
		page.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (parent instanceof TabFolder) {
			TabItem tab = new TabItem((TabFolder) parent, SWT.NONE);
			tab.setText(name);
			tab.setControl(page);
		}

		createVerticalSpacer(page, 1);
		Label label = new Label(page, SWT.NULL);
		String labelText;
		if (index == CompilerFlags.SCHEMA_FLAGS) {
			labelText = PDEUIMessages.CompilersConfigurationBlock_altlabel;
		} else {
			labelText = PDEUIMessages.CompilersConfigurationBlock_label;
		}
		label.setText(labelText);
		GridData gd = new GridData(SWT.BEGINNING, SWT.TOP, true, false);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		createVerticalSpacer(page, 1);
		String[] flagIds = CompilerFlags.getFlags(index);
		for (int i = 0; i < flagIds.length; i++) {
			Control control = createFlag(page, flagIds[i], choices);
			fFlagControls.add(control);
		}
		return page;
	}

	/**
	 * Creates a vertical spacer for separating components. If applied to a 
	 * <code>GridLayout</code>, this method will automatically span all of the columns of the parent
	 * to make vertical space
	 * 
	 * @param parent the parent composite to add this spacer to
	 * @param numlines the number of vertical lines to make as space
	 */
	public static void createVerticalSpacer(Composite parent, int numlines) {
		Label lbl = new Label(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		Layout layout = parent.getLayout();
		if (layout instanceof GridLayout) {
			gd.horizontalSpan = ((GridLayout) parent.getLayout()).numColumns;
		}
		gd.heightHint = numlines;
		lbl.setLayoutData(gd);
	}

	/**
	 * Performs a full build of the workspace
	 */
	private void doFullBuild() {
		Job buildJob = new Job(PDEUIMessages.CompilersConfigurationBlock_building) {
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}

			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject[] projects = null;
					if (project == null) {
						projects = PDEPlugin.getWorkspace().getRoot().getProjects();
					} else {
						projects = new IProject[] {project};
					}
					monitor.beginTask("", projects.length * 2); //$NON-NLS-1$
					for (int i = 0; i < projects.length; i++) {
						IProject projectToBuild = projects[i];
						if (!projectToBuild.isOpen())
							continue;
						if (projectToBuild.hasNature(PDE.PLUGIN_NATURE)) {
							if (fBuilders.contains(PDE.MANIFEST_BUILDER_ID))
								projectToBuild.build(IncrementalProjectBuilder.FULL_BUILD, PDE.MANIFEST_BUILDER_ID, null, new SubProgressMonitor(monitor, 1));
							else
								monitor.worked(1);
							if (fBuilders.contains(PDE.SCHEMA_BUILDER_ID))
								projectToBuild.build(IncrementalProjectBuilder.FULL_BUILD, PDE.SCHEMA_BUILDER_ID, null, new SubProgressMonitor(monitor, 1));
							else
								monitor.worked(1);
						} else if (projectToBuild.hasNature(PDE.FEATURE_NATURE)) {
							if (fBuilders.contains(PDE.FEATURE_BUILDER_ID))
								projectToBuild.build(IncrementalProjectBuilder.FULL_BUILD, PDE.FEATURE_BUILDER_ID, null, new SubProgressMonitor(monitor, 2));
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
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true);
		buildJob.schedule();
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
					hasChange = CompilerFlags.getFlag(project, flagId) != CompilerFlags.getDefaultFlag(flagId);
				else
					hasChange = ((Combo) control).getSelectionIndex() != CompilerFlags.getDefaultFlag(flagId);
				((Combo) control).select(CompilerFlags.getDefaultFlag(flagId));
			} else if (control instanceof Button) {
				if (project != null)
					hasChange = CompilerFlags.getBoolean(project, flagId) != CompilerFlags.getDefaultBoolean(flagId);
				else
					hasChange = ((Button) control).getSelection() != CompilerFlags.getDefaultBoolean(flagId);
				((Button) control).setSelection(CompilerFlags.getDefaultBoolean(flagId));
			} else if (control instanceof Text) {
				if (project != null)
					hasChange = !CompilerFlags.getString(project, flagId).equals(CompilerFlags.getDefaultString(flagId));
				else
					hasChange = ((Text) control).getText() != CompilerFlags.getDefaultString(flagId);
				((Text) control).setText(CompilerFlags.getDefaultString(flagId));
			}
			if (hasChange)
				fChangedControls.add(control);
		}
	}

	/**
	 * Applies the changes settings (if any), and frequests a full build (if needed)
	 * @param enabled
	 * @return the success of the operation
	 */
	public boolean performOk(boolean enabled) {
		Set changedControls = fChangedControls;
		if (!enabled) {
			// fChangedControls is not a valid change.
			// The change is the difference between values in
			// PROJECT,INSTANCE,DEFAULT
			// and INSTANCE,DEFAULT scopes.
			changedControls = new HashSet();
			for (Iterator iter = fFlagControls.iterator(); iter.hasNext();) {
				Control control = (Control) iter.next();
				String flagId = (String) control.getData();
				if (!CompilerFlags.getString(project, flagId).equals(CompilerFlags.getString(null, flagId))) {
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

			MessageDialog dialog = new MessageDialog(fShell, title, null, message, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 2);
			int res = dialog.open();

			if (res == 2) {
				return false;
			} else if (res == 0) {
				build = true;
			}
		}
		if (project != null && enabled != CompilerFlags.getBoolean(project, CompilerFlags.USE_PROJECT_PREF)) {
			if (enabled) {
				CompilerFlags.setBoolean(project, CompilerFlags.USE_PROJECT_PREF, true);
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
}
