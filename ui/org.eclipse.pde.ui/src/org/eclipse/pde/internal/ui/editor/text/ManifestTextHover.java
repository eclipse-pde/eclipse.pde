package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;

public class ManifestTextHover extends PDETextHover {

	private PDESourcePage fSourcePage;
	private IJavaProject fJP;
	
	public ManifestTextHover(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
		IProject project = ((PDEFormEditor)fSourcePage.getEditor()).getCommonProject();
		fJP = JavaCore.create(project);
	}
	
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		int offset = hoverRegion.getOffset();
		IDocumentRange range = fSourcePage.getRangeElement(offset, true);
		if (range instanceof IManifestHeader) {
			IManifestHeader header = (IManifestHeader)range;
			String headerName = header.getName();
			if (offset >= header.getOffset() + headerName.length())
				return null;
			return PDEJavaHelper.getOSGIConstantJavaDoc(headerName, fJP);
		}
		return null;
	}

}
