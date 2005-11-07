package org.eclipse.pde.internal.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;

public class ManifestHeaderScanner extends AbstractManifestScanner{
	
	public ManifestHeaderScanner(IColorManager manager, IPreferenceStore store) {
		super(manager, store);
		initialize();
	}

	protected String[] getTokenProperties() {
		return new String[] {IPDEColorConstants.P_HEADER_KEY};
	}

	protected List createRules() {
		setDefaultReturnToken(getToken(IPDEColorConstants.P_HEADER_KEY));
		List rules = new ArrayList();
		rules.add(new WordRule(new ManifestHeaderDetector(), getToken(IPDEColorConstants.P_HEADER_KEY)));
		return rules;
	}
}