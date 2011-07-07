/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
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
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.search.ShowDescriptionAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class SchemaFormPage extends PDEFormPage implements IModelChangedListener {
	public static final String PAGE_ID = "form"; //$NON-NLS-1$
	private ElementSection fSection;
	private SchemaBlock fBlock;
	private DetailsPart fDetailsPart;
	private ImageHyperlink fImageHyperlinkPreviewRefDoc;
	private ShowDescriptionAction fPreviewAction;

	public class SchemaBlock extends PDEMasterDetailsBlock implements IDetailsPageProvider {

		public SchemaBlock() {
			super(SchemaFormPage.this);
		}

		protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
			fSection = new ElementSection(getPage(), parent);
			return fSection;
		}

		protected void registerPages(DetailsPart detailsPart) {
			fDetailsPart = detailsPart;
			detailsPart.setPageLimit(5);
			detailsPart.registerPage(ISchemaObjectReference.class, new SchemaElementReferenceDetails(fSection));
			detailsPart.registerPage(ISchemaRootElement.class, new SchemaRootElementDetails(fSection));
			detailsPart.registerPage(ISchemaElement.class, new SchemaElementDetails(fSection));
			detailsPart.registerPage(ISchemaCompositor.class, new SchemaCompositorDetails(fSection));
			detailsPart.registerPage(SchemaStringAttributeDetails.class, new SchemaStringAttributeDetails(fSection));
			detailsPart.registerPage(SchemaJavaAttributeDetails.class, new SchemaJavaAttributeDetails(fSection));
			detailsPart.registerPage(SchemaOtherAttributeDetails.class, new SchemaOtherAttributeDetails(fSection));
			detailsPart.registerPage(SchemaIdentifierAttributeDetails.class, new SchemaIdentifierAttributeDetails(fSection));
			detailsPart.setPageProvider(this);
		}

		public Object getPageKey(Object object) {
			if (object instanceof ISchemaObjectReference)
				return ISchemaObjectReference.class;
			else if (object instanceof ISchemaRootElement)
				return ISchemaRootElement.class;
			else if (object instanceof ISchemaElement)
				return ISchemaElement.class;
			else if (object instanceof ISchemaCompositor)
				return ISchemaCompositor.class;
			else if (object instanceof ISchemaAttribute) {
				ISchemaAttribute att = (ISchemaAttribute) object;
				int kind = att.getKind();
				switch (kind) {
					case IMetaAttribute.JAVA :
						return SchemaJavaAttributeDetails.class;
					case IMetaAttribute.IDENTIFIER :
						return SchemaIdentifierAttributeDetails.class;
					case IMetaAttribute.STRING :
						if (att.getType().getName().equals(ISchemaAttribute.TYPES[ISchemaAttribute.STR_IND]))
							return SchemaStringAttributeDetails.class;
				}
				return SchemaOtherAttributeDetails.class;
			} else
				return null;
		}

		public IDetailsPage getPage(Object object) {
			return null;
		}
	}

	public SchemaFormPage(PDEFormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.SchemaEditor_FormPage_title);
		fBlock = new SchemaBlock();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.SCHEMA_EDITOR_MAIN;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		ISchema schema = (ISchema) ((SchemaEditor) getEditor()).getAggregateModel();

		if (schema.isEditable()) {
			form.getToolBarManager().add(createUIControlConPreviewRefDoc());
			form.getToolBarManager().update(true);
		}

		super.createFormContent(managedForm);
		fBlock.createContent(managedForm);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.SCHEMA_EDITOR_MAIN);
		initialize();
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

	private void createUIImageHyperlinkPreviewRefDoc(Composite parent) {
		fImageHyperlinkPreviewRefDoc = new ImageHyperlink(parent, SWT.NONE);
		fImageHyperlinkPreviewRefDoc.setText(PDEUIMessages.SchemaEditor_previewLink);
		fImageHyperlinkPreviewRefDoc.setUnderlined(true);
		fImageHyperlinkPreviewRefDoc.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
	}

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

	private void handleLinkEnteredPreviewRefDoc(String message) {
		// Update colour
		fImageHyperlinkPreviewRefDoc.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getActiveForeground());
		// Update IDE status line
		getEditor().getEditorSite().getActionBars().getStatusLineManager().setMessage(message);
	}

	private void handleLinkExitedPreviewRefDoc() {
		// Update colour
		fImageHyperlinkPreviewRefDoc.setForeground(getManagedForm().getToolkit().getHyperlinkGroup().getForeground());
		// Update IDE status line
		getEditor().getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
	}

	private void handleLinkActivatedPreviewRefDoc() {
		ISchema schema = (ISchema) ((SchemaEditor) getEditor()).getAggregateModel();
		if (fPreviewAction == null) {
			fPreviewAction = new ShowDescriptionAction(schema);
		} else {
			fPreviewAction.setSchema(schema);
		}

		fPreviewAction.run();
	}

	public void initialize() {
		ISchema schema = (ISchema) getModel();
		getManagedForm().getForm().setText(schema.getName());
		schema.addModelChangedListener(this);
	}

	public void dispose() {
		ISchema schema = (ISchema) getModel();
		if (schema != null)
			schema.removeModelChangedListener(this);
		super.dispose();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			String changeProperty = event.getChangedProperty();
			if (changeProperty != null && changeProperty.equals(ISchemaObject.P_NAME)) {
				Object[] change = event.getChangedObjects();
				if (change.length > 0 && change[0] instanceof ISchema)
					getManagedForm().getForm().setText(((ISchema) change[0]).getName());
			}
		} else if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(event);
		}
		// Update master section
		if (fSection != null) {
			fSection.handleModelChanged(event);
		}
		// Update details section
		IDetailsPage page = fDetailsPart.getCurrentPage();
		if (page instanceof IModelChangedListener) {
			((IModelChangedListener) page).modelChanged(event);
		}
	}

	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Note:  Cannot use event.  There are no changed objects within it
		// This method acts like a refresh
		ISchema schema = (ISchema) getModel();
		getManagedForm().getForm().setText(schema.getName());
	}
}
