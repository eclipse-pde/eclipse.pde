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
package org.eclipse.pde.api.tools.ui.internal.actions;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Compares {@link org.eclipse.pde.api.tools.internal.provisional.model.IApiElement}s
 * to a given {@link IApiBaseline} to get a delta
 * 
 * @since 1.0.1
 */
public class ExportDialog extends Dialog {

	//widget state ids 
	static final String SETTINGS_SECTION = ApiUIPlugin.PLUGIN_ID + ".api.exportsession"; //$NON-NLS-1$
	static final String REPORT_PATH_STATE = SETTINGS_SECTION + ".reportpath"; //$NON-NLS-1$
	
	private String title = null;;
	private Text reportPathText;
	public String reportPath;

	/**
	 * Constructor
	 * @param provider
	 * @param title
	 */
	public ExportDialog(IShellProvider provider, String title) {
		super(provider);
		this.title = title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(this.title);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		GridData gd = (GridData) comp.getLayoutData();
		gd.widthHint = 310;

		SWTFactory.createWrapLabel(comp, ActionMessages.EnterFileNameForExport, 2);
		this.reportPathText = SWTFactory.createText(comp, SWT.SINGLE | SWT.BORDER | SWT.FLAT, 1, GridData.BEGINNING | GridData.FILL_HORIZONTAL);
		gd = (GridData) this.reportPathText.getLayoutData();
		gd.grabExcessHorizontalSpace = true;

		Button browseButton = SWTFactory.createPushButton(comp, ActionMessages.Browse, null);
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell());
				dialog.setText(ActionMessages.SelectFileName);
				String loctext = ExportDialog.this.reportPathText.getText().trim();
				if (loctext.length() > 0) {
					File file = new File(loctext).getParentFile();
					if (file != null && file.exists()) {
						dialog.setFilterPath(file.getAbsolutePath());
					}
				}
				String newPath = dialog.open();
				if (newPath != null && !new Path(loctext).equals(new Path(newPath))) {
					ExportDialog.this.reportPathText.setText(newPath);
					ExportDialog.this.reportPath = newPath;
				}
			}
		});

		initialize();
		return comp;
	}
	
	/**
	 * Initializes the controls
	 */
	void initialize() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if(settings != null) {
			restoreTextSelection(this.reportPathText, REPORT_PATH_STATE, settings);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		String oldPath = this.reportPath;
		String newPath = this.reportPathText.getText().trim();
		if (newPath != null && !new Path(oldPath).equals(new Path(newPath))) {
			this.reportPathText.setText(newPath);
			this.reportPath = newPath;
		}
		saveWidgetState();
		super.okPressed();
	}
	/**
	 * Restores the selected item for the given combo based on the stored value from the 
	 * dialog settings
	 * 
	 * @param combo
	 * @param id
	 * @param settings
	 */
	private void restoreTextSelection(Text text, String id, IDialogSettings settings) {
		String value = settings.get(id);
		if(value != null) {
			ExportDialog.this.reportPath = value;
			text.setText(value);
		}
	}
	
	/**
	 * Saves the state of the widgets on the page
	 */
	void saveWidgetState() {
		IDialogSettings rootsettings = ApiUIPlugin.getDefault().getDialogSettings();
		IDialogSettings settings = rootsettings.getSection(SETTINGS_SECTION);
		if(settings == null) {
			settings = rootsettings.addNewSection(SETTINGS_SECTION);
		}
		settings.put(REPORT_PATH_STATE, this.reportPath);
	}
}