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

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class SchemaElementReferenceDetails extends AbstractSchemaDetails {

	private SchemaElementReference fElement;
	private Hyperlink fReferenceLink;
	private Label fRefLabel;

	public SchemaElementReferenceDetails(ElementSection section) {
		super(section, true, false);
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		createMinOccurComp(parent, toolkit);
		createMaxOccurComp(parent, toolkit);

		fRefLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaElementReferenceDetails_reference);
		fRefLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		fReferenceLink = toolkit.createHyperlink(parent, new String(), SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fReferenceLink.setLayoutData(gd);

		setText(PDEUIMessages.SchemaElementReferenceDetails_title);
	}

	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaElementReference))
			return;
		fElement = (SchemaElementReference) object;

		setDecription(NLS.bind(PDEUIMessages.SchemaElementReferenceDetails_description, fElement.getName()));
		fReferenceLink.setText(fElement.getName());
		updateMinOccur(fElement.getMinOccurs());
		updateMaxOccur(fElement.getMaxOccurs());
		boolean editable = isEditableElement();
		fRefLabel.setEnabled(editable);
		fReferenceLink.setEnabled(editable);
		enableMinMax(editable);
	}

	public void hookListeners() {
		hookMinOccur(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				fElement.setMinOccurs(getMinOccur());
			}
		});
		hookMaxOccur(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (blockListeners())
					return;
				fElement.setMaxOccurs(getMaxOccur());
			}
		});
		fReferenceLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (blockListeners())
					return;
				fireMasterSelection(new StructuredSelection(fElement.getReferencedObject()));
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
