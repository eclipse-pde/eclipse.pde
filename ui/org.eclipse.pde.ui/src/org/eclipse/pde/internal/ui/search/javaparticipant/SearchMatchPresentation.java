package org.eclipse.pde.internal.ui.search.javaparticipant;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.search.IMatchPresentation;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PartInitException;

public class SearchMatchPresentation implements IMatchPresentation {

	private ILabelProvider fLabelProvider;
	
	private class LabelProvider extends SharedLabelProvider {
		private Image fTypeImage;
		private Image fPackageImage;
		private Point fImageSize = new Point(22, 16);
		
		private LabelProvider() {
			fTypeImage = new JavaElementImageDescriptor(PDEPluginImages.DESC_CLASS_OBJ, 0, fImageSize).createImage();
			fPackageImage = new JavaElementImageDescriptor(PDEPluginImages.DESC_PACKAGE_OBJ, 0, fImageSize).createImage();
		}
		
		public Image getImage(Object element) {
			if (element instanceof SearchHit)
				return ((SearchHit)element).isTypeHit() ? fTypeImage : fPackageImage;
			return super.getImage(element);
		}

		public String getText(Object element) {
			String value = null;
			IResource resource = null;
			if (element instanceof SearchHit) {
				value = ((SearchHit)element).getValue();
				resource = ((SearchHit)element).getResource();
				return resource.getFullPath().toOSString().substring(1) + " - " + value; //$NON-NLS-1$
			}
			return super.getText(element);
		}
		
		public void dispose() {
			if (fTypeImage != null && !fTypeImage.isDisposed())
				fTypeImage.dispose();
			if (fPackageImage != null && !fPackageImage.isDisposed())
				fPackageImage.dispose();
			super.dispose();
		}
	}
	
	public ILabelProvider createLabelProvider() {
		if (fLabelProvider == null)
			fLabelProvider = new LabelProvider();
		return fLabelProvider;
	}

	public void showMatch(Match match, int currentOffset, int currentLength,
			boolean activate) throws PartInitException {
		ClassSearchEditorOpener.open(match);
	}

}
