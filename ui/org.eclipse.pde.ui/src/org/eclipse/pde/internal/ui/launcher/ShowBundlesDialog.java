/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;


import java.util.Map;
import java.util.StringJoiner;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class ShowBundlesDialog extends Dialog {
	private Text fModuleArgumentsText;
	private final Map<IPluginModelBase, String> fModelsWithStartLevels;


	protected ShowBundlesDialog(Shell parentShell, Map<IPluginModelBase, String> modelsWithLevels) {
		super(parentShell);
		fModelsWithStartLevels = modelsWithLevels;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(PDEUIMessages.ShowBundlesDialog_LaunchBundles);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID,
				PDEUIMessages.ShowBundlesDialog_Copy, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				PDEUIMessages.ShowBundlesDialog_Close, false);
	}

	@Override
	protected Rectangle getConstrainedShellBounds(Rectangle preferredSize) {
		Rectangle result = super.getConstrainedShellBounds(preferredSize);
		int heightLimit = convertHeightInCharsToPixels(40);
		int widthLimit = convertWidthInCharsToPixels(150);
		if (result.height > heightLimit) {
			result.y += (result.height - heightLimit) / 2;
			result.height = heightLimit;
		}
		if (result.width > widthLimit) {
			result.x += (result.width - widthLimit) / 2;
			result.width = widthLimit;
		}
		return result;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		Label explanation = new Label(comp, SWT.NONE);
		explanation.setText("List of all bundles contained in this launch using the schema:\n" //$NON-NLS-1$
				+ "<Symbolic Name>, <Version>, <Start level>, <File Path>"); //$NON-NLS-1$
		explanation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fModuleArgumentsText = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		fModuleArgumentsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		StringJoiner lines = new StringJoiner("\n"); //$NON-NLS-1$
		fModelsWithStartLevels.forEach((model, value) -> {
			BundleDescription bundle = model.getBundleDescription();
			String startLevel = value.substring(0, value.indexOf(':'));
			lines.add(bundle.getSymbolicName() + ", " + bundle.getVersion() + ", " + startLevel + ", " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ model.getInstallLocation());
		});
		fModuleArgumentsText.setText(lines.toString());
		fModuleArgumentsText.setEditable(false);

		return comp;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == OK) {
			Clipboard clipboard = new Clipboard(null);
			try {
				Transfer[] transfers = { TextTransfer.getInstance() };
				Object[] data = { fModuleArgumentsText.getText() };
				clipboard.setContents(data, transfers);
			} finally {
				clipboard.dispose();
			}
		}
		super.buttonPressed(buttonId);
	}

}
