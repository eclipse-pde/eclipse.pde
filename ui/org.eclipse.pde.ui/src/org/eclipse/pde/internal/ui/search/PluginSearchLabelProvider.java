package org.eclipse.pde.internal.ui.search;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.swt.graphics.Image;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchLabelProvider extends LabelProvider {
	
	public PluginSearchLabelProvider() {
		// Increment reference count for the global label provider
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public void dispose() {
		// Allow global label provider to release shared images, if needed.
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry)element;
			return PDEPlugin.getDefault().getLabelProvider().getImage((IPluginObject)entry.getGroupByKey());
		}
		return super.getImage(element);
	}

	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry) element;

			IPluginObject object = (IPluginObject)entry.getGroupByKey();
			
			if (object instanceof IPluginBase) {
				return ((IPluginBase)object).getId();
			}
			
			if (object instanceof IPluginImport) {
				return ((IPluginImport)object).getId() 
					+ " - "
					+ object.getModel().getPluginBase().getId();
			} 
			
			if (object instanceof IPluginExtension) {
				return ((IPluginExtension)object).getPoint()
					 + " - "
					+ object.getModel().getPluginBase().getId();
			}
			
			if (object instanceof IPluginExtensionPoint) {
				return object.getModel().getPluginBase().getId()
					+ "."
					+ ((IPluginExtensionPoint)object).getId();
			}
		}
		
		return super.getText(element);
	}
	

}
