/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaCompositorDetails extends AbstractSchemaDetails {

	private SchemaCompositor fCompositor;
	private ComboPart fKind;
	
	public SchemaCompositorDetails(ISchemaCompositor compositor, ElementSection section) {
		super(section, true);
		fCompositor = (SchemaCompositor)compositor;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		createMinOccurComp(parent, toolkit);
		createMaxOccurComp(parent, toolkit);
		
		toolkit.createLabel(parent, PDEUIMessages.SchemaCompositorDetails_type).setForeground(
				toolkit.getColors().getColor(FormColors.TITLE));
		fKind = new ComboPart();
		fKind.createControl(parent, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fKind.getControl().setLayoutData(gd);
		fKind.setItems(new String[] {
				ISchemaCompositor.kindTable[ISchemaCompositor.CHOICE],
				ISchemaCompositor.kindTable[ISchemaCompositor.SEQUENCE]});
		fKind.getControl().setEnabled(isEditable());
		
		
		setText(PDEUIMessages.SchemaCompositorDetails_title);
		setDecription(NLS.bind(PDEUIMessages.SchemaCompositorDetails_description, fCompositor.getName()));
	}

	public void updateFields() {

		updateMinOccur(fCompositor.getMinOccurs());
		updateMaxOccur(fCompositor.getMaxOccurs());
		
		fKind.select(fCompositor.getKind() - 1);
	}

	public void hookListeners() {
		hookMinOccur(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCompositor.setMinOccurs(getMinOccur());
			}
		});
		hookMaxOccur(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCompositor.setMaxOccurs(getMaxOccur());
			}
		});
		fKind.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fCompositor.setKind(fKind.getSelectionIndex() + 1);
				setDecription(NLS.bind(PDEUIMessages.SchemaCompositorDetails_description, fCompositor.getName()));
			}
		});
	}
}
