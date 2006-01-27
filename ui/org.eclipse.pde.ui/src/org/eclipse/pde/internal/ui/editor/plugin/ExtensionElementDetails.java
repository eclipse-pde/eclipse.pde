/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.plugin.rows.BooleanAttributeRow;
import org.eclipse.pde.internal.ui.editor.plugin.rows.ChoiceAttributeRow;
import org.eclipse.pde.internal.ui.editor.plugin.rows.ClassAttributeRow;
import org.eclipse.pde.internal.ui.editor.plugin.rows.ExtensionAttributeRow;
import org.eclipse.pde.internal.ui.editor.plugin.rows.ResourceAttributeRow;
import org.eclipse.pde.internal.ui.editor.plugin.rows.TextAttributeRow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


public class ExtensionElementDetails extends PDEDetails {
	private IPluginElement input;
	private ISchemaElement schemaElement;
	private ArrayList rows;
	private Section section;
	/**
	 *  
	 */
	public ExtensionElementDetails(ISchemaElement schemaElement) {
		this.schemaElement = schemaElement;
		rows = new ArrayList();
	}
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public void createContents(Composite parent) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.leftMargin = 5;
		layout.rightMargin = 0;
		layout.bottomMargin = 0;
		parent.setLayout(layout);
		FormToolkit toolkit = getManagedForm().getToolkit();
		section = toolkit.createSection(parent, Section.TITLE_BAR
				| Section.DESCRIPTION);
		section.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		section.marginHeight = 5;
		section.marginWidth = 5;
		section.setText(PDEUIMessages.ExtensionElementDetails_title); 
		section.setDescription(PDEUIMessages.ExtensionElementDetails_desc); 
		TableWrapData td = new TableWrapData(TableWrapData.FILL,
				TableWrapData.TOP);
		td.grabHorizontal = true;
		section.setLayoutData(td);
		//toolkit.createCompositeSeparator(section);
		Composite client = toolkit.createComposite(section);
		GridLayout glayout = new GridLayout();
		boolean paintedBorder = toolkit.getBorderStyle() != SWT.BORDER;
		glayout.marginWidth = glayout.marginHeight = 2;//paintedBorder?2:0;
		int span = 2;
		glayout.numColumns = span;
		if (paintedBorder)
			glayout.verticalSpacing = 7;
		client.setLayout(glayout);
		if (schemaElement != null) {
			ISchemaAttribute atts[] = schemaElement.getAttributes();
			// Compute horizontal span
			for (int i = 0; i < atts.length; i++) {
				if (atts[i].getKind() == ISchemaAttribute.JAVA
						|| atts[i].getKind() == ISchemaAttribute.RESOURCE) {
					span = 3;
					break;
				}
			}
			glayout.numColumns = span;
			// Add required attributes first
			for (int i = 0; i < atts.length; i++) {
				if (atts[i].getUse() == ISchemaAttribute.REQUIRED)
					rows
							.add(createAttributeRow(atts[i], client, toolkit,
									span));
			}
			// Add the rest
			for (int i = 0; i < atts.length; i++) {
				if (atts[i].getUse() != ISchemaAttribute.REQUIRED)
					rows
							.add(createAttributeRow(atts[i], client, toolkit,
									span));
			}
			createSpacer(toolkit, client, span);
		}
		else {
			// no schema - delay until input is set
		}
		toolkit.paintBordersFor(client);
		section.setClient(client);
		markDetailsPart(section);
	}
	private ExtensionAttributeRow createAttributeRow(ISchemaAttribute att,
			Composite parent, FormToolkit toolkit, int span) {
		ExtensionAttributeRow row;
		if (att.getKind() == ISchemaAttribute.JAVA)
			row = new ClassAttributeRow(this, att);
		else if (att.getKind() == ISchemaAttribute.RESOURCE)
			row = new ResourceAttributeRow(this, att);
		else {
			ISchemaSimpleType type = att.getType();
			if (type.getName().equals("boolean")) //$NON-NLS-1$
				row = new BooleanAttributeRow(this, att);
			else {
				ISchemaRestriction restriction = type.getRestriction();
				if (restriction != null)
					row = new ChoiceAttributeRow(this, att);
				else
					row = new TextAttributeRow(this, att);
			}
		}
		row.createContents(parent, toolkit, span);
		return row;
	}
	
	private ExtensionAttributeRow createAttributeRow(IPluginAttribute att,
			Composite parent, FormToolkit toolkit, int span) {
		ExtensionAttributeRow row;
		row = new TextAttributeRow(this, att);
		row.createContents(parent, toolkit, span);
		return row;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#inputChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			input = (IPluginElement) ssel.getFirstElement();
		} else
			input = null;
		update();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType()==IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(input))
				refresh();
		}
	}
	
	private void update() {
		updateDescription();
		if (schemaElement==null)
			updateRows();
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			row.setInput(input);
		}
	}
	private void updateRows() {
		if (input==null) return;
		IPluginAttribute [] atts = input.getAttributes();
		FormToolkit toolkit = getManagedForm().getToolkit();
		boolean rowsAdded=false;
		for (int i=0; i<atts.length; i++) {
			if (!hasAttribute(atts[i].getName())) {
				rows.add(createAttributeRow(atts[i], (Composite)section.getClient(), 
						toolkit, 2)); 
				rowsAdded=true;
			}
		}
		if (rowsAdded) {
			((Composite)section.getClient()).layout(true);
			section.layout(true);
			section.getParent().layout(true);
			reflow();
		}
	}
	private void reflow() {
		Composite parent = section.getParent();
		while (parent!=null) {
			if (parent instanceof SharedScrolledComposite) {
				((SharedScrolledComposite)parent).reflow(true);
				return;
			}
			parent = parent.getParent();
		}
	}
	private boolean hasAttribute(String attName) {
		for (int i=0; i<rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow)rows.get(i);
			if (row.getName().equals(attName))
				return true;
		}
		return false;
	}
	private void updateDescription() {
		if (input != null) {
			String iname = input.getName();
			section.setDescription(NLS.bind(PDEUIMessages.ExtensionElementDetails_setDesc, iname)); 
		} else {
			section
					.setDescription(PDEUIMessages.ExtensionElementDetails_setSelectedDesc); 
		}
		section.layout();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#commit()
	 */
	public void commit(boolean onSave) {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			row.commit();
		}
		super.commit(onSave);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#setFocus()
	 */
	public void setFocus() {
		if (rows.size() > 0)
			((ExtensionAttributeRow) rows.get(0)).setFocus();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#dispose()
	 */
	public void dispose() {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			row.dispose();
		}
		IPluginModelBase model = (IPluginModelBase)getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.IDetailsPage#refresh()
	 */
	public void refresh() {
		update();
		super.refresh();
	}
}
