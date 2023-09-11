/*******************************************************************************
 *  Copyright (c) 2005, 2021 IBM Corporation and others.
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
 *     Wim Jongman - Refactor into reusable class
 *******************************************************************************/
package org.eclipse.pde.internal.ui.dialogs;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.ui.dialogs.TextURLDropAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.widgets.WidgetFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A simple dialog that forces the user to enter a syntactical valid URL.
 *
 */
public class RepositoryDialog extends StatusDialog {
	private Text fLocation;
	private final String fRepoURL;
	private String fLocationStr;

	/**
	 * @param shell
	 *            the shell to use for the dialog which may not be null
	 * @param repoURL
	 *            The initial value of the URL, which may be null
	 */
	public RepositoryDialog(Shell shell, String repoURL) {
		super(shell);
		fRepoURL = repoURL;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		((GridLayout) comp.getLayout()).numColumns = 2;
		WidgetFactory.label(SWT.NONE).text(PDEUIMessages.UpdatesSection_Location).create(comp);
		fLocation = WidgetFactory.text(SWT.BORDER).create(comp);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.ENTRY_FIELD_WIDTH);
		fLocation.setLayoutData(data);
		DropTarget target = new DropTarget(fLocation, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK);
		target.setTransfer(new Transfer[] { URLTransfer.getInstance(), FileTransfer.getInstance() });
		target.addDropListener(new TextURLDropAdapter(fLocation, true));
		fLocation.addModifyListener(e -> validate());

		if (fRepoURL != null) {
			fLocation.setText(fRepoURL);
		}
		validate();
		return comp;
	}

	protected void validate() {
		fLocationStr = fLocation.getText().trim();
		updateStatus(isValidURL(fLocationStr));
	}

	private IStatus isValidURL(String location) {
		if (location.length() == 0) {
			return Status.error(PDEUIMessages.UpdatesSection_ErrorInvalidURL);
		}
		if (!(location.startsWith("http://") //$NON-NLS-1$
				|| location.startsWith("https://") //$NON-NLS-1$
				|| location.startsWith("file:/"))) { //$NON-NLS-1$
			return Status.error(PDEUIMessages.UpdatesSection_ErrorInvalidURL);
		}
		try {
			URL url = new URL(location);
			if (url.getHost().trim().isBlank() && url.getPath().isBlank()) {
				return Status.error(PDEUIMessages.UpdatesSection_ErrorInvalidURL);
			}
		} catch (MalformedURLException e) {
			return Status.error(PDEUIMessages.UpdatesSection_ErrorInvalidURL);
		}
		return Status.OK_STATUS;
	}

	@Override
	protected Control createHelpControl(Composite parent) {
		return parent;
	}

	/**
	 * If the result of this dialog open() is {@link Window#OK} then this method
	 * will return a http, https or file url. Otherwise the result is
	 * unpredictable.
	 *
	 * @return the repository URL
	 */
	public String getResult() {
		return fLocationStr;
	}

}