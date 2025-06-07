package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class UpdateCorrectHeaderName extends AbstractManifestMarkerResolution {

	public UpdateCorrectHeaderName(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.RemoveInvalidCharacters;
	}

	private String removeInvalidChars(String userHeader) {
		return userHeader.replaceAll("[^a-zA-Z0-9-_]", ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected void createChange(BundleModel model) {
		model.getBundle();
		IDocument doc = model.getDocument();
		try {
			int lineNum = (int) marker.getAttribute(IMarker.LINE_NUMBER);
			IRegion lineInfo = doc.getLineInformation(lineNum - 1);
			int offset = lineInfo.getOffset();
			int length = lineInfo.getLength();

			String getLine = doc.get(offset, length);
			int colonInd = getLine.indexOf(':');

			if (colonInd > 0) {
				String userHeader = getLine.substring(0, colonInd);
				String correctHeader = removeInvalidChars(userHeader);
				doc.replace(offset, colonInd, correctHeader);
			}

		} catch (Exception e) {

		}

	}

}
