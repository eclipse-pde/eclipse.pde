/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - Provide more structure, safety, and convenience for ID-based references between extension points (id hell)
 *     Brian de Alwis (MTI) - bug 429420
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
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
	private ArrayList<ExtensionAttributeRow> rows;
	private Section section;

	/**
	 * @param masterSection
	 * @param schemaElement
	 */
	public ExtensionElementDetails(PDESection masterSection, ISchemaElement schemaElement) {
		super(masterSection);
		this.schemaElement = schemaElement;
		rows = new ArrayList<>();
	}

	@Override
	public String getContextId() {
		return PluginInputContext.CONTEXT_ID;
	}

	@Override
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	@Override
	public PDEFormPage getPage() {
		return (PDEFormPage) getManagedForm().getContainer();
	}

	@Override
	public boolean isEditable() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return model != null && model.isEditable();
	}

	@Override
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
				for (ISchemaAttribute attribute : atts) {
					if (attribute.getKind() == IMetaAttribute.JAVA || attribute.getKind() == IMetaAttribute.RESOURCE || attribute.getKind() == IMetaAttribute.IDENTIFIER) {
						span = 3;
						break;
					}
				}
			}
			glayout.numColumns = span;
			// Add required attributes first
			for (ISchemaAttribute attribute : atts) {
				if (attribute.getUse() == ISchemaAttribute.REQUIRED)
					rows.add(createAttributeRow(attribute, client, toolkit, span));
			}
			// Add the non-required attributes
			for (ISchemaAttribute attribute : atts) {
				if (attribute.getUse() != ISchemaAttribute.REQUIRED)
					rows.add(createAttributeRow(attribute, client, toolkit, span));
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

	@Override
	public void selectionChanged(IFormPart masterPart, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() == 1) {
			input = (IPluginElement) ssel.getFirstElement();
		} else
			input = null;
		update();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj.equals(input)) {
				// do smart update (update only the row whose property changed
				String property = e.getChangedProperty();
				if (property != null) {
					for (int i = 0; i < rows.size(); i++) {
						ExtensionAttributeRow row = rows.get(i);
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
			ExtensionAttributeRow row = rows.get(i);
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
			ExtensionAttributeRow row = rows.get(i);
			if (row.getName().equals(attName))
				return true;
		}
		return false;
	}

	private void updateDescription() {
		if (input != null) {
			String iname = input.getName();
			String label = null;
			if (0 == input.getAttributeCount()) {
				label = PDEUIMessages.ExtensionElementDetails_descNoAttributes;
			} else if (schemaElement != null && schemaElement.hasDeprecatedAttributes()) {
				label = NLS.bind(PDEUIMessages.ExtensionElementDetails_setDescDepr, iname);
			} else {
				label = NLS.bind(PDEUIMessages.ExtensionElementDetails_setDesc, iname);
			}
			if (schemaElement != null && schemaElement.isDeprecated()) {
				label += "\n\n"; //$NON-NLS-1$
				label += NLS.bind(PDEUIMessages.ElementIsDeprecated, iname);
			}
			section.setDescription(label);
		} else {
			// no extensions = no description
			section.setDescription(""); //$NON-NLS-1$
		}
		section.layout();
	}

	@Override
	public void commit(boolean onSave) {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = rows.get(i);
			row.commit();
		}
		super.commit(onSave);
	}

	@Override
	public void setFocus() {
		if (!rows.isEmpty())
			rows.get(0).setFocus();
	}

	@Override
	public void dispose() {
		for (int i = 0; i < rows.size(); i++) {
			ExtensionAttributeRow row = rows.get(i);
			row.dispose();
		}
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	public void refresh() {
		update();
		super.refresh();
	}
}
