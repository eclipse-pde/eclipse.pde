/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEChildFormPage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.update.ui.forms.internal.AbstractSectionForm;
import org.eclipse.update.ui.forms.internal.IFormPage;

public class SchemaDocPage extends PDEChildFormPage {
	public static final String PAGE_TITLE = "SchemaEditor.DocPage.title";

	public SchemaDocPage(SchemaFormPage mainPage) {
		super(mainPage, PDEPlugin.getResourceString(PAGE_TITLE));
	}
	
	/**
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#becomesInvisible(IFormPage)
	 */
	public boolean becomesInvisible(IFormPage newPage) {
		getForm().commitChanges(false);
		return true;
	}

	protected AbstractSectionForm createForm() {
		return new DocForm(this);
	}
	
	public IContentOutlinePage createContentOutlinePage() {
		return null;
	}

	/*private boolean isElementComplete(ISchemaElement element) {
		if (isObjectComplete(element))
			return false;
		ISchemaType type = element.getType();
		if (!(type instanceof ISchemaComplexType))
			return true;
		ISchemaComplexType complexType = (ISchemaComplexType) type;

		ISchemaAttribute[] attributes = complexType.getAttributes();
		for (int i = 0; i < attributes.length; i++) {
			ISchemaAttribute att = attributes[i];
			if (isObjectComplete(att) == false)
				return false;
		}
		return true;
	}*/
	/*private boolean isMarkupComplete() {
		ISchema schema = (ISchema) getModel();
		ISchemaElement[] elements = schema.getElements();
		for (int i = 0; i < elements.length; i++) {
			ISchemaElement element = elements[i];
			if (isElementComplete(element) == false)
				return false;
		}
		return true;
	}*/
	/*private boolean isObjectComplete(ISchemaObject object) {
		String text = object.getDescription();
		if (text != null)
			text.trim();
		return (text != null && text.length() > 0);
	}*/
	public void update() {
		super.update();
	}
	public void updateEditorInput(Object object) {
		((DocForm) getForm()).updateEditorInput(object);
	}
}
