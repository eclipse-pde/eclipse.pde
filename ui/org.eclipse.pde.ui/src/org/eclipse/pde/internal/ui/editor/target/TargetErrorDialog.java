/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.TreeMessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

public class TargetErrorDialog extends TreeMessageDialog {
	
	private static LabelProvider fLabelProvider;
	
	private static class TreeNode {
		Object[] fChildren;
		boolean fFeature;
		protected TreeNode (Object[] children, boolean feature) {
			fChildren = children;	
			fFeature = feature;
		}
		protected Object[] getChildren() 	{	return fChildren;	}
		protected boolean isFeatureBased() 	{	return fFeature;	}
		public String toString() {
			return fFeature ? PDEUIMessages.TargetPluginsTab_features : PDEUIMessages.TargetPluginsTab_plugins;
		}
	}
	
	protected static class ErrorDialogContentProvider extends DefaultContentProvider implements ITreeContentProvider{
		TreeNode fPlugins, fFeatures;
		ErrorDialogContentProvider(TreeNode features, TreeNode plugins) {
			fFeatures = features;	
			fPlugins = plugins;
		}
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof TreeNode)
				return ((TreeNode)parentElement).getChildren();
			return null;
		}
		public Object getParent(Object element) {
			return null;
		}
		public boolean hasChildren(Object element) {
			return element instanceof TreeNode;
		}
		public Object[] getElements(Object inputElement) {
			if (fFeatures != null && fPlugins != null) 		return new Object[] {fFeatures, fPlugins};
			else if (fFeatures != null)						return new Object[] {fFeatures};
			else 											return new Object[] {fPlugins};
		}
	}

	private TargetErrorDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
				dialogImageType, dialogButtonLabels, defaultIndex);
	}
	
	public static void showDialog(Shell parentShell, Object[] features, Object[] plugins) {
		TreeMessageDialog dialog = new TargetErrorDialog(parentShell, PDEUIMessages.TargetErrorDialog_title, null, PDEUIMessages.TargetErrorDialog_description, 
				MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL }, 0);
		TreeNode featureNode = (features.length > 0) ? new TreeNode(features, true) : null;
		TreeNode pluginNode = (plugins.length > 0) ? new TreeNode(plugins, false) : null;
		dialog.setContentProvider(new ErrorDialogContentProvider(featureNode, pluginNode));
		dialog.setLabelProvider(getLabelProvider());
		dialog.setInput(new Object());
		dialog.open();
	}
	
	protected static LabelProvider getLabelProvider() {
		if (fLabelProvider == null)  {
			fLabelProvider = new LabelProvider() {
				
				public Image getImage(Object obj) {
					if (obj instanceof TreeNode) {
						if (((TreeNode)obj).isFeatureBased())
							return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ, 0);
						return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ, 0);
					} if (obj instanceof IFeaturePlugin) {
						IFeaturePlugin plugin = (IFeaturePlugin) obj;
						if (plugin.isFragment())
							return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FRAGMENT_OBJ, 0);
						return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ, 0);
					}
					return PDEPlugin.getDefault().getLabelProvider().getImage(obj);
				}

				public String getText(Object obj) {
					return PDEPlugin.getDefault().getLabelProvider().getText(obj);
				}
			};
		}
		return fLabelProvider;
	}

}
