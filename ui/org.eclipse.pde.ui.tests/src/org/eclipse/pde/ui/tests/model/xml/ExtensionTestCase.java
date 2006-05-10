package org.eclipse.pde.ui.tests.model.xml;

import org.eclipse.pde.core.plugin.IPluginExtension;

public class ExtensionTestCase extends XMLModelTestCase {
	
	protected final IPluginExtension loadOneElement() {
		StringBuffer sb = new StringBuffer("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, LF);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		return extensions[0];
	}
	
	protected final IPluginExtension reloadModel(int expectedOps) {
		reload(expectedOps);
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		return extensions[0];
	}
}
