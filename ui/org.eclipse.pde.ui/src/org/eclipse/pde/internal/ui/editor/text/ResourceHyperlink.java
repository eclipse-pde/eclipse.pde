package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class ResourceHyperlink extends AbstractHyperlink {

	private IResource fResource;
	
	public ResourceHyperlink(IRegion region, String element, IResource res) {
		super(region, element);
		fResource = res;
	}

	public void open() {
		try {
			if (fResource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)fResource, true);
			else if (fResource != null) {
				IPackagesViewPart part = (IPackagesViewPart)PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
				part.selectAndReveal(fResource);
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

}
