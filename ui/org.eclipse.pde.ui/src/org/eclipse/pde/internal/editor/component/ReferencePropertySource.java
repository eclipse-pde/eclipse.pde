package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.*;
import java.net.*;
import org.eclipse.ui.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.base.model.component.*;

public class ReferencePropertySource extends ComponentPropertySource {
	private Vector descriptors;
	private IPluginBase pluginBase;
	private Image errorImage;
	public final static String KEY_ID = "ComponentEditor.ReferenceProp.id";
	public final static String KEY_NAME = "ComponentEditor.ReferenceProp.name";
	public final static String KEY_VERSION = "ComponentEditor.ReferenceProp.version";
	public final static String KEY_ORIGINAL_VERSION = "ComponentEditor.ReferenceProp.originalVersion";
	
	private final static String P_NAME = "name";
	private final static String P_ID = "id";
	private final static String P_VERSION = "version";
	private final static String P_REF_VERSION = "ref_version";

	public class VersionProvider extends LabelProvider {
		public Image getImage(Object obj) {
			String originalVersion = getOriginalVersion();
			IComponentReference ref = getPluginReference();
			boolean inSync = ref.getVersion().equals(originalVersion);
			return inSync ? null : errorImage;
		}
	}

public ReferencePropertySource(IComponentReference reference, Image errorImage) {
	super(reference);
	this.errorImage = errorImage;
}
private String getOriginalVersion() {
	IPluginBase pluginBase = getPluginBase();
	return pluginBase.getVersion();
}
private IPluginBase getPluginBase() {
	if (pluginBase == null) {
		IComponentReference reference = getPluginReference();
		String id = reference.getId();
		WorkspaceModelManager manager =
			PDEPlugin.getDefault().getWorkspaceModelManager();
		IPluginModelBase[] models = null;
		if (reference instanceof IComponentFragment) {
			models = manager.getWorkspaceFragmentModels();
		} else {
			models = manager.getWorkspacePluginModels();
		}
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase modelBase = models[i];
			IPluginBase candidate = modelBase.getPluginBase();
			if (candidate.getId().equals(id)) {
				pluginBase = candidate;
				break;
			}
		}
	}
	return pluginBase;
}
public IComponentReference getPluginReference() {
	return (IComponentReference)object;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = new Vector();
		PropertyDescriptor desc = new PropertyDescriptor(P_ID, PDEPlugin.getResourceString(KEY_ID));
		descriptors.addElement(desc);
		desc = new PropertyDescriptor(P_NAME, PDEPlugin.getResourceString(KEY_NAME));
		descriptors.addElement(desc);
		desc = createTextPropertyDescriptor(P_VERSION, PDEPlugin.getResourceString(KEY_VERSION));
		//desc.setLabelProvider(new VersionProvider());
		descriptors.addElement(desc);
		desc = new PropertyDescriptor(P_REF_VERSION, PDEPlugin.getResourceString(KEY_ORIGINAL_VERSION));
		descriptors.addElement(desc);
	}
	return toDescriptorArray(descriptors);
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_ID)) {
		return getPluginReference().getId();
	}
	if (name.equals(P_NAME)) {
		return getPluginReference().getLabel();
	}
	if (name.equals(P_VERSION)) {
		return getPluginReference().getVersion();
	}
	if (name.equals(P_REF_VERSION)) {
		return getOriginalVersion();
	}
	return null;
}
public void setElement(IComponentPlugin plugin) {
	object = plugin;
}
public void setPropertyValue(Object name, Object value) {
	String svalue = value.toString();
	String realValue = svalue == null | svalue.length() == 0 ? null : svalue;
	try {
		if (name.equals(P_NAME)) {
			getPluginReference().setLabel(realValue);
		} else
			if (name.equals(P_VERSION)) {
				getPluginReference().setVersion(realValue);
			}
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
}
