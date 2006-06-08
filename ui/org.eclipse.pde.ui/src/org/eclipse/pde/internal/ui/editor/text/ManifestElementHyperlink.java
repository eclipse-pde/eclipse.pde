package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public abstract class ManifestElementHyperlink implements IHyperlink {

	private IRegion fRegion;
	protected String fElement;

	public ManifestElementHyperlink(IRegion region, String element) {
		fRegion = region;
		fElement = element;
	}

	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	public String getHyperlinkText() {
		return null;
	}

	public String getTypeLabel() {
		return null;
	}
	
	protected abstract void open2();
	
	public void open() {
		// remove whitespace inbetween chars
		int len = fElement.length();
		StringBuffer sb = new StringBuffer(len);
		for (int i = 0; i < len; i++) {
			char c = fElement.charAt(i);
			if (!Character.isWhitespace(c))
				sb.append(c);
		}
		fElement = sb.toString();
		open2();
	}
}
