package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;


public class ShowDescriptionAction extends Action {
	private IPluginExtensionPoint point;
	public ShowDescriptionAction(IPluginExtensionPoint point) {
		this.point = point;
		setText("Show Description");
	}

}
