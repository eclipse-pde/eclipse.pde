package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
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

public class FeatureOutlinePage extends FormOutlinePage {
	private Image urlImage;
	private Image pluginImage;
	private Image fragmentImage;

	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof FeatureFormPage) {
				return getURLs();
			}
			if (parent instanceof FeatureReferencePage) {
				return getReferences();
			}
			return super.getChildren(parent);
		}
		public Object getParent(Object child) {
			Object parent = getParentPage(child);
			if (parent!=null) return parent;
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

public FeatureOutlinePage(PDEFormPage formPage) {
	super(formPage);
	urlImage = PDEPluginImages.DESC_LINK_OBJ.createImage();
	pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
	fragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();

}
protected ITreeContentProvider createContentProvider() {
	return new ContentProvider();
}
public void createControl(Composite parent) {
	super.createControl(parent);
	IFeatureModel model = (IFeatureModel)formPage.getModel();
	model.addModelChangedListener(this);
}
protected ILabelProvider createLabelProvider() {
	return new OutlineLabelProvider();
}
public void dispose() {
	super.dispose();
	urlImage.dispose();
	fragmentImage.dispose();
	pluginImage.dispose();
	IFeatureModel model = (IFeatureModel)formPage.getModel();
	model.removeModelChangedListener(this);
}
Image getObjectImage(Object obj) {
	if (obj instanceof IFeatureURLElement) {
		return urlImage;
	}
	if (obj instanceof IFeaturePlugin) {
		IFeaturePlugin fref = (IFeaturePlugin)obj;
		if (fref.isFragment())
			return fragmentImage;
		else
			return pluginImage;
	}
	return null;
}
String getObjectLabel(Object obj) {
	if (obj instanceof IFeatureURLElement) {
		return ((IFeatureURLElement)obj).getLabel();
	}
	if (obj instanceof IFeaturePlugin) {
		return ((IFeaturePlugin)obj).getLabel();
	}
	return null;
}
public IPDEEditorPage getParentPage(Object item) {
	if (item instanceof IFeatureURLElement)
		return formPage.getEditor().getPage(FeatureEditor.FEATURE_PAGE);
	if (item instanceof IFeaturePlugin)
		return formPage.getEditor().getPage(FeatureEditor.REFERENCE_PAGE);
	return super.getParentPage(item);
}
private Object[] getReferences() {
	IFeatureModel model = (IFeatureModel)formPage.getModel();
	IFeature feature = model.getFeature();
	return feature.getPlugins();
}
private Object[] getURLs() {
	IFeatureModel model = (IFeatureModel)formPage.getModel();
	IFeature component = model.getFeature();
	IFeatureURL url = component.getURL();
	if (url==null) return new Object[0];
	IFeatureURLElement [] updates = url.getUpdates();
	IFeatureURLElement [] discoveries = url.getDiscoveries();
	int size = updates.length + discoveries.length;
	Object [] result = new Object[size];
	System.arraycopy(updates, 0, result, 0, updates.length);
	System.arraycopy(discoveries, 0, result, updates.length, discoveries.length);
	return result;
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		treeViewer.refresh();
		return;
	}
	Object object = event.getChangedObjects()[0];
	if (object instanceof IFeaturePlugin
		|| object instanceof IFeatureURLElement) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			treeViewer.update(object, null);
		} else {
			// find the parent
			Object parent = null;

			parent = getParentPage(object);
			if (parent != null) {
				if (event.getChangeType() == IModelChangedEvent.INSERT)
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
}
