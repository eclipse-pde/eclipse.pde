/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;
import java.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.plugin.rows.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;


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
		section.setText(PDEPlugin.getResourceString("ExtensionElementDetails.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("ExtensionElementDetails.desc")); //$NON-NLS-1$
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
			section.setDescription(PDEPlugin.getFormattedMessage("ExtensionElementDetails.setDesc", iname)); //$NON-NLS-1$
		} else {
			section
					.setDescription(PDEPlugin.getResourceString("ExtensionElementDetails.setSelectedDesc")); //$NON-NLS-1$
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