/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *	   Remy Chi Jian Suen <remy.suen@gmail.com> - Bug 201965 [Schema][Editors] Inappropriate selection behaviour after delete attempt in non-editable editor
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class SchemaAttributeDetails extends AbstractSchemaDetails {

	private SchemaAttribute fAttribute;
	private FormEntry fValue;
	private FormEntry fName;
	private Button fDepTrue;
	private Button fDepFalse;
	private ComboPart fType;
	private ComboPart fUseDefault;
	private ComboPart fUseOther;
	private StackLayout fUseLayout;
	private Composite fUseComp;
	private Composite fUseCompDefault;
	private Composite fUseCompOther;

	public SchemaAttributeDetails(ElementSection section) {
		super(section, false, true);
	}

	class SchemaAttributeContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			ISchemaSimpleType type = fAttribute.getType();
			ISchemaRestriction restriction = type.getRestriction();
			if (restriction != null)
				return restriction.getChildren();
			return new Object[0];
		}
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);

		fName = new FormEntry(parent, toolkit, PDEUIMessages.SchemaDetails_name, SWT.NONE);
		// Ensures label columns on every detail page are same width
		((GridData) fName.getLabel().getLayoutData()).widthHint = minLabelWeight;

		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		label.setForeground(foreground);
		Button[] buttons = createTrueFalseButtons(parent, toolkit, 2);
		fDepTrue = buttons[0];
		fDepFalse = buttons[1];

		label = toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_use);
		label.setForeground(foreground);

		fUseComp = toolkit.createComposite(parent);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fUseComp.setLayoutData(gd);
		fUseLayout = new StackLayout();
		fUseComp.setLayout(fUseLayout);

		fUseCompDefault = toolkit.createComposite(fUseComp);
		fUseCompDefault.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(2).create());

		fUseDefault = createComboPart(fUseCompDefault, toolkit, ISchemaAttribute.USE_TABLE, 1, SWT.NONE);
		fValue = new FormEntry(fUseCompDefault, toolkit, null, 0, 1);

		fUseCompOther = toolkit.createComposite(fUseComp);
		fUseCompOther.setLayout(GridLayoutFactory.fillDefaults().margins(0, 0).create());

		fUseOther = createComboPart(fUseCompOther, toolkit, ISchemaAttribute.USE_TABLE, 1);

		label = toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_type);
		label.setForeground(foreground);
		fType = createComboPart(parent, toolkit, ISchemaAttribute.TYPES, 2);

		createTypeDetails(parent, toolkit);

		toolkit.paintBordersFor(parent);
		setText(PDEUIMessages.SchemaAttributeDetails_title);
	}

	protected abstract void createTypeDetails(Composite parent, FormToolkit toolkit);

	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaAttribute))
			return;
		fAttribute = (SchemaAttribute) object;
		setDecription(NLS.bind(PDEUIMessages.SchemaAttributeDetails_description, fAttribute.getName()));
		fName.setValue(fAttribute.getName(), true); 
		fDepTrue.setSelection(fAttribute.isDeprecated());
		fDepFalse.setSelection(!fAttribute.isDeprecated());

		boolean isStringType = fAttribute.getType().getName().equals(ISchemaAttribute.TYPES[ISchemaAttribute.STR_IND]);
		int kind = fAttribute.getKind();
		fType.select(isStringType ? 1 + kind : 0);

		fUseDefault.select(fAttribute.getUse());
		fUseOther.select(fAttribute.getUse());
		Object value = fAttribute.getValue();
		fValue.setValue(value != null ? value.toString() : PDEUIMessages.SchemaAttributeDetails_defaultDefaultValue, true);

		boolean editable = isEditableElement();
		if (fAttribute.getUse() != 2) {
			fUseLayout.topControl = fUseCompOther;
		} else {
			fUseLayout.topControl = fUseCompDefault;
		}
		fUseComp.layout();
		fName.setEditable(editable);
		fDepTrue.setEnabled(editable);
		fDepFalse.setEnabled(editable);
		fType.setEnabled(editable);
		fUseDefault.setEnabled(editable);
		fUseOther.setEnabled(editable);
		fValue.setEditable(editable);
	}

	public void hookListeners() {
		fValue.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				fAttribute.setValue(fValue.getValue());
			}
		});
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				boolean revert = false;
				if (fName.getValue().length() == 0)
					revert = true;
				else {
					ISchemaObject parent = fAttribute.getParent();
					while (!(parent instanceof ISchemaElement))
						parent = parent.getParent();
					ISchemaElement element = (ISchemaElement) parent;
					ISchemaAttribute[] attributes = element.getAttributes();
					for (int i = 0; i < attributes.length; i++) {
						if (attributes[i] != fAttribute && attributes[i].getName().equalsIgnoreCase(fName.getValue())) {
							revert = true;
							break;
						}
					}
				}
				if (revert)
					fName.setValue(fAttribute.getName(), true);
				else {
					fAttribute.setName(fName.getValue());
					setDecription(NLS.bind(PDEUIMessages.SchemaAttributeDetails_description, fAttribute.getName()));
				}
			}
		});
		fDepTrue.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				fAttribute.setDeprecatedProperty(fDepTrue.getSelection());
			}
		});
		fType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				String typeString = fType.getSelection();
				if (!typeString.equals(ISchemaAttribute.TYPES[ISchemaAttribute.BOOL_IND]))
					typeString = ISchemaAttribute.TYPES[ISchemaAttribute.STR_IND];

				fAttribute.setType(new SchemaSimpleType(fAttribute.getSchema(), typeString));

				int kind = fType.getSelectionIndex() - 1; // adjust for "boolean" in combo
				fAttribute.setKind(kind > 0 ? kind : 0); // kind could be -1

				ISchemaSimpleType type = fAttribute.getType();
				if (type instanceof SchemaSimpleType && kind != IMetaAttribute.STRING && ((SchemaSimpleType) type).getRestriction() != null) {
					((SchemaSimpleType) type).setRestriction(null);
				}
				fireSelectionChange();
			}
		});
		fUseDefault.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				int i = fUseDefault.getSelectionIndex();
				setBlockListeners(true);
				fUseOther.select(i);
				setBlockListeners(false);
				doUseChange(i);
			}
		});
		fUseOther.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				int i = fUseOther.getSelectionIndex();
				setBlockListeners(true);
				fUseDefault.select(i);
				setBlockListeners(false);
				doUseChange(i);
			}
		});
	}

	private void doUseChange(int index) {
		fAttribute.setUse(index);
		if (index == 2) {
			fUseLayout.topControl = fUseCompDefault;
			fUseComp.layout();
			if (fValue.getValue().equals(PDEUIMessages.SchemaAttributeDetails_defaultDefaultValue))
				fValue.getText().setSelection(0, fValue.getValue().length());
			fValue.getText().setFocus();
		} else if (index != 2) {
			fUseLayout.topControl = fUseCompOther;
			fUseComp.layout();
			fValue.setValue(PDEUIMessages.SchemaAttributeDetails_defaultDefaultValue);
			fUseCompOther.setFocus();
		}
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] changedObjs = event.getChangedObjects();
		if (event.getChangeType() == IModelChangedEvent.INSERT && changedObjs.length > 0) {
			if (changedObjs[0] instanceof SchemaAttribute) {
				fName.getText().setFocus();
			}
		}
		super.modelChanged(event);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fName.commit();
		fValue.commit();
	}

	protected SchemaAttribute getAttribute() {
		return fAttribute;
	}
}
