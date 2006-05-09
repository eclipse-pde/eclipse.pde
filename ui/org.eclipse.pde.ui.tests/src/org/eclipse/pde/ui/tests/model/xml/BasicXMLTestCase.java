package org.eclipse.pde.ui.tests.model.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.core.plugin.IExtensionsModelFactory;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;

public class BasicXMLTestCase extends XMLModelTestCase {

	public static Test suite() {
		return new TestSuite(BasicXMLTestCase.class);
	}
	
	public void testReadSimpleExtension() {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension point=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, LF);
		load();
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}
	
	public void testReadMultilineSimpleExtension() {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension ");
		sb.append(LF);
		sb.append("point=\"org.eclipse.pde.ui.samples\">");
		sb.append(LF);
		sb.append("<sample />");
		sb.append(LF);
		sb.append("</extension>");				
		setXMLContents(sb, LF);
		load();
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}
	
	public void testAddSimpleExtension() throws Exception {
		setXMLContents(null, LF);
		load(true);
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 0);

		IExtensionsModelFactory factory = fModel.getFactory();
		IPluginExtension ext = factory.createExtension();
		ext.setPoint("org.eclipse.pde.ui.samples");
		IPluginElement obj = factory.createElement(ext);
		obj.setName("sample");
		ext.add(obj);
		fModel.getPluginBase().add(ext);

		reload(1);
		
		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}
	
	public void testRemoveSimpleExtension() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\"><sample /></extension>");
		setXMLContents(sb, LF);
		load(true);
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		
		fModel.getPluginBase().remove(extensions[0]);

		reload(1);
		
		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 0);
	}
}
