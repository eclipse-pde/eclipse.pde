/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class SchemaOverviewPage extends PDEFormPage {

	public static final String PAGE_ID = "overview"; //$NON-NLS-1$

	private IColorManager fColorManager = ColorManager.getDefault();

	private ImageHyperlink fImageHyperlinkPreviewRefDoc;

	private DocSection fDocSection;

	private SchemaSpecSection fGeneralInfoSection;
	private SchemaIncludesSection fInclusionSection;

	private ShowDescriptionAction fPreviewAction;

	public SchemaOverviewPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.SchemaEditor_DocPage_title);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.SCHEMA_EDITOR_DOC;
	}

	public void setActive(boolean active) {
		if (!active)
			getManagedForm().commit(false);
		super.setActive(active);
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		ISchema schema = (ISchema) ((SchemaEditor) getEditor()).getAggregateModel();

		if (schema.isEditable()) {
			form.getToolBarManager().add(createUIControlConPreviewRefDoc());
			form.getToolBarManager().update(true);
		}

		super.createFormContent(managedForm);

		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		Composite top = managedForm.getToolkit().createComposite(form.getBody());
		top.setLayout(FormLayoutFactory.createFormPaneGridLayout(true, 2));
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fGeneralInfoSection = new SchemaSpecSection(this, top);
		fInclusionSection = new SchemaIncludesSection(this, top);
		fDocSection = new DocSection(this, form.getBody(), fColorManager);

		managedForm.addPart(fGeneralInfoSection);
		managedForm.addPart(fInclusionSection);
		managedForm.addPart(fDocSection);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.SCHEMA_EDITOR_DOC);
	}

	private ControlContribution createUIControlConPreviewRefDoc() {
		return new ControlContribution("Preview") { //$NON-NLS-1$
			protected Control createControl(Composite parent) {
				// Create UI
				createUIImageHyperlinkPreviewRefDoc(parent);
				// Create Listener
				createUIListenerImageHyperlinkPreviewRefDoc();
				return fImageHyperlinkPreviewRefDoc;
			}
		};
	}

	/**
	 * @param parent
	 */
	private void createUIImageHyperlinkPreviewRefDoc(Composite parent) {
		fImageHyperlinkPreviewRefDoc = new ImageHyperlink(parent, SWT.NONE);
		fImageHyperlinkPreviewRefDoc.setText(PDEUIMessages.SchemaEditor_previewLink);
		fImageHyperlinkPreviewRefDoc.setUnderlined(true);
		fImageHyperlinkPreviewRefDoc.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
	}

	/**
	 * 
	 */
	private void createUIListenerImageHyperlinkPreviewRefDoc() {
		fImageHyperlinkPreviewRefDoc.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {
				handleLinkActivatedPreviewRefDoc();
			}

			public void linkEntered(HyperlinkEvent e) {
				handleLinkEnteredPreviewRefDoc(e.getLabel());
			}

			public void linkExited(HyperlinkEvent e) {
				handleLinkExitedPreviewRefDoc();
			}
		});
	}

	/**
	 * @param message
	 */
	private void handleLinkEnteredPreviewRefDoc(String message) {
		// Update colour
		fImageHyperlinkPreviewRefDoc.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getActiveForeground());
		// Update IDE status line
		getEditor().getEditorSite().getActionBars().getStatusLineManager().setMessage(message);
	}

	/**
	 *
	 */
	private void handleLinkExitedPreviewRefDoc() {
		// Update colour
		fImageHyperlinkPreviewRefDoc.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
		// Update IDE status line
		getEditor().getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
	}

	/**
	 * 
	 */
	private void handleLinkActivatedPreviewRefDoc() {
		ISchema schema = (ISchema) ((SchemaEditor) getEditor()).getAggregateModel();
		if (fPreviewAction == null) {
			fPreviewAction = new ShowDescriptionAction(schema);
		} else {
			fPreviewAction.setSchema(schema);
		}

		fPreviewAction.run();
	}

	public void dispose() {
		fColorManager.dispose();
		super.dispose();
	}

}
