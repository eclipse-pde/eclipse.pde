package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.swt.graphics.Image;

public class SchemaFormOutlinePage extends FormOutlinePage {
	private Object[] topics;

	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof SchemaFormPage) {
				return getMarkup();
			}
			if (parent instanceof ISchemaElement) {
				return getAttributes((ISchemaElement) parent);
			}
			if (parent instanceof SchemaDocPage) {
				return getTopics();
			}
			return super.getChildren(parent);
		}
		public Object getParent(Object child) {
			return super.getParent(child);
		}
	}

	class OutlineLabelProvider extends BasicLabelProvider {
		public String getText(Object obj) {
			String label = getObjectLabel(obj);
			if (label != null)
				return label;
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			Image image = PDEPlugin.getDefault().getLabelProvider().getImage(obj);
			if (image != null)
				return image;
			return super.getImage(obj);
		}
	}

	public SchemaFormOutlinePage(PDEFormPage formPage) {
		super(formPage);
	}
	protected ITreeContentProvider createContentProvider() {
		return new ContentProvider();
	}
	protected ILabelProvider createLabelProvider() {
		return new OutlineLabelProvider();
	}
	protected Object[] createTopics() {
		ISchema schema = (ISchema) formPage.getModel();
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
		ISchema schema = (ISchema) formPage.getModel();
		return schema.getElements();
	}

	String getObjectLabel(Object obj) {
		if (obj instanceof ISchema) {
			return PDEPlugin.getResourceString(DocSection.KEY_TOPIC_OVERVIEW);
		}
		if (obj instanceof IDocumentSection) {
			IDocumentSection section = (IDocumentSection) obj;
			String sectionId = section.getSectionId();
			if (sectionId.equals(IDocumentSection.EXAMPLES))
				return PDEPlugin.getResourceString(DocSection.KEY_TOPIC_EXAMPLES);
			if (sectionId.equals(IDocumentSection.IMPLEMENTATION))
				return PDEPlugin.getResourceString(DocSection.KEY_TOPIC_IMPLEMENTATION);
			if (sectionId.equals(IDocumentSection.API_INFO))
				return PDEPlugin.getResourceString(DocSection.KEY_TOPIC_API);
			if (sectionId.equals(IDocumentSection.COPYRIGHT))
				return PDEPlugin.getResourceString(DocSection.KEY_TOPIC_COPYRIGHT);
		}
		return null;
	}

	public IPDEEditorPage getParentPage(Object item) {
		if (item instanceof IDocumentSection || item instanceof ISchema)
			return formPage.getEditor().getPage(SchemaEditor.DOC_PAGE);
		if (item instanceof ISchemaObject)
			return formPage.getEditor().getPage(SchemaEditor.DEFINITION_PAGE);
		return super.getParentPage(item);
	}
	Object[] getTopics() {
		if (topics == null) {
			topics = createTopics();
		}
		return topics;
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
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
					parent = formPage.getEditor().getPage(SchemaEditor.DEFINITION_PAGE);
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