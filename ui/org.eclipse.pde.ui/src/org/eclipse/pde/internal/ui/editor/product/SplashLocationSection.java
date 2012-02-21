/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class SplashLocationSection extends PDESection {

	private FormEntry fPluginEntry;

	public SplashLocationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
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
			public void textValueChanged(FormEntry entry) {
				getSplashInfo().setLocation(entry.getValue(), false);
			}

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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		ISplashInfo info = getSplashInfo();
		fPluginEntry.setValue(info.getLocation(), true);
		super.refresh();
	}

	public void commit(boolean onSave) {
		fPluginEntry.commit();
		super.commit(onSave);
	}

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
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getDefault().getLabelProvider());
		dialog.setElements(PluginRegistry.getActiveModels());
		dialog.setMultipleSelection(false);
		dialog.setTitle(PDEUIMessages.SplashSection_selection);
		dialog.setMessage(PDEUIMessages.SplashSection_message);
		if (dialog.open() == Window.OK) {
			IPluginModelBase model = (IPluginModelBase) dialog.getFirstResult();
			fPluginEntry.setValue(model.getPluginBase().getId());
		}
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

}
