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
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.graphics.Image;


public class SchemaFormOutlinePage extends FormOutlinePage {
	private Object[] topics;
	public Object[] getChildren(Object parent) {
		ISchema schema = (ISchema) editor.getAggregateModel();
		if (schema.isValid()) {
			if (parent instanceof SchemaFormPage) {
				return getMarkup();
			}
			if (parent instanceof ISchemaElement) {
				return getAttributes((ISchemaElement) parent);
			}
			if (parent instanceof SchemaDocPage) {
				return getTopics();
			}
		}
		return super.getChildren(parent);
	}
	class OutlineLabelProvider extends BasicLabelProvider {
		public String getText(Object obj) {
			String label = getObjectLabel(obj);
			if (label != null)
				return label;
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			Image image = PDEPlugin.getDefault().getLabelProvider().getImage(
					obj);
			if (image != null)
				return image;
			return super.getImage(obj);
		}
	}
	public SchemaFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}
	protected Object[] createTopics() {
		ISchema schema = (ISchema) editor.getAggregateModel();
		IDocumentSection[] sections = schema.getDocumentSections();
		Object[] result = new Object[sections.length + 1];
		result[0] = schema;
		for (int i = 1; i <= sections.length; i++) {
			result[i] = sections[i - 1];
		}
		return result;
	}
	Object[] getAttributes(ISchemaElement element) {
		ISchemaType type = element.getType();
		if (type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttributes();
		}
		return new Object[0];
	}
	Object[] getMarkup() {
		ISchema schema = (ISchema) editor.getAggregateModel();
		return schema.getElements();
	}
	String getObjectLabel(Object obj) {
		if (obj instanceof ISchema) {
			return PDEUIMessages.SchemaEditor_topic_overview;
		}
		if (obj instanceof IDocumentSection) {
			IDocumentSection section = (IDocumentSection) obj;
			String sectionId = section.getSectionId();
			if (sectionId.equals(IDocumentSection.EXAMPLES))
				return PDEUIMessages.SchemaEditor_topic_examples;
			if (sectionId.equals(IDocumentSection.SINCE))
				return PDEUIMessages.SchemaEditor_topic_since;
			if (sectionId.equals(IDocumentSection.IMPLEMENTATION))
				return PDEUIMessages.SchemaEditor_topic_implementation;
			if (sectionId.equals(IDocumentSection.API_INFO))
				return PDEUIMessages.SchemaEditor_topic_api;
			if (sectionId.equals(IDocumentSection.COPYRIGHT))
				return PDEUIMessages.SchemaEditor_topic_copyright;
		}
		return null;
	}
	Object[] getTopics() {
		if (topics == null) {
			topics = createTopics();
		}
		return topics;
	}
	protected String getParentPageId(Object item) {
		if (item instanceof ISchemaElement || item instanceof ISchemaAttribute)
			return SchemaFormPage.PAGE_ID;
		if (item instanceof IDocumentSection || item instanceof ISchema)
			return SchemaDocPage.PAGE_ID;
		return super.getParentPageId(item);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			topics = null;
			treeViewer.refresh();
			return;
		}
		Object object = event.getChangedObjects()[0];
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			treeViewer.update(object, null);
		} else {
			// find the parent
			Object parent = null;
			if (object instanceof ISchemaObject) {
				parent = ((ISchemaObject) object).getParent();
			}
			if (parent != null) {
				if (parent instanceof ISchema) {
					//parent =
					// formPage.getEditor().getPage(SchemaEditor.DEFINITION_PAGE);
				}
				treeViewer.refresh(parent);
				treeViewer.expandToLevel(parent, 2);
			} else {
				treeViewer.refresh();
				treeViewer.expandAll();
			}
		}
	}
}
