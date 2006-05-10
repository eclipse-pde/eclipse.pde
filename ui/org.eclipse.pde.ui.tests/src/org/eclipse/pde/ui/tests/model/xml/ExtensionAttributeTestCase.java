package org.eclipse.pde.ui.tests.model.xml;

import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ExtensionAttributeTestCase extends ExtensionTestCase {

	public static Test suite() {
		return new TestSuite(ExtensionAttributeTestCase.class);
	}
	
	public void testAddNewExtensionAttribute() throws Exception {
		IPluginExtension ext = loadOneElement();
		IPluginObject child = ext.getChildren()[0];
		
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		elem.setAttribute("id", "org.eclipse.pde.sample1");
		
		ext = reloadModel(1);
		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
	}
	
	public void testAddExtensionAttribute() throws Exception { 
		StringBuffer sb = new StringBuffer("<extension point=\"org.eclipse.pde.ui.samples\">");
		sb.append("<sample\t id=\"org.eclipse.pde.sample1\"/></extension>");
		setXMLContents(sb, LF);
		load(true);
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];
		
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("name", "pde sample");
		
		IPluginExtension ext = reloadModel(1);
		
		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 2);
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
	}
	
	public void testAddNewMultipleAttributes() throws Exception {
		IPluginExtension ext = loadOneElement();
		IPluginObject child = ext.getChildren()[0];
		
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		elem.setAttribute("id", "org.eclipse.pde.sample1");
		elem.setAttribute("name", "pde sample");
		
		ext = reloadModel(1);
		
		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
	}
	
	public void testRemoveExtensionAttribute() throws Exception {
		StringBuffer sb = new StringBuffer("<extension point=\"org.eclipse.pde.ui.samples\">\t");
		sb.append("<sample id=\"org.eclipse.pde.sample1\"/>\t</extension>");
		setXMLContents(sb, LF);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];
		
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("id", null);
		
		IPluginExtension ext = reloadModel(1);
		
		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 0);
	}
	
	public void testRemoveMultipleExtensionAttributes() throws Exception {
		StringBuffer sb = new StringBuffer("<extension point=\"org.eclipse.pde.ui.samples\">\n");
		sb.append("<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/></extension>");
		setXMLContents(sb, LF);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];
		
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("id", null);
		elem.setAttribute("perspectiveId", null);
		
		IPluginExtension ext = reloadModel(2);
		
		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 1);
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
	}
	
	public void testChangeExtensionAttribute() throws Exception {
		StringBuffer sb = new StringBuffer("<extension point=\"org.eclipse.pde.ui.samples\">\n");
		sb.append("<sample id=\"org.eclipse.pde.sample1\" name=\"pde sample\" perspectiveId=\"org.eclipse.pde.ui.PDEPerspective\"/></extension>");
		setXMLContents(sb, LF);
		load(true);

		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(extensions.length, 1);
		assertEquals(extensions[0].getChildCount(), 1);
		IPluginObject child = extensions[0].getChildren()[0];
		assertTrue(child instanceof IPluginElement);
		IPluginElement elem = (IPluginElement) child;
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample1");
		elem.setAttribute("id", "org.eclipse.pde.sample2");
		
		IPluginExtension ext = reloadModel(1);
		
		assertEquals(ext.getChildCount(), 1);
		elem = (IPluginElement) ext.getChildren()[0];
		assertEquals(elem.getName(), "sample");
		assertEquals(elem.getAttributeCount(), 3);
		assertEquals(elem.getAttribute("id").getValue(), "org.eclipse.pde.sample2");
		assertEquals(elem.getAttribute("name").getValue(), "pde sample");
		assertEquals(elem.getAttribute("perspectiveId").getValue(), "org.eclipse.pde.ui.PDEPerspective");
	}
}
