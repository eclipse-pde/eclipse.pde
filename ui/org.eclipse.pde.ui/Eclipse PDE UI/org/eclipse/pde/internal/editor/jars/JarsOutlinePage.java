package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.editor.*;
import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;

public class JarsOutlinePage extends FormOutlinePage {
	private Image libraryImage;

	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof JarsPage) {
				return getLibraries();
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
			Image image = getObjectImage(obj);
			if (image != null)
				return image;
			return super.getImage(obj);
		}
	}

public JarsOutlinePage(PDEFormPage formPage) {
	super(formPage);
	libraryImage = PDEPluginImages.DESC_JAVA_LIB_OBJ.createImage();
}
protected ITreeContentProvider createContentProvider() {
	return new ContentProvider();
}
public void createControl(Composite parent) {
	super.createControl(parent);
	IJarsModel model = (IJarsModel)formPage.getModel();
	model.addModelChangedListener(this);
}
protected ILabelProvider createLabelProvider() {
	return new OutlineLabelProvider();
}
public void dispose() {
	super.dispose();
	libraryImage.dispose();
	IJarsModel model = (IJarsModel)formPage.getModel();
	model.removeModelChangedListener(this);
}
IJarEntry [] getLibraries() {
	IJarsModel model = (IJarsModel)formPage.getModel();
	return model.getJars().getJarEntries();
}
Image getObjectImage(Object obj) {
	if (obj instanceof IJarEntry) {
		return libraryImage;
	}
	return null;
}
String getObjectLabel(Object obj) {
	if (obj instanceof IJarEntry) {
		return ((IJarEntry)obj).getName();
	}
	return null;
}
public IPDEEditorPage getParentPage(Object item) {
	if (item instanceof IJarEntry)
		return formPage.getEditor().getPage(PluginJarsEditor.JARS_PAGE);
	return super.getParentPage(item);
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		treeViewer.refresh();
		treeViewer.expandAll();
		return;
	}
	Object object = event.getChangedObjects()[0];
	if (event.getChangeType() == IModelChangedEvent.CHANGE) {
		treeViewer.update(object, null);
	} else {
		// find the parent
		Object parent = null;

		if (object instanceof IJarEntry)
			parent = getParentPage(object);
		if (parent != null) {
			if (event.getChangeType()==IModelChangedEvent.INSERT)
			   treeViewer.add(parent, object);
			else
			   treeViewer.remove(object);
		} else {
			treeViewer.refresh();
			treeViewer.expandAll();
		}
	}
}
}
