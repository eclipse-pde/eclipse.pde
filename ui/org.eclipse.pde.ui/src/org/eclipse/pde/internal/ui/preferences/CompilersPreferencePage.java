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
package org.eclipse.pde.internal.ui.preferences;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
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
	private ArrayList flagControls;
	private HashSet changedControls = null;

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

		flagControls = new ArrayList();
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (changedControls == null)  
					changedControls = new HashSet();  
				changedControls.add(e.widget);
			}
		};

		ModifyListener mlistener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (changedControls == null)
					changedControls = new HashSet();  
				changedControls.add(e.widget);
			}
		};
		
		String[] choices = new String[] { PDEPlugin.getResourceString("CompilersPreferencePage.error"), PDEPlugin.getResourceString("CompilersPreferencePage.warning"), PDEPlugin.getResourceString("CompilersPreferencePage.ignore")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.plugins"), CompilerFlags.PLUGIN_FLAGS, choices); //$NON-NLS-1$
		createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.schemas"), CompilerFlags.SCHEMA_FLAGS, choices); //$NON-NLS-1$
		createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.features"), CompilerFlags.FEATURE_FLAGS, choices); //$NON-NLS-1$
		//createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.sites"), CompilerFlags.SITE_FLAGS, choices); //$NON-NLS-1$

		for (int i = 0; i < flagControls.size(); i++) {
			Control control = (Control) flagControls.get(i);
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

	private void createPage(
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
			textKey = "CompilersPreferencePage.altlabel";
		else
			textKey = "CompilersPreferencePage.label";
		label.setText(PDEPlugin.getResourceString(textKey)); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		String[] flagIds = CompilerFlags.getFlags(index);
		for (int i = 0; i < flagIds.length; i++) {
			Control control = createFlag(page, flagIds[i], choices);
			flagControls.add(control);
		}
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
			slabel.setText(PDEPlugin.getResourceString("CompilersPreferencePage.label"));
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
		boolean hasChange = false;
		changedControls = new HashSet();
		for (int i = 0; i < flagControls.size(); i++) {
			Control control = (Control) flagControls.get(i);
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
				changedControls.add(control);
			hasChange = false;
		}

	}

	public boolean performOk() {
		if (changedControls != null) {

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

			if (res != 0 && res != 1) {
				return false;
			}
			for (Iterator iter = changedControls.iterator(); iter.hasNext();) {
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
			}
			CompilerFlags.save();

			if (res == 0) {
				doFullBuild();
			}
			changedControls=null; 
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
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
					try {
						PDEPlugin.getWorkspace().build(
							IncrementalProjectBuilder.FULL_BUILD,
							monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InterruptedException e) {
			// cancelled by user
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		}
	}
}
