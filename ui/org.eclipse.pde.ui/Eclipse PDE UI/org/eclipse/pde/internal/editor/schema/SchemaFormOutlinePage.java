package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.editor.*;
import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;

public class SchemaFormOutlinePage extends FormOutlinePage {
	private Object[] topics;
	private Hashtable registry=new Hashtable();
	private Image globalElementImage;
	private Image topicImage;
	
	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof SchemaFormPage) {
				return getMarkup();
			}
			if (parent instanceof ISchemaElement) {
				return getAttributes((ISchemaElement)parent);
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
			if (label!=null) return label;
			return super.getText(obj);
		}
		public Image getImage(Object obj) {
			Image image = getObjectImage(obj);
			if (image!=null) return image;
			return super.getImage(obj);
		}
	}

public SchemaFormOutlinePage(PDEFormPage formPage) {
	super(formPage);
	globalElementImage = PDEPluginImages.DESC_GEL_SC_OBJ.createImage();
	topicImage = PDEPluginImages.DESC_DOC_SECTION_OBJ.createImage();
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
public void dispose() {
	super.dispose();
	globalElementImage.dispose();
	topicImage.dispose();

	for (Enumeration images = registry.elements(); images.hasMoreElements();) {
		((Image) images.nextElement()).dispose();
	}
}
Object[] getAttributes(ISchemaElement element) {
	ISchemaType type = element.getType();
	if (type instanceof ISchemaComplexType) {
		return ((ISchemaComplexType)type).getAttributes();
	}
	return new Object[0];
}
Image getBaseObjectImage(Object obj) {
	if (obj instanceof ISchemaAttribute) {
		ISchemaAttribute att = (ISchemaAttribute) obj;
		if (att.getKind() == ISchemaAttribute.JAVA)
			return PDEPluginImages.get(PDEPluginImages.IMG_ATT_CLASS_OBJ);
		if (att.getUse() == ISchemaAttribute.REQUIRED)
			return PDEPluginImages.get(PDEPluginImages.IMG_ATT_REQ_OBJ);
		return PDEPluginImages.get(PDEPluginImages.IMG_ATT_IMPL_OBJ);
	}
	if (obj instanceof ISchemaElement) {
		return globalElementImage;
	}
	if (obj instanceof IDocumentSection || obj instanceof ISchema) {
		return topicImage;
	}
	return null;
}
ImageDescriptor getBaseObjectImageDescriptor(Object obj) {
	if (obj instanceof ISchemaAttribute) {
		ISchemaAttribute att = (ISchemaAttribute) obj;
		if (att.getKind() == ISchemaAttribute.JAVA)
			return PDEPluginImages.DESC_ATT_CLASS_OBJ;
		if (att.getUse() == ISchemaAttribute.REQUIRED)
			return PDEPluginImages.DESC_ATT_REQ_OBJ;
		return PDEPluginImages.DESC_ATT_IMPL_OBJ;
	}
	if (obj instanceof ISchemaElement) {
		return PDEPluginImages.DESC_GEL_SC_OBJ;
	}
	if (obj instanceof IDocumentSection || obj instanceof ISchema) {
		return PDEPluginImages.DESC_DOC_SECTION_OBJ;
	}
	return null;
}
Object[] getMarkup() {
	ISchema schema = (ISchema)formPage.getModel();
	return schema.getElements();
}
Image getObjectImage(Object obj) {
	if (obj instanceof ISchemaObject) {
		ISchemaObject sobj = (ISchemaObject) obj;
		String text = sobj.getDescription();
		if (text != null)
			text = text.trim();
		if (text != null && text.length() > 0) {
			// complete
			String key = getOverlayKey(obj);
			Image image = (Image)registry.get(key);
			if (image == null) {
				ImageDescriptor desc =
					new OverlayIcon(
						getBaseObjectImageDescriptor(obj),
						new ImageDescriptor[][] { { PDEPluginImages.DESC_DOC_CO }});
				registry.put(key, desc.createImage());
				image = (Image)registry.get(key);
			}
			return image;
		} else {
			Image image = getBaseObjectImage(obj);
			if (image != null)
				return image;
		}
	}
	return null;
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
String getOverlayKey(Object obj) {
	String key = obj.getClass().getName();
	if (obj instanceof ISchemaAttribute) {
		ISchemaAttribute att = (ISchemaAttribute)obj;
		key += ":"+att.getUse()+","+att.getKind();
	}
	return key;
}
public IPDEEditorPage getParentPage(Object item) {
	if (item instanceof IDocumentSection || item instanceof ISchema)
		return formPage.getEditor().getPage(SchemaEditor.DOC_PAGE);
	if (item instanceof ISchemaObject)
		return formPage.getEditor().getPage(SchemaEditor.DEFINITION_PAGE);
	return super.getParentPage(item);
}
Object[] getTopics() {
	if (topics==null) {
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
