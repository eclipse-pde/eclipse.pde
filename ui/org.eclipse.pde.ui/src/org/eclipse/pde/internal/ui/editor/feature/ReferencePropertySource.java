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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.IWorkspaceModelManager;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class ReferencePropertySource extends FeatureEntryPropertySource {
	private IPluginBase pluginBase;
	private Image errorImage;
	public final static String KEY_NAME = "FeatureEditor.ReferenceProp.name";
	public final static String KEY_VERSION =
		"FeatureEditor.ReferenceProp.version";
	public final static String KEY_ORIGINAL_VERSION =
		"FeatureEditor.ReferenceProp.originalVersion";

	private final static String P_NAME = "name";
	private final static String P_VERSION = "version";
	private final static String P_REF_VERSION = "ref_version";

	public class VersionProvider extends LabelProvider {
		public Image getImage(Object obj) {
			String originalVersion = getOriginalVersion();
			IFeaturePlugin ref = getPluginReference();
			boolean inSync = ref.getVersion().equals(originalVersion);
			return inSync ? null : errorImage;
		}
	}

	public ReferencePropertySource(
		IFeaturePlugin reference,
		Image errorImage) {
		super(reference);
		this.errorImage = errorImage;
	}
	private String getOriginalVersion() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase == null)
			return "";
		return pluginBase.getVersion();
	}
	private IPluginBase getPluginBase() {
		if (pluginBase == null) {
			IFeaturePlugin reference = getPluginReference();
			if (reference.getModel().getUnderlyingResource() == null)
				return null;
			String id = reference.getId();
			IWorkspaceModelManager manager =
				PDECore.getDefault().getWorkspaceModelManager();
			IPluginModelBase[] models = null;
			if (reference.isFragment()) {
				models = manager.getFragmentModels();
			} else {
				models = manager.getPluginModels();
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
	public IFeaturePlugin getPluginReference() {
		return (IFeaturePlugin) object;
	}
	protected void createPropertyDescriptors() {
		super.createPropertyDescriptors();
		PropertyDescriptor desc;
		desc =
			new PropertyDescriptor(
				P_NAME,
				PDEPlugin.getResourceString(KEY_NAME));
		descriptors.addElement(desc);
		desc =
			createTextPropertyDescriptor(
				P_VERSION,
				PDEPlugin.getResourceString(KEY_VERSION));
		//desc.setLabelProvider(new VersionProvider());
		descriptors.addElement(desc);
		desc =
			new PropertyDescriptor(
				P_REF_VERSION,
				PDEPlugin.getResourceString(KEY_ORIGINAL_VERSION));

	}

	public Object getPropertyValue(Object name) {
		if (name.equals(P_NAME)) {
			return getPluginReference().getLabel();
		}
		if (name.equals(P_VERSION)) {
			return getPluginReference().getVersion();
		}
		if (name.equals(P_REF_VERSION)) {
			return getOriginalVersion();
		}
		return super.getPropertyValue(name);
	}
	public void setElement(IFeaturePlugin plugin) {
		object = plugin;
	}
	public void setPropertyValue(Object name, Object value) {
		String svalue = value.toString();
		String realValue =
			svalue == null | svalue.length() == 0 ? null : svalue;
		try {
			if (name.equals(P_NAME)) {
				getPluginReference().setLabel(realValue);
			} else if (name.equals(P_VERSION)) {
				getPluginReference().setVersion(realValue);
			} else super.setPropertyValue(name, value);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
