/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class ExtensionsPage extends PDEFormPage {
	public static final String PAGE_ID = "extensions"; //$NON-NLS-1$

	private ExtensionsSection fSection;
	private ExtensionsBlock fBlock;

	public class ExtensionsBlock extends PDEMasterDetailsBlock implements IDetailsPageProvider {

		private ExtensionElementBodyTextDetails fBodyTextDetails;

		public ExtensionsBlock() {
			super(ExtensionsPage.this);
		}

		protected PDESection createMasterSection(IManagedForm managedForm, Composite parent) {
			fSection = new ExtensionsSection(getPage(), parent);
			return fSection;
		}

		protected void registerPages(DetailsPart detailsPart) {
			detailsPart.setPageLimit(10);
			// register static page for the extensions
			detailsPart.registerPage(IPluginExtension.class, new ExtensionDetails(fSection));
			// Register a static page for the extension elements that contain 
			// only body text (no child elements or attributes)
			// (e.g. schema simple type)
			fBodyTextDetails = new ExtensionElementBodyTextDetails(fSection);
			detailsPart.registerPage(ExtensionElementBodyTextDetails.class, fBodyTextDetails);
			// register a dynamic provider for elements
			detailsPart.setPageProvider(this);
		}

		public Object getPageKey(Object object) {
			if (object instanceof IPluginExtension)
				return IPluginExtension.class;
			if (object instanceof IPluginElement) {
				ISchemaElement element = ExtensionsSection.getSchemaElement((IPluginElement) object);
				// Extension point schema exists
				if (element != null) {
					// Use the body text page if the element has no child 
					// elements or attributes
					if (element.getType() instanceof ISchemaSimpleType) {
						// Set the schema element (to provide hover text 
						// content)
						fBodyTextDetails.setSchemaElement(element);
						return ExtensionElementBodyTextDetails.class;
					}
					return element;
				}
				// No Extension point schema
				// no element - construct one
				IPluginElement pelement = (IPluginElement) object;
				// Use the body text page if the element has no child 
				// elements or attributes
				if ((pelement.getAttributeCount() == 0) && (pelement.getChildCount() == 0)) {
					// Unset the previous schema element (no hover text 
					// content)					
					fBodyTextDetails.setSchemaElement(null);
					return ExtensionElementBodyTextDetails.class;
				}
				String ename = pelement.getName();
				IPluginExtension extension = ExtensionsSection.getExtension((IPluginParent) pelement.getParent());
				return extension.getPoint() + "/" + ename; //$NON-NLS-1$
			}
			return object.getClass();
		}

		public IDetailsPage getPage(Object object) {
			if (object instanceof ISchemaElement)
				return new ExtensionElementDetails(fSection, (ISchemaElement) object);
			if (object instanceof String)
				return new ExtensionElementDetails(fSection, null);
			return null;
		}
	}

	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public ExtensionsPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.ExtensionsPage_tabName);
		fBlock = new ExtensionsBlock();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#getHelpResource()
	 */
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_PLUGIN_EXTENSIONS;
	}

	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.ExtensionsPage_title);
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_EXTENSIONS_OBJ));
		fBlock.createContent(managedForm);
		//refire selection
		fSection.fireSelection();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_EXTENSIONS);
		super.createFormContent(managedForm);
	}

	public void updateFormSelection() {
		super.updateFormSelection();
		IFormPage page = getPDEEditor().findPage(PluginInputContext.CONTEXT_ID);
		if (page instanceof ManifestSourcePage) {
			ISourceViewer viewer = ((ManifestSourcePage) page).getViewer();
			if (viewer == null)
				return;
			StyledText text = viewer.getTextWidget();
			if (text == null)
				return;
			int offset = text.getCaretOffset();
			if (offset < 0)
				return;

			IDocumentRange range = ((ManifestSourcePage) page).getRangeElement(offset, true);
			if (range instanceof IDocumentAttributeNode)
				range = ((IDocumentAttributeNode) range).getEnclosingElement();
			else if (range instanceof IDocumentTextNode)
				range = ((IDocumentTextNode) range).getEnclosingElement();
			if ((range instanceof IPluginExtension) || (range instanceof IPluginElement)) {
				fSection.selectExtensionElement(new StructuredSelection(range));
			}
		}
	}
}
