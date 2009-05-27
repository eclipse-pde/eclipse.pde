/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaCompositorDetails extends AbstractSchemaDetails {

	private SchemaCompositor fCompositor;
	private ComboPart fKind;
	private Label fKindLabel;

	public SchemaCompositorDetails(ElementSection section) {
		super(section, true, false);
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		createMinOccurComp(parent, toolkit);
		createMaxOccurComp(parent, toolkit);

		fKindLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaCompositorDetails_type);
		fKindLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		fKind = new ComboPart();
		fKind.createControl(parent, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fKind.getControl().setLayoutData(gd);
		fKind.setItems(new String[] {ISchemaCompositor.kindTable[ISchemaCompositor.CHOICE], ISchemaCompositor.kindTable[ISchemaCompositor.SEQUENCE]});
		fKind.getControl().setEnabled(isEditable());

		setText(PDEUIMessages.SchemaCompositorDetails_title);
	}

	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaCompositor))
			return;
		fCompositor = (SchemaCompositor) object;
		setDecription(NLS.bind(PDEUIMessages.SchemaCompositorDetails_description, fCompositor.getName()));
		updateMinOccur(fCompositor.getMinOccurs());
		updateMaxOccur(fCompositor.getMaxOccurs());
		fKind.select(fCompositor.getKind() - 1);

		boolean editable = isEditableElement();
		fKindLabel.setEnabled(editable);
		fKind.setEnabled(editable);
		enableMinMax(editable);
	}

	public void hookListeners() {
		hookMinOccur(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				fCompositor.setMinOccurs(getMinOccur());
			}
		});
		hookMaxOccur(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				fCompositor.setMaxOccurs(getMaxOccur());
			}
		});
		fKind.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				fCompositor.setKind(fKind.getSelectionIndex() + 1);
				setDecription(NLS.bind(PDEUIMessages.SchemaCompositorDetails_description, fCompositor.getName()));
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		// NO-OP
	}
}
