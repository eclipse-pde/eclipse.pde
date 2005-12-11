package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.FormPage;

public class TargetOutlinePage extends FormOutlinePage {
	
	public class PluginNode {
		
		public ITargetPlugin[] fPlugins;
		
		public String toString() {
			return PDEUIMessages.TargetOutlinePage_plugins;
		}
		
		public PluginNode(ITargetPlugin[] plugins) {
			fPlugins = plugins;
		}
		
		public ITargetPlugin[] getModels() {
			return fPlugins;
		}
		
	}
	
	public class FeatureNode {
		
		public ITargetFeature[] fFeatures;
		
		public String toString() {
			return PDEUIMessages.TargetOutlinePage_features;
		}
		
		public FeatureNode(ITargetFeature[] features) {
			fFeatures = features;
		}
		
		public ITargetFeature[] getModels() {
			return fFeatures;
		}
	}
	
	public TargetOutlinePage(PDEFormEditor editor) {
		super(editor);
	}
	
	protected Object[] getChildren(Object parent) {
		if (parent instanceof OverviewPage) {
			OverviewPage page = (OverviewPage)parent;
			ITarget target = ((ITargetModel)page.getModel()).getTarget();
			if (target.useAllPlugins())
				return new Object[0];
			
			ITargetPlugin[] plugins = target.getPlugins();
			PluginNode pNode = (plugins.length > 0)  ?  new PluginNode(plugins) : null;

			ITargetFeature[] features = target.getFeatures();
			FeatureNode fNode = (features.length > 0)  ? new FeatureNode(features) : null;

			if (pNode != null && fNode != null) 
				return new Object[] {pNode, fNode};
			if (pNode != null)
				return new Object[] {pNode};
			if (fNode != null)
				return new Object[] {fNode};
		}
		if (parent instanceof PluginNode)
			return ((PluginNode)parent).getModels();
		if (parent instanceof FeatureNode)
			return ((FeatureNode)parent).getModels();
		return new Object[0];
	}
	
	protected ILabelProvider createLabelProvider() {
		return new BasicLabelProvider() {
			public Image getImage(Object element) {
				PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
				if (element instanceof PluginNode)
					return provider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
				if (element instanceof FeatureNode)
					return provider.get(PDEPluginImages.DESC_FEATURE_OBJ);
				if (element instanceof FormPage)
					return super.getImage(element);
				return provider.getImage(element);
		}};
	}

}
