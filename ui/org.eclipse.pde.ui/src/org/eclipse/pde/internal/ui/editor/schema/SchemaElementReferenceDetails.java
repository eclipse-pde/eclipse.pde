/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
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

	@Override
	public void createDetails(Composite parent) {
		FormToolkit toolkit = getManagedForm().getToolkit();

		createMinOccurComp(parent, toolkit);
		createMaxOccurComp(parent, toolkit);

		fRefLabel = toolkit.createLabel(parent, PDEUIMessages.SchemaElementReferenceDetails_reference);
		fRefLabel.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		fReferenceLink = toolkit.createHyperlink(parent, "", SWT.NONE); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fReferenceLink.setLayoutData(gd);

		setText(PDEUIMessages.SchemaElementReferenceDetails_title);
	}

	@Override
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

	@Override
	public void hookListeners() {
		hookMinOccur(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			fElement.setMinOccurs(getMinOccur());
		}));
		hookMaxOccur(widgetSelectedAdapter(e -> {
			if (blockListeners())
				return;
			fElement.setMaxOccurs(getMaxOccur());
		}));
		fReferenceLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (blockListeners())
					return;
				fireMasterSelection(new StructuredSelection(fElement.getReferencedObject()));
			}
		});
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		// NO-OP
	}
}
