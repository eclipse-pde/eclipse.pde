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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class NewArchiveDialog extends BaseNewDialog {
	private Text pathText;
	private Text urlText;

	public NewArchiveDialog(Shell shell, ISiteModel siteModel, ISiteArchive archive) {
		super(shell, siteModel, archive);
	}

	protected void createEntries(Composite container) {
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString("SiteEditor.NewArchiveDialog.path")); //$NON-NLS-1$
		pathText = new Text(container, SWT.SINGLE | SWT.BORDER);
		pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(PDEPlugin.getResourceString("SiteEditor.NewArchiveDialog.url"))); //$NON-NLS-1$
		urlText = new Text(container, SWT.SINGLE | SWT.BORDER);
		urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		ISiteArchive archive = (ISiteArchive)getSiteObject();
		if (archive != null) {
			setIfDefined(urlText, archive.getURL());
			setIfDefined(pathText, archive.getPath());
		}
	}

	protected String getDialogTitle() {
		return PDEPlugin.getResourceString(PDEPlugin.getResourceString("SiteEditor.NewArchiveDialog.title")); //$NON-NLS-1$
	}

	protected String getHelpId() {
		return IHelpContextIds.NEW_ARCHIVE_DIALOG;
	}

	protected String getEmptyErrorMessage() {
		return PDEPlugin.getResourceString(PDEPlugin.getResourceString("SiteEditor.NewArchiveDialog.error")); //$NON-NLS-1$
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
		ISiteArchive thisArchive = (ISiteArchive) getSiteObject();
		String currentPath = thisArchive != null ? thisArchive.getPath() : null;
		
		ISiteModel model = getSiteModel();
		ISiteArchive[] archives = model.getSite().getArchives();
		for (int i = 0; i < archives.length; i++) {
			ISiteArchive archive = archives[i];
			String apath = archive.getPath();
			if (currentPath != null && currentPath.equals(path)) {
				// do not have to change path while editing
				return false;
			}
			if (apath != null && apath.equals(path)) {
				return true;
			}
		}
		return false;
	}
	
	private ISiteArchive getArchive() {
		return (ISiteArchive)getSiteObject();
	}

	protected void execute() {
		ISiteModel siteModel = getSiteModel();
		ISiteArchive archive = getArchive();
		boolean add = (archive == null);
		if (archive == null)
			archive = siteModel.getFactory().createArchive();

		try {
			archive.setURL(urlText.getText());
			archive.setPath(pathText.getText());
			if (add)
				siteModel.getSite().addArchives(new ISiteArchive[] { archive });
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
