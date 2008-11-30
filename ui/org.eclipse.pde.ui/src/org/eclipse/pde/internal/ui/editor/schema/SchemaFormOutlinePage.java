/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class SchemaFormOutlinePage extends FormOutlinePage {
	private Object[] fTopics;

	public Object[] getChildren(Object parent) {
		ISchema schema = (ISchema) fEditor.getAggregateModel();
		if (schema != null && schema.isValid()) {
			if (parent instanceof SchemaFormPage) {
				return getMarkup();
			}
			if (parent instanceof ISchemaElement) {
				return getAttributes((ISchemaElement) parent);
			}
			if (parent instanceof SchemaOverviewPage) {
				return getTopics();
			}
		}
		return super.getChildren(parent);
	}

	class SchemaLabelProvider extends BasicLabelProvider {
		public SchemaLabelProvider(ILabelProvider ilp) {
			super(ilp);
		}

		public String getText(Object obj) {
			String label = getObjectLabel(obj);
			return (label == null) ? super.getText(obj) : label;
		}
	}

	public SchemaFormOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	public ILabelProvider createLabelProvider() {
		return new SchemaLabelProvider(PDEPlugin.getDefault().getLabelProvider());
	}

	protected Object[] createTopics() {
		ISchema schema = (ISchema) fEditor.getAggregateModel();
		IDocumentSection[] sections = schema.getDocumentSections();
		Object[] result = new Object[sections.length + 1];
		result[0] = schema;
		for (int i = 1; i <= sections.length; i++) {
			result[i] = sections[i - 1];
		}
		return result;
	}

	private Object[] getAttributes(ISchemaElement element) {
		ISchemaType type = element.getType();
		if (type instanceof ISchemaComplexType) {
			return ((ISchemaComplexType) type).getAttributes();
		}
		return new Object[0];
	}

	private Object[] getMarkup() {
		ISchema schema = (ISchema) fEditor.getAggregateModel();
		return schema.getElements();
	}

	protected String getObjectLabel(Object obj) {
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
			if (sectionId.equalsIgnoreCase(IDocumentSection.API_INFO))
				return PDEUIMessages.SchemaEditor_topic_api;
			if (sectionId.equals(IDocumentSection.COPYRIGHT))
				return PDEUIMessages.SchemaEditor_topic_copyright;
		}
		return null;
	}

	Object[] getTopics() {
		if (fTopics == null) {
			fTopics = createTopics();
		}
		return fTopics;
	}

	protected String getParentPageId(Object item) {
		if (item instanceof ISchemaElement || item instanceof ISchemaAttribute)
			return SchemaFormPage.PAGE_ID;
		if (item instanceof IDocumentSection || item instanceof ISchema)
			return SchemaOverviewPage.PAGE_ID;
		return super.getParentPageId(item);
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			fTopics = null;
			fTreeViewer.refresh();
			return;
		}
		Object object = event.getChangedObjects()[0];
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			fTreeViewer.update(object, null);
		} else {
			// find the parent
			Object parent = null;
			if (object instanceof ISchemaObject) {
				parent = ((ISchemaObject) object).getParent();
			}
			if (parent != null) {
				fTreeViewer.refresh(parent);
				fTreeViewer.expandToLevel(parent, 2);
			} else {
				fTreeViewer.refresh();
				fTreeViewer.expandAll();
			}
		}
	}
}
