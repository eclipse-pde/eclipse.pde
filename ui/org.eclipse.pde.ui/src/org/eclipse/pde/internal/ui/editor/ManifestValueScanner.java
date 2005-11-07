package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;

public class ManifestValueScanner extends AbstractManifestScanner {

	class AssignmentDetector implements IWordDetector {
		public boolean isWordStart(char c) {
			if (':' != c || fDocument == null)
				return false;
			try {
				// check whether it is the first ':'
				IRegion lineInfo = fDocument.getLineInformationOfOffset(fOffset);
				int offset = lineInfo.getOffset();
				String line = fDocument.get(offset, lineInfo.getLength());
				int i = line.indexOf(c);
				return i != -1 && i + lineInfo.getOffset() + 1 == fOffset;
			} catch (BadLocationException ex) {
				return false;
			}
		}
		public boolean isWordPart(char c) {
			return false;
		}
	}
	
	public ManifestValueScanner(IColorManager manager, IPreferenceStore store) {
		super(manager, store);
		initialize();
	}

	protected String[] getTokenProperties() {
		return new String[] {IPDEColorConstants.P_HEADER_VALUE, IPDEColorConstants.P_HEADER_ASSIGNMENT};
	}

	protected List createRules() {
		setDefaultReturnToken(getToken(IPDEColorConstants.P_HEADER_VALUE));
		List rules = new ArrayList();
		rules.add(new WordRule(new AssignmentDetector(), getToken(IPDEColorConstants.P_HEADER_ASSIGNMENT)));
		rules.add(new WhitespaceRule(new IWhitespaceDetector() {
			public boolean isWhitespace(char c) {
				return Character.isWhitespace(c);
			}
		}));

		return rules;
	}
}