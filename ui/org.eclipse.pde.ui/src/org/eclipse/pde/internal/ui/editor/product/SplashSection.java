/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;


public class SplashSection extends PDESection {

	private FormEntry fPluginEntry;

	public SplashSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.SplashSection_title); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.SplashSection_desc); //$NON-NLS-1$

		Composite client = toolkit.createComposite(section);
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 3;
		layout.topMargin = 5;
		client.setLayout(layout);
		
		Label label = toolkit.createLabel(client, PDEUIMessages.SplashSection_label, SWT.WRAP); //$NON-NLS-1$
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		label.setLayoutData(td);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fPluginEntry = new FormEntry(client, toolkit, PDEUIMessages.SplashSection_plugin, PDEUIMessages.SplashSection_browse, false); //$NON-NLS-1$ //$NON-NLS-2$
		fPluginEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getSplashInfo().setLocation(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
		});
		fPluginEntry.setEditable(isEditable());
				
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	public void refresh() {
		fPluginEntry.setValue(getSplashInfo().getLocation(), true);
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
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	private void handleBrowse() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getDefault().getLabelProvider());
		dialog.setElements(PDECore.getDefault().getModelManager().getWorkspaceModels());
		dialog.setMultipleSelection(false);
		dialog.setTitle(PDEUIMessages.SplashSection_selection); //$NON-NLS-1$
		dialog.setMessage(PDEUIMessages.SplashSection_message); //$NON-NLS-1$
		if (dialog.open() == ElementListSelectionDialog.OK) {
			IPluginModelBase model = (IPluginModelBase)dialog.getFirstResult();
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

}
