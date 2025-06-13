package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddSpaceBeforeValue extends AbstractManifestMarkerResolution {

	public AddSpaceBeforeValue(int type, IMarker marker) {
		super(type, marker);
	}

	@Override
	public String getLabel() {
		return PDEUIMessages.AddSpaceAfterColon_add;
	}

	@Override
	protected void createChange(BundleModel model) {
		try {
			IDocument doc = model.getDocument();
			int lineNum = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			IRegion lineInfo = doc.getLineInformation(lineNum - 1);
			int offset = lineInfo.getOffset();
			int length = lineInfo.getLength();

			String getLine = doc.get(offset, length);
			int colonInd = getLine.indexOf(':');

			if (colonInd > 0 && getLine.charAt(colonInd + 1) != ' ') {
				String userHeader = getLine.substring(0, colonInd + 1);
				userHeader = userHeader + " "; //$NON-NLS-1$
				doc.replace(offset, colonInd + 1, userHeader);
			}
		} catch (BadLocationException e) {
			// the location has been checked to ensure that it cannot be a bad
			// location
			e.printStackTrace();
		}

	}

}
