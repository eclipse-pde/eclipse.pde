package org.eclipse.pde.internal.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.model.build.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.model.*;

import java.util.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;

public class BuildOutlinePage extends FormOutlinePage {
	private Image variableImage;

	class ContentProvider extends BasicContentProvider {
		public Object[] getChildren(Object parent) {
			if (parent instanceof BuildPage) {
				return getVariables();
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

public BuildOutlinePage(PDEFormPage formPage) {
	super(formPage);
	variableImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_BUILD_VAR_OBJ);
}
protected ITreeContentProvider createContentProvider() {
	return new ContentProvider();
}
public void createControl(Composite parent) {
	super.createControl(parent);
	IBuildModel model = (IBuildModel)formPage.getModel();
	model.addModelChangedListener(this);
}
protected ILabelProvider createLabelProvider() {
	return new OutlineLabelProvider();
}
public void dispose() {
	super.dispose();
	IBuildModel model = (IBuildModel)formPage.getModel();
	model.removeModelChangedListener(this);
}
Image getObjectImage(Object obj) {
	if (obj instanceof IBuildEntry) {
		return variableImage;
	}
	return null;
}
String getObjectLabel(Object obj) {
	if (obj instanceof IBuildEntry) {
		return ((IBuildEntry)obj).getName();
	}
	return null;
}
public IPDEEditorPage getParentPage(Object item) {
	if (item instanceof IBuildEntry)
		return formPage.getEditor().getPage(BuildPropertiesEditor.BUILD_PAGE);
	return super.getParentPage(item);
}
IBuildEntry [] getVariables() {
	IBuildModel model = (IBuildModel)formPage.getModel();
	return model.getBuild().getBuildEntries();
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

		if (object instanceof String) return;

		if (object instanceof IBuildEntry)
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
