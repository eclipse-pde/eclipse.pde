package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

public abstract class AbstractHyperlink implements IHyperlink {

	protected String fElement;
	private IRegion fRegion;
	
	public AbstractHyperlink(IRegion region, String element) {
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

}
