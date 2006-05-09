package org.eclipse.pde.ui.tests.model.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.core.plugin.IPluginExtension;

public class BasicXMLTestCase extends XMLModelTestCase {

	public static Test suite() {
		return new TestSuite(BasicXMLTestCase.class);
	}
	
	public void testReadSimpleExtensions() {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, LF);
		load();
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getId(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}
}
