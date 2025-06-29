package org.eclipse.pde.internal.ui.correction;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class AddSpaceBeforeValue extends AbstractManifestMarkerResolution {

	private static final Logger logger = Logger.getLogger(AddSpaceBeforeValue.class.getName());

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
			logger.log(Level.SEVERE, "Failed to apply AddSpaceBeforeValue quick fix, unexpected location in the doc", //$NON-NLS-1$
					e);
		}

	}

}