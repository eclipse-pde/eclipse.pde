package org.eclipse.pde.internal.ui.preferences;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.pde.internal.builders.CompilerFlags;
import org.eclipse.pde.internal.core.IEnvironmentVariables;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 */
public class CompilersPreferencePage
	extends PreferencePage
	implements IWorkbenchPreferencePage, IEnvironmentVariables {
	private ArrayList flagCombos;
	private Preferences preferences;
	private HashSet changedCombos = null;

	public CompilersPreferencePage() {
		setDescription(PDEPlugin.getResourceString("CompilersPreferencePage.desc")); //$NON-NLS-1$
		preferences = PDECore.getDefault().getPluginPreferences();
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

		flagCombos = new ArrayList();
		SelectionListener listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (changedCombos == null)
					changedCombos = new HashSet();
				changedCombos.add(e.widget);
			}
		};

		String[] choices = new String[] { PDEPlugin.getResourceString("CompilersPreferencePage.error"), PDEPlugin.getResourceString("CompilersPreferencePage.warning"), PDEPlugin.getResourceString("CompilersPreferencePage.ignore")}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.plugins"), CompilerFlags.PLUGIN_FLAGS, choices); //$NON-NLS-1$
		createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.features"), CompilerFlags.FEATURE_FLAGS, choices); //$NON-NLS-1$
		createPage(folder, PDEPlugin.getResourceString("CompilersPreferencePage.sites"), CompilerFlags.SITE_FLAGS, choices); //$NON-NLS-1$

		for (int i = 0; i < flagCombos.size(); i++) {
			Combo combo = (Combo) flagCombos.get(i);
			combo.addSelectionListener(listener);
		}

		//WorkbenchHelp.setHelp(container, IHelpContextIds.TARGET_ENVIRONMENT_PREFERENCE_PAGE);
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
		label.setText(PDEPlugin.getResourceString("CompilersPreferencePage.label")); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		String[] flagIds = CompilerFlags.getFlags(index);
		for (int i = 0; i < flagIds.length; i++) {
			Combo combo = createFlag(page, flagIds[i], choices);
			flagCombos.add(combo);
		}
	}

	private Combo createFlag(Composite page, String flagId, String[] choices) {
		Label label = new Label(page, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(flagId));
		Combo combo = new Combo(page, SWT.READ_ONLY);
		combo.setItems(choices);
		combo.setData(flagId);
		combo.select(CompilerFlags.getFlag(flagId));
		return combo;
	}

	/**
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		for (int i = 0; i < flagCombos.size(); i++) {
			Combo combo = (Combo) flagCombos.get(i);
			String flagId = (String) combo.getData();
			combo.select(CompilerFlags.getDefaultFlag(flagId));
		}
		changedCombos = null;
	}

	public boolean performOk() {
		if (changedCombos != null) {

			String title = PDEPlugin.getResourceString("CompilersPreferencePage.rebuild.title"); //$NON-NLS-1$
			String message =
				PDEPlugin.getResourceString("CompilersPreferencePage.rebuild.message"); //$NON-NLS-1$

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
			for (Iterator iter = changedCombos.iterator(); iter.hasNext();) {
				Combo combo = (Combo) iter.next();
				int index = combo.getSelectionIndex();
				String flagId = (String) combo.getData();
				CompilerFlags.setFlag(flagId, index);
			}
			CompilerFlags.save();

			if (res == 0) {
				doFullBuild();
			}
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
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
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