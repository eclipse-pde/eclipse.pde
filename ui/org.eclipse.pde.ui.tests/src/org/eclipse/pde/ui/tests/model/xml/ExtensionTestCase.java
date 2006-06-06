package org.eclipse.pde.ui.tests.model.xml;

import org.eclipse.pde.core.plugin.IPluginExtension;

public class ExtensionTestCase extends XMLModelTestCase {

	protected final IPluginExtension reloadModel() {
		reload();
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		return extensions[0];
	}
}
