/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 212758
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.List;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * This is a simple info dialog capable to display list of given plugins. The list should
 * contain elements recognizable by {@link PDELabelProvider}.
 */
public class PluginImportFinishDialog extends TitleAreaDialog {

	private TableViewer fPluginListViewer;
	private List<?> fPluginList;
	private String fTitle;
	private String fMessage;
	private boolean fConfigured;

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		super.setTitle(fTitle);
		super.setMessage(fMessage, IMessageProvider.INFORMATION);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		composite.setLayout(new GridLayout(1, true));
		fPluginListViewer = new TableViewer(composite);
		fPluginListViewer.setLabelProvider(new PDELabelProvider());
		fPluginListViewer.setContentProvider(new PluginImportTableContentProvider());
		fPluginListViewer.setInput(fPluginList);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 100;
		data.heightHint = 200;
		fPluginListViewer.getTable().setLayoutData(data);
		fConfigured = true;
		return composite;

	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(PDEUIMessages.ImportWizard_title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IHelpContextIds.PLUGIN_IMPORT_FINISH_DIALOG);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	public PluginImportFinishDialog(Shell parentShell) {
		super(parentShell);
		fConfigured = false;
	}

	public final void setInput(List<?> pluginList) {
		fPluginList = pluginList;
	}

	static private class PluginImportTableContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<?>) inputElement).toArray();
		}
	}

	@Override
	public void setMessage(String newMessage) {
		fMessage = newMessage;
		if (fConfigured)
			super.setMessage(fMessage, IMessageProvider.INFORMATION);
	}

	@Override
	public void setTitle(String newTitle) {
		fTitle = newTitle;
		if (fConfigured)
			super.setTitle(fTitle);
	}

}
