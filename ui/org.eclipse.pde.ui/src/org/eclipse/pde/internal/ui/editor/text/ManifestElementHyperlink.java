package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IRegion;

public abstract class ManifestElementHyperlink extends AbstractHyperlink {

	public ManifestElementHyperlink(IRegion region, String element) {
		super(region, element);
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
