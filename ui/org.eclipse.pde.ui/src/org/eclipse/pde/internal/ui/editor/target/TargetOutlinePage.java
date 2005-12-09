package org.eclipse.pde.internal.ui.editor.target;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;

public class TargetOutlinePage extends FormOutlinePage {
	
	public class PluginNode {
		
		public ITargetPlugin[] fPlugins;
		public boolean fUseAllPlugins = false;
		
		public String toString() {
			if (fUseAllPlugins)
				return PDEUIMessages.TargetOutlinePage_useAllPlugins;
			return PDEUIMessages.TargetOutlinePage_plugins;
		}
		
		public PluginNode(ITargetPlugin[] plugins) {
			fPlugins = plugins;
		}
		
		public PluginNode() {
			fUseAllPlugins = true;
		}
		
		public ITargetPlugin[] getModels() {
			if (fUseAllPlugins)
				return new ITargetPlugin[0];
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
	
	public void modelChanged(IModelChangedEvent event) {
		super.modelChanged(event);
	}
	
	protected Object[] getChildren(Object parent) {
		if (parent instanceof OverviewPage) {
			OverviewPage page = (OverviewPage)parent;
			ITarget target = ((ITargetModel)page.getModel()).getTarget();
			if (target.useAllPlugins())
				return new Object[] { new PluginNode() };
			
			PluginNode pNode = null;
			ITargetPlugin[] plugins = target.getPlugins();
			if (plugins.length > 0) {
				pNode = new PluginNode(plugins);
			}
			
			FeatureNode fNode = null;
			ITargetFeature[] features = target.getFeatures();
			if (features.length > 0) {
				fNode = new FeatureNode(features);
			}
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

}
