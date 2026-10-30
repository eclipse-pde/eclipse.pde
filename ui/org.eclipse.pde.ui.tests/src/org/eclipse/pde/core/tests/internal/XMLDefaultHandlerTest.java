/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.Arrays;

import org.eclipse.core.internal.runtime.XmlProcessorFactory;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

public class XMLDefaultHandlerTest {

	@Test
	public void preservesSplitCharacterData() throws Exception {
		XMLDefaultHandler handler = new XMLDefaultHandler();
		handler.startDocument();
		handler.startElement("", "", "root", new AttributesImpl());
		char[] buffer = { '_', 'a', '\uD83D', '_' };
		handler.characters(buffer, 1, 2);
		Arrays.fill(buffer, '!'); // SAX may reuse its buffer after the callback.
		handler.characters(new char[0], 0, 0);
		handler.characters(new char[] { '_', '\uDE00', 'b', '_' }, 1, 2);
		handler.endElement("", "", "root");
		handler.endDocument();

		Node root = handler.getDocumentElement();
		assertEquals("a\uD83D\uDE00b", root.getTextContent());
		assertEquals(1, root.getChildNodes().getLength());
	}

	@Test
	public void preservesMixedContentAcrossElementBoundaries() throws Exception {
		Element root = parse("<root>before<child attr='a &amp; b'>inside<![CDATA[ <raw>]]>&amp;</child>"
				+ "between<empty/>after</root>", false).getDocumentElement();

		assertEquals(5, root.getChildNodes().getLength());
		assertEquals("before", root.getFirstChild().getNodeValue());
		Element child = (Element) root.getChildNodes().item(1);
		assertEquals("a & b", child.getAttribute("attr"));
		assertEquals("inside <raw>&", child.getTextContent());
		assertEquals(1, child.getChildNodes().getLength());
		assertEquals("between", root.getChildNodes().item(2).getNodeValue());
		assertEquals("empty", root.getChildNodes().item(3).getNodeName());
		assertEquals("after", root.getLastChild().getNodeValue());
	}

	@Test
	public void preservesWhitespace() throws Exception {
		Element root = parse("<root xml:space='preserve'> \t<child/> \n</root>", false).getDocumentElement();

		assertEquals("preserve", root.getAttribute("xml:space"));
		assertEquals(3, root.getChildNodes().getLength());
		assertEquals(" \t", root.getFirstChild().getNodeValue());
		assertEquals(" \n", root.getLastChild().getNodeValue());
	}

	@Test
	public void abbreviatedModeKeepsElementsAndAttributesWithoutText() throws Exception {
		Element root = parse("<root a='v'>before<child b='x'>inside</child>after</root>", true)
				.getDocumentElement();

		assertEquals("v", root.getAttribute("a"));
		assertEquals(1, root.getChildNodes().getLength());
		Element child = (Element) root.getFirstChild();
		assertEquals("child", child.getNodeName());
		assertEquals("x", child.getAttribute("b"));
		assertEquals(0, child.getChildNodes().getLength());
	}

	@Test
	public void preservesProcessingInstructionsAndTextAroundComments() throws Exception {
		Document document = parse("<?pde version='3.8'?><!--copyright--><root>before<!--comment-->after</root>",
				false);

		assertEquals(2, document.getChildNodes().getLength());
		assertEquals(Node.PROCESSING_INSTRUCTION_NODE, document.getFirstChild().getNodeType());
		assertEquals("pde", document.getFirstChild().getNodeName());
		assertEquals("version='3.8'", document.getFirstChild().getNodeValue());
		assertEquals("beforeafter", document.getDocumentElement().getTextContent());
		assertEquals(1, document.getDocumentElement().getChildNodes().getLength());
	}

	@Test
	public void preservesLongTextSplitIntoSingleCharacters() throws Exception {
		XMLDefaultHandler handler = new XMLDefaultHandler();
		handler.startDocument();
		handler.startElement("", "", "root", new AttributesImpl());
		String text = "ab\uD83D\uDE00 &<>\n".repeat(1_000);
		char[] characters = text.toCharArray();
		for (int i = 0; i < characters.length; i++) {
			handler.characters(characters, i, 1);
		}
		handler.endElement("", "", "root");
		handler.endDocument();

		Node root = handler.getDocumentElement();
		assertEquals(text, root.getTextContent());
		assertEquals(1, root.getChildNodes().getLength());
	}

	private static Document parse(String xml, boolean abbreviated) throws Exception {
		XMLDefaultHandler handler = new XMLDefaultHandler(abbreviated);
		XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE().parse(new InputSource(new StringReader(xml)), handler);
		return handler.getDocument();
	}
}
