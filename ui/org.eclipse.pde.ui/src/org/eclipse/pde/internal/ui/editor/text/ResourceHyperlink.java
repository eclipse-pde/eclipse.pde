package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.PartInitException;

public class ResourceHyperlink extends AbstractHyperlink {

	private IResource fResource;
	
	public ResourceHyperlink(IRegion region, String element, IResource res) {
		super(region, element);
		fResource = res;
	}

	public void open() {
		if (fResource == null)
			return;
		try {
			IPackagesViewPart part = (IPackagesViewPart)PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
			part.selectAndReveal(fResource);
		} catch (PartInitException e) {
		}
	}

}
