package org.eclipse.pde.internal.ui.search;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.swt.graphics.Image;


public class DependencyExtentLabelProvider extends JavaElementLabelProvider {

	public Image getImage(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry)element;
			element = entry.getGroupByKey();
		}
		if (element instanceof IPluginExtensionPoint)
			return PDEPlugin.getDefault().getLabelProvider().getImage(
				(IPluginExtensionPoint) element);
					
		return super.getImage(element);
	}

	public String getText(Object element) {
		if (element instanceof ISearchResultViewEntry) {
			ISearchResultViewEntry entry = (ISearchResultViewEntry) element;
			element = entry.getGroupByKey();
		}
		if (element instanceof IPluginExtensionPoint) {
			return ((IPluginExtensionPoint) element)
				.getModel()
				.getPluginBase()
				.getId()
				+ "."
				+ ((IPluginExtensionPoint) element).getId();
		} else if (element instanceof IType) {
			return super.getText(element) + " - " + ((IType)element).getPackageFragment().getElementName();
		}
		return super.getText(element);
	}

}
