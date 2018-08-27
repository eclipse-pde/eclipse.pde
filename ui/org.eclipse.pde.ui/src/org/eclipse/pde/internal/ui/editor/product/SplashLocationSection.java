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
 *     Simon Scholz <simon.scholz@vogella.com> - bug 440275
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class SplashLocationSection extends PDESection {

	private FormEntry fPluginEntry;

	public SplashLocationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {

		// Configure section
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);
		section.setText(PDEUIMessages.SplashSection_title);
		section.setDescription(PDEUIMessages.SplashSection_desc);
		// Create and configure client
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		// Create form entry
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fPluginEntry = new FormEntry(client, toolkit, PDEUIMessages.SplashSection_plugin, PDEUIMessages.SplashSection_browse, false);
		fPluginEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				getSplashInfo().setLocation(entry.getValue(), false);
			}

			@Override
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
		});
		fPluginEntry.setEditable(isEditable());

		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	@Override
	public void refresh() {
		ISplashInfo info = getSplashInfo();
		fPluginEntry.setValue(info.getLocation(), true);
		super.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		fPluginEntry.commit();
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		fPluginEntry.cancelEdit();
		super.cancelEdit();
	}

	private ISplashInfo getSplashInfo() {
		ISplashInfo info = getProduct().getSplashInfo();
		if (info == null) {
			info = getModel().getFactory().createSplashInfo();
			getProduct().setSplashInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	private void handleBrowse() {

		PluginSelectionDialog pluginSelectionDialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PluginRegistry.getActiveModels(), false);

		pluginSelectionDialog.setTitle(PDEUIMessages.SplashSection_selection);
		pluginSelectionDialog.setMessage(PDEUIMessages.SplashSection_message);
		if (pluginSelectionDialog.open() == Window.OK) {
			IPluginModelBase model = (IPluginModelBase) pluginSelectionDialog.getFirstResult();
			fPluginEntry.setValue(model.getPluginBase().getId());
		}
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

}
