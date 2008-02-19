/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - Provide more structure, safety, and convenience for ID-based references between extension points (id hell)
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.plugin.rows.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.*;

public class ExtensionElementDetails extends AbstractPluginElementDetails {
	private IPluginElement input;
	private ISchemaElement schemaElement;
	private ArrayList rows;
	private Section section;

	/**
	 * @param masterSection
	 * @param schemaElement
	 */
	public ExtensionElementDetails(PDESection masterSection, ISchemaElement schemaElement) {
		super(masterSection);
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
		parent.setLayout(FormLayoutFactory.createDetailsGridLayout(false, 1));
		FormToolkit toolkit = getManagedForm().getToolkit();
		section = toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
		section.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		section.setText(PDEUIMessages.ExtensionElementDetails_title);
		section.setDescription(""); //$NON-NLS-1$
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_BEGINNING));

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), section);

		Composite client = toolkit.createComposite(section);
		int span = 2;
		GridLayout glayout = FormLayoutFactory.createSectionClientGridLayout(false, span);
		client.setLayout(glayout);
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (schemaElement != null) {
			ISchemaAttribute atts[] = schemaElement.getAttributes();
			if (isEditable()) {
				// Compute horizontal span
				for (int i = 0; i < atts.length; i++) {
					if (atts[i].getKind() == IMetaAttribute.JAVA || atts[i].getKind() == IMetaAttribute.RESOURCE || atts[i].getKind() == IMetaAttribute.IDENTIFIER) {
						span = 3;
						break;
					}
				}
			}
			glayout.numColumns = span;
			// Add required attributes first
			for (int i = 0; i < atts.length; i++) {
				if (atts[i].getUse() == ISchemaAttribute.REQUIRED)
					rows.add(createAttributeRow(atts[i], client, toolkit, span));
			}
			// Add the rest
			for (int i = 0; i < atts.length; i++) {
				if (atts[i].getUse() != ISchemaAttribute.REQUIRED)
					rows.add(createAttributeRow(atts[i], client, toolkit, span));
			}
			createSpacer(toolkit, client, span);
		} else {
			// no schema - delay until input is set
		}
		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Dynamically add focus listeners to all the section client's 
		// children in order to track the last focus control
		getPage().addLastFocusListeners(client);

		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.addModelChangedListener(this);
		markDetailsPart(section);
	}

	private ExtensionAttributeRow createAttributeRow(ISchemaAttribute att, Composite parent, FormToolkit toolkit, int span) {
		ExtensionAttributeRow row;
		if (att.getKind() == IMetaAttribute.JAVA)
			row = new ClassAttributeRow(this, att);
		else if (att.getKind() == IMetaAttribute.RESOURCE)
			row = new ResourceAttributeRow(this, att);
		else if (att.getKind() == IMetaAttribute.IDENTIFIER)
			row = new IdAttributeRow(this, att);
		else if (att.isTranslatable())
			row = new TranslatableAttributeRow(this, att);
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

	private ExtensionAttributeRow createAttributeRow(IPluginAttribute att, Composite parent, FormToolkit toolkit, int span) {
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
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(input)) {
				// do smart update (update only the row whose property changed
				String property = e.getChangedProperty();
				if (property != null) {
					for (int i = 0; i < rows.size(); i++) {
						ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
						ISchemaAttribute attribute = row.getAttribute();
						if (attribute == null) {
							continue;
						}
						String name = attribute.getName();
						if (name == null) {
							continue;
						}
						if (name.equals(property)) {
							row.setInput(input);
						}
					}
				} else
					refresh();
			}
		}
	}

	private void update() {
		updateDescription();
		if (schemaElement == null)
			updateRows();
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			row.setInput(input);
		}
	}

	private void updateRows() {
		if (input == null)
			return;
		IPluginAttribute[] atts = input.getAttributes();
		FormToolkit toolkit = getManagedForm().getToolkit();
		boolean rowsAdded = false;
		for (int i = 0; i < atts.length; i++) {
			if (!hasAttribute(atts[i].getName())) {
				rows.add(createAttributeRow(atts[i], (Composite) section.getClient(), toolkit, 2));
				rowsAdded = true;
			}
		}
		if (rowsAdded) {
			((Composite) section.getClient()).layout(true);
			section.layout(true);
			section.getParent().layout(true);
			reflow();
		}
	}

	private void reflow() {
		Composite parent = section.getParent();
		while (parent != null) {
			if (parent instanceof SharedScrolledComposite) {
				((SharedScrolledComposite) parent).reflow(true);
				return;
			}
			parent = parent.getParent();
		}
	}

	private boolean hasAttribute(String attName) {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = (ExtensionAttributeRow) rows.get(i);
			if (row.getName().equals(attName))
				return true;
		}
		return false;
	}

	private void updateDescription() {
		if (input != null) {
			if (0 == input.getAttributeCount()) {
				section.setDescription(PDEUIMessages.ExtensionElementDetails_descNoAttributes);
			} else {
				String iname = input.getName();
				section.setDescription(NLS.bind(PDEUIMessages.ExtensionElementDetails_setDesc, iname));
			}
		} else {
			// no extensions = no description
			section.setDescription(""); //$NON-NLS-1$
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
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model != null)
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
