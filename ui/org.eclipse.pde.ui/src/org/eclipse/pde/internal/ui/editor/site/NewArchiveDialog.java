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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class NewArchiveDialog extends BaseNewDialog {
	private static final String KEY_TITLE = "NewArchiveDialog.title"; //$NON-NLS-1$
	private static final String KEY_PATH = "NewArchiveDialog.path"; //$NON-NLS-1$
	private static final String KEY_URL = "NewArchiveDialog.url"; //$NON-NLS-1$
	private static final String KEY_EMPTY = "NewArchiveDialog.empty"; //$NON-NLS-1$
	private static final String SETTINGS_SECTION = "NewArchiveDialog"; //$NON-NLS-1$
	private static final String S_URL = "url"; //$NON-NLS-1$
	private static final String S_PATH = "path"; //$NON-NLS-1$
	private Text pathText;
	private Text urlText;

	public NewArchiveDialog(Shell shell, ISiteModel siteModel) {
		super(shell, siteModel, null);
	}

	protected void createEntries(Composite container) {
		GridData gd;
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PATH));
		pathText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		pathText.setLayoutData(gd);

		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_URL));
		urlText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		urlText.setLayoutData(gd);
		presetEntries();
	}

	private void presetEntries() {
		IDialogSettings settings = getDialogSettings(SETTINGS_SECTION);
		String initialURL = settings.get(S_URL);
		String initialPath = settings.get(S_PATH);
		setIfDefined(urlText, initialURL);
		setIfDefined(pathText, initialPath);
	}

	protected String getDialogTitle() {
		return PDEPlugin.getResourceString(KEY_TITLE);
	}

	protected String getHelpId() {
		return IHelpContextIds.NEW_ARCHIVE_DIALOG;
	}

	protected String getEmptyErrorMessage() {
		return PDEPlugin.getResourceString(KEY_EMPTY);
	}

	protected void hookListeners(ModifyListener modifyListener) {
		pathText.addModifyListener(modifyListener);
		urlText.addModifyListener(modifyListener);
	}

	protected void dialogChanged() {
		IStatus status = null;
		if (urlText.getText().length() == 0
			|| pathText.getText().length() == 0)
			status = getEmptyErrorStatus();
		else {
			if (hasPath(pathText.getText()))
				status =
					createErrorStatus(PDEPlugin.getResourceString("NewArchiveDialog.alreadyExists")); //$NON-NLS-1$
		}
		if (status == null)
			status = getOKStatus();
		updateStatus(status);
	}

	private boolean hasPath(String path) {
		ISiteModel model = getSiteModel();
		ISiteArchive[] archives = model.getSite().getArchives();
		for (int i = 0; i < archives.length; i++) {
			ISiteArchive archive = archives[i];
			String apath = archive.getPath();
			if (apath != null && apath.equals(path)) {
				return true;
			}
		}
		return false;
	}

	protected void execute() {
		ISiteModel siteModel = getSiteModel();
		ISiteArchive archive = siteModel.getFactory().createArchive();

		try {
			archive.setURL(urlText.getText());
			archive.setPath(pathText.getText());
			siteModel.getSite().addArchives(new ISiteArchive[] { archive });
			IDialogSettings settings = getDialogSettings(SETTINGS_SECTION);
			settings.put(S_PATH, archive.getPath());
			settings.put(S_URL, archive.getURL());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
