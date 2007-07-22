/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class SchemaAttributeDetails extends AbstractSchemaDetails {
	
	private SchemaAttribute fAttribute;
	private FormEntry fValue;
	private FormEntry fName;
	private Button fDepTrue;
	private Button fDepFalse;
	private ComboPart fType;
	private ComboPart fUse;
	
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
		
		Label label = toolkit.createLabel(parent, PDEUIMessages.SchemaDetails_deprecated);
		label.setForeground(foreground);
		Button[] buttons = createTrueFalseButtons(parent, toolkit, 2);
		fDepTrue = buttons[0];
		fDepFalse = buttons[1];

		label = toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_type);
		label.setForeground(foreground);
		fType = createComboPart(parent, toolkit, ISchemaAttribute.TYPES, 2);
		
		createTypeDetails(parent, toolkit);
		
		label = toolkit.createLabel(parent, PDEUIMessages.SchemaAttributeDetails_use);
		label.setForeground(foreground);
		fUse = createComboPart(parent, toolkit, ISchemaAttribute.USE_TABLE, 2);
		
		fValue = new FormEntry(parent, toolkit, PDEUIMessages.SchemaAttributeDetails_defaultValue, null, false, 6);
		
		toolkit.paintBordersFor(parent);
		setText(PDEUIMessages.SchemaAttributeDetails_title);
	}

	protected abstract void createTypeDetails(Composite parent, FormToolkit toolkit);

	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaAttribute))
			return;
		fAttribute = (SchemaAttribute)object;
		setDecription(NLS.bind(PDEUIMessages.SchemaAttributeDetails_description, fAttribute.getName()));
		fName.setValue(fAttribute.getName(), true); //$NON-NLS-1$
		fDepTrue.setSelection(fAttribute.isDeprecated());
		fDepFalse.setSelection(!fAttribute.isDeprecated());
		
		boolean isStringType = fAttribute.getType().getName().equals(ISchemaAttribute.TYPES[ISchemaAttribute.STR_IND]);
		int kind = fAttribute.getKind();
		fType.select(isStringType ? 1 + kind : 0);
		
		fUse.select(fAttribute.getUse());
		Object value = fAttribute.getValue();
		fValue.setValue(value != null ? value.toString() : "", true); //$NON-NLS-1$
		
		boolean editable = isEditableElement();
		if (fAttribute.getUse() != 2) {
			fValue.getLabel().setEnabled(false);
			fValue.getText().setEditable(false);
		} else {
			fValue.setEditable(editable);
		}
		fName.setEditable(editable);
		fDepTrue.setEnabled(editable);
		fDepFalse.setEnabled(editable);
		fType.setEnabled(editable);
		fUse.setEnabled(editable);
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
					ISchemaElement element = (ISchemaElement)parent;
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
				if (type instanceof SchemaSimpleType
						&& kind != IMetaAttribute.STRING
						&& ((SchemaSimpleType) type).getRestriction() != null) {
					((SchemaSimpleType) type).setRestriction(null);
				}
				fireSelectionChange();
			}
		});
		fUse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				int use = fUse.getSelectionIndex();
				fAttribute.setUse(use);
				fValue.getLabel().setEnabled(use == 2);
				fValue.getText().setEditable(use == 2);
				if (use == 2 && fValue.getValue().length() == 0) {
					fValue.setValue(PDEUIMessages.SchemaAttributeDetails_defaultDefaultValue);
					fValue.getText().setSelection(0, fValue.getValue().length());
					fValue.getText().setFocus();
				} else if (use != 2)
					fValue.setValue(""); //$NON-NLS-1$
				
			}
		});
	}

	public void modelChanged(IModelChangedEvent event) {
		Object[] changedObjs = event.getChangedObjects();
		if(event.getChangeType() == IModelChangedEvent.INSERT && changedObjs.length > 0) {
			if(changedObjs[0] instanceof SchemaAttribute) {	
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
