/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

public class NewArchiveDialog extends StatusDialog {

	private IStatus fErrorStatus;

	private IStatus fOkStatus;

	private Text fPathText;

	private ISiteArchive fSiteArchive;

	private ISiteModel fSiteModel;

	private Text fUrlText;

	public NewArchiveDialog(Shell shell, ISiteModel siteModel, ISiteArchive archive) {
		super(shell);
		this.fSiteModel = siteModel;
		this.fSiteArchive = archive;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		dialogChanged();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 10;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		container.setLayoutData(gd);

		createEntries(container);

		ModifyListener listener = e -> dialogChanged();
		fPathText.addModifyListener(listener);
		fUrlText.addModifyListener(listener);
		setTitle(PDEUIMessages.SiteEditor_NewArchiveDialog_title);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.NEW_ARCHIVE_DIALOG);
		return container;
	}

	private void createEntries(Composite container) {
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEUIMessages.SiteEditor_NewArchiveDialog_path);
		fPathText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		label = new Label(container, SWT.NULL);
		label.setText(PDEUIMessages.SiteEditor_NewArchiveDialog_url);
		fUrlText = new Text(container, SWT.SINGLE | SWT.BORDER);
		fUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (fSiteArchive != null) {
			setIfDefined(fUrlText, fSiteArchive.getURL());
			setIfDefined(fPathText, fSiteArchive.getPath());
		}
	}

	private IStatus createErrorStatus(String message) {
		return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null);
	}

	private void dialogChanged() {
		IStatus status = null;
		if (fUrlText.getText().length() == 0 || fPathText.getText().length() == 0)
			status = getEmptyErrorStatus();
		else {
			if (hasPath(fPathText.getText()))
				status = createErrorStatus(PDEUIMessages.NewArchiveDialog_alreadyExists);
		}
		if (status == null)
			status = getOKStatus();
		updateStatus(status);
	}

	private void execute() {
		boolean add = (fSiteArchive == null);
		if (fSiteArchive == null)
			fSiteArchive = fSiteModel.getFactory().createArchive();

		try {
			fSiteArchive.setURL(fUrlText.getText());
			fSiteArchive.setPath(fPathText.getText());
			if (add)
				fSiteModel.getSite().addArchives(new ISiteArchive[] {fSiteArchive});
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private IStatus getEmptyErrorStatus() {
		if (fErrorStatus == null)
			fErrorStatus = createErrorStatus(PDEUIMessages.SiteEditor_NewArchiveDialog_error);
		return fErrorStatus;
	}

	private IStatus getOKStatus() {
		if (fOkStatus == null)
			fOkStatus = new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", //$NON-NLS-1$
					null);
		return fOkStatus;
	}

	private boolean hasPath(String path) {
		String currentPath = fSiteArchive != null ? fSiteArchive.getPath() : null;

		ISiteModel model = fSiteModel;
		ISiteArchive[] archives = model.getSite().getArchives();
		for (ISiteArchive archive : archives) {
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

	@Override
	protected void okPressed() {
		execute();
		super.okPressed();
	}

	private void setIfDefined(Text text, String value) {
		if (value != null)
			text.setText(value);
	}
}
