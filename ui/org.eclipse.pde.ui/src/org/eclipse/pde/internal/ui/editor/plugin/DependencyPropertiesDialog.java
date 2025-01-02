/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.PluginVersionPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

// TODO this needs a rewrite along with PluginVersionPart
public class DependencyPropertiesDialog extends StatusDialog {

	private Button fReexportButton;
	private Button fOptionalButton;
	private boolean fEditable;
	private boolean fShowReexport;

	private boolean fExported;
	private boolean fOptional;

	private PluginVersionPart fVersionPart;

	private boolean fShowOptional;
	private String fVersion;
	private String fPluginId;

	public DependencyPropertiesDialog(boolean editable, IPluginImport plugin) {
		this(editable, true, plugin.isReexported(), plugin.isOptional(), plugin.getVersion(), true, true,
				plugin.getId(), true);
	}

	public DependencyPropertiesDialog(boolean editable, ImportPackageObject object) {
		this(editable, false, false, object.isOptional(), object.getVersion(), true, true, object.getName(), false);
	}

	public DependencyPropertiesDialog(boolean editable, ExportPackageObject object) {
		this(editable, false, false, false, object.getVersion(), false, false, null, false);
	}

	public DependencyPropertiesDialog(boolean editable, boolean showReexport, boolean export, boolean optional,
			String version, boolean showOptional, boolean isImport, String pluginId, boolean isPlugin) {
		super(PDEPlugin.getActiveWorkbenchShell());
		fEditable = editable;
		fShowReexport = showReexport;
		fExported = export;
		fOptional = optional;
		fShowOptional = showOptional;
		fPluginId = pluginId;
		if (isImport)
			fVersionPart = new PluginVersionPart(true, isPlugin);
		else
			fVersionPart = new PluginVersionPart(false, isPlugin) {
				@Override
				protected String getGroupText() {
					return PDEUIMessages.DependencyPropertiesDialog_exportGroupText;
				}
			};
		fVersionPart.setVersion(version);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, PDEUIMessages.DependencyPropertiesDialog_closeButtonLabel, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);

		if (fShowOptional || fShowReexport) {
			Group container = new Group(comp, SWT.NONE);
			container.setText(PDEUIMessages.DependencyPropertiesDialog_properties);
			container.setLayout(new GridLayout());
			container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			if (fShowOptional) {
				fOptionalButton = new Button(container, SWT.CHECK);
				fOptionalButton.setText(PDEUIMessages.DependencyPropertiesDialog_optional);
				GridData gd = new GridData();
				gd.horizontalSpan = 2;
				fOptionalButton.setLayoutData(gd);
				fOptionalButton.setEnabled(fEditable);
				fOptionalButton.setSelection(fOptional);
			}

			if (fShowReexport) {
				fReexportButton = new Button(container, SWT.CHECK);
				fReexportButton.setText(PDEUIMessages.DependencyPropertiesDialog_reexport);
				GridData gd = new GridData();
				gd.horizontalSpan = 2;
				fReexportButton.setLayoutData(gd);
				fReexportButton.setEnabled(fEditable);
				fReexportButton.setSelection(fExported);
			}
		}

		fVersionPart.createVersionFields(comp, true, fEditable);
		ModifyListener ml = e -> updateStatus(fVersionPart.validateFullVersionRangeText(true));
		fVersionPart.addListeners(ml, ml);

		if (fPluginId != null && !fPluginId.equals("system.bundle")) //$NON-NLS-1$
			fVersionPart.createVersionSelectionField(comp, fPluginId);

		return comp;
	}

	public boolean isReexported() {
		return fExported;
	}

	public boolean isOptional() {
		return fOptional;
	}

	public String getVersion() {
		return fVersion;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void okPressed() {
		fOptional = (fOptionalButton == null) ? false : fOptionalButton.getSelection();
		fExported = (fReexportButton == null) ? false : fReexportButton.getSelection();

		fVersion = fVersionPart.getVersion();

		super.okPressed();
	}
}
