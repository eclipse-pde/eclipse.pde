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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaObjectReference;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

public class SchemaElementReferenceDetails extends AbstractSchemaDetails {

	private SchemaElementReference fElement;
	private Hyperlink fReferenceLink;
	private Label fRefLabel;
	
	public SchemaElementReferenceDetails(ISchemaObjectReference compositor, ElementSection section) {
		super(section, true);
		fElement = (SchemaElementReference)compositor;
	}

	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();
		
		createMinOccurComp(parent, toolkit);
		createMaxOccurComp(parent, toolkit);
		
		fRefLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaElementReferenceDetails_reference);
		fRefLabel.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		fReferenceLink = toolkit.createHyperlink(parent, fElement.getName(), SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fReferenceLink.setLayoutData(gd);
		
		setText(PDEUIMessages.SchemaElementReferenceDetails_title);
		setDecription(NLS.bind(PDEUIMessages.SchemaElementReferenceDetails_description, fElement.getName()));
	}

	public void updateFields() {
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
				fElement.setMinOccurs(getMinOccur());
			}
		});
		hookMaxOccur(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fElement.setMaxOccurs(getMaxOccur());
			}
		});
		fReferenceLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				fireMasterSelection(new StructuredSelection(fElement.getReferencedObject()));
			}
		});
	}

	public boolean isEditableElement() {
		return fElement.getCompositor().getSchema().isEditable();
	}
}
