package org.eclipse.pde.ui.tests.model.xml;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;

public class SwapXMLModelTestCase extends XMLModelTestCase {
	
	public static Test suite() {
		return new TestSuite(SwapXMLModelTestCase.class);
	}
	
	// all one one line
	public void testSwapTwoChildren() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append("<child id=\"a\" /><child id=\"b\" />");
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}
	
	// all on diff line
	public void testSwapTwoChildren2() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append(LF);
		sb.append("<child id=\"a\" />");
		sb.append(LF);
		sb.append("<child id=\"b\" />");
		sb.append(LF);
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}
	
	// all on diff line with tabs
	public void testSwapTwoChildren3() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append(LF);
		sb.append("\t<child id=\"a\" />");
		sb.append(LF);
		sb.append("\t<child id=\"b\" />");
		sb.append(LF);
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}
	
	// some on diff lines with no spacing
	public void testSwapTwoChildren4() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append("<child id=\"a\" />");
		sb.append(LF);
		sb.append("<child id=\"b\" />");
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}
	
	// some on diff lines with spacing
	public void testSwapTwoChildren5() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<extension id=\"org.eclipse.pde.ui.samples\">");
		sb.append("\t<child id=\"a\" />");
		sb.append(LF);
		sb.append("\t<child id=\"b\" />");
		sb.append(LF);
		sb.append("</extension>");
		setXMLContents(sb, LF);
		twoChildSwap();
	}
	
	private void twoChildSwap() throws Exception {
		load(true);
		
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);
		
		IPluginObject[] children = extensions[0].getChildren();
		
		assertEquals(2, children.length);
		assertTrue(children[0] instanceof IPluginElement);
		assertTrue(children[1] instanceof IPluginElement);
		assertEquals("a", ((IPluginElement)children[0]).getAttribute("id").getValue());
		assertEquals("b", ((IPluginElement)children[1]).getAttribute("id").getValue());
		
		extensions[0].swap(children[0], children[1]);
		
		// move source edit - only one op
		reload(1);
		
		extensions = fModel.getPluginBase().getExtensions();
		assertEquals(1, extensions.length);
		
		children = extensions[0].getChildren();
		
		assertEquals(2, children.length);
		assertTrue(children[0] instanceof IPluginElement);
		assertTrue(children[1] instanceof IPluginElement);
		assertEquals("b", ((IPluginElement)children[0]).getAttribute("id").getValue());
		assertEquals("a", ((IPluginElement)children[1]).getAttribute("id").getValue());
	}
}
