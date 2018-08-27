/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
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

	@Override
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

	@Override
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

	@Override
	public void hookListeners() {
		hookMinOccur(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			fCompositor.setMinOccurs(getMinOccur());
		}));
		hookMaxOccur(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			fCompositor.setMaxOccurs(getMaxOccur());
		}));
		fKind.addSelectionListener(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			fCompositor.setKind(fKind.getSelectionIndex() + 1);
			setDecription(NLS.bind(PDEUIMessages.SchemaCompositorDetails_description, fCompositor.getName()));
		}));
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		// NO-OP
	}
}
