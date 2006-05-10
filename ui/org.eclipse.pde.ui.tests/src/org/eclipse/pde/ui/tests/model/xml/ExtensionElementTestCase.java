package org.eclipse.pde.ui.tests.model.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;

public class ExtensionElementTestCase extends ExtensionTestCase {

	public static Test suite() {
		return new TestSuite(ExtensionElementTestCase.class);
	}
	
	public void testAddNewExtensionElement() throws Exception {
		StringBuffer buffer = new StringBuffer("\t<extension point=\"org.eclipse.pde.ui.samples\">\n</extension>");
		setXMLContents(buffer, LF);
		load(true);
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);

		IPluginExtension ext = extensions[0];
		ext.add(createElement("sample", ext));

		reload(1);
		
		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getPoint(), "org.eclipse.pde.ui.samples");
		assertEquals(extensions[0].getChildCount(), 1);
		assertEquals(extensions[0].getChildren()[0].getName(), "sample");
	}
	
	public void testAddExtensionElement() throws Exception {
		IPluginExtension ext = loadOneElement();
		ext.add(createElement("sample1", ext));

		ext = reloadModel(1);
		assertEquals(ext.getChildCount(), 2);
		assertEquals(ext.getChildren()[0].getName(), "sample");	
	}
	
	public void testAddNewMultipleExtensionElements() throws Exception {
		StringBuffer buffer = new StringBuffer("\n<extension point=\"org.eclipse.pde.ui.samples\">\n</extension>");
		setXMLContents(buffer, LF);
		load(true);
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);

		IPluginExtension ext = extensions[0];
		ext.add(createElement("sample1", ext));
		ext.add(createElement("sample2", ext));

		ext = reloadModel(2);
		assertEquals(ext.getChildCount(), 2);
//		TODO uncomment after post32 branch is merged
//		assertEquals(ext.getChildren()[0].getName(), "sample1");
//		assertEquals(ext.getChildren()[1].getName(), "sample2");
	}
	
	public void testAddMultipleExtensionElements() throws Exception {
		IPluginExtension ext = loadOneElement();
		
		ext.add(createElement("sample1", ext));
		ext.add(createElement("sample2", ext));

		ext = reloadModel(2);
		assertEquals(ext.getChildCount(), 3);
		assertEquals(ext.getChildren()[0].getName(), "sample");
//		TODO uncomment after post32 branch is merged
//		assertEquals(ext.getChildren()[1].getName(), "sample1");
//		assertEquals(ext.getChildren()[2].getName(), "sample2");
	}
	
	public void testRemoveExtensionElement() throws Exception {
		IPluginExtension ext = loadOneElement();
		assertEquals(ext.getChildCount(), 1);
		
		ext.remove(ext.getChildren()[0]);

		ext = reloadModel(1);
		assertEquals(ext.getChildCount(), 0);
	}
	
	public void testRemoveMulitpleExtensionElements() throws Exception {
		StringBuffer sb = new StringBuffer("<extension point=\"org.eclipse.pde.ui.samples\"><sample />");
		sb.append("\t<sample1/>\t\n<sample2 /></extension>"); 
		setXMLContents(sb, LF);	
		load(true);
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		IPluginExtension ext = extensions[0];
		assertEquals(ext.getChildCount(), 3);
		assertEquals(ext.getPoint(), "org.eclipse.pde.ui.samples");
		IPluginObject[] children = ext.getChildren();
		assertEquals(children[0].getName(), "sample");
		assertEquals(children[1].getName(), "sample1");
		assertEquals(children[2].getName(), "sample2");
		
		ext.remove(children[0]);
		ext.remove(children[2]);
		
		ext = reloadModel(2);
		assertEquals(ext.getChildCount(), 1);
		assertEquals(ext.getChildren()[0].getName(), "sample1");
	}
	
	private IPluginElement createElement(String name, IPluginExtension parent) throws CoreException {
		IPluginElement result = fModel.getFactory().createElement(parent);
		result.setName(name);
		return result;
	}

}
