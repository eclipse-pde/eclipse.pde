/*******************************************************************************
 *  Copyright (c) 2023 Joerg Kubitz and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.IntFunction;

import javax.xml.parsers.SAXParser;

import org.eclipse.pde.internal.core.util.XmlParserFactory;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlParserFactoryTest {

	List<Path> tmpFiles = new ArrayList<>();

	@Test
	public void testParseXmlWithExternalEntity() throws Exception {
		SAXParser parser =XmlParserFactory.createSAXParserWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(parser, this::createMalciousXml);
			assertTrue("SAXParseException expected", false);
		} catch (SAXParseException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DOCTYPE"));
			assertTrue(message, message.contains("http://apache.org/xml/features/disallow-doctype-decl"));
		}
	}

	@Test
	public void testParseXmlWithoutExternalEntity() throws Exception {
		SAXParser parser = XmlParserFactory.createSAXParserWithErrorOnDOCTYPE();
		testParseXmlWithExternalEntity(parser, this::createNormalXml);
	}

	@Test
	public void testParseXmlWithIgnoredExternalEntity() throws Exception {
		SAXParser parser = XmlParserFactory.createSAXParserIgnoringDOCTYPE();
		testParseXmlWithExternalEntity(parser, this::createMalciousXml);
	}

	@Test
	public void testParseXmlWithoutIgnoredExternalEntity() throws Exception {
		SAXParser parser = XmlParserFactory.createSAXParserIgnoringDOCTYPE();
		testParseXmlWithExternalEntity(parser, this::createNormalXml);
	}

	InputStream createMalciousXml(int localPort) {
		try {
			Path tempSecret = Files.createTempFile("test", ".txt");
			tmpFiles.add(tempSecret);
			Files.writeString(tempSecret, "secret");
			Path tempDtd = Files.createTempFile("test", ".dtd");
			tmpFiles.add(tempDtd);
			String dtdContent = "<!ENTITY % var1 SYSTEM \"" + tempSecret.toUri().toURL() + "\">\n" //
					+ "<!ENTITY var4 SYSTEM \"" + tempSecret.toUri().toURL() + "\">\n" //
					+ "<!ENTITY % var2 \"<!ENTITY var3 SYSTEM 'http://localhost:" + localPort + "/?%var1;'>\">\n" //
					+ "%var2;\n";
			Files.writeString(tempDtd, dtdContent);
			StringBuilder sb = new StringBuilder();
			sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			URI dtdURi = tempDtd.toUri();
			String dtdUrl = dtdURi.toURL().toString();
			sb.append("<!DOCTYPE var1 SYSTEM \"" + dtdUrl + "\">\n");
			sb.append("<Body>&var3;&var4;</Body>");
			String xml = sb.toString();
			return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	InputStream createNormalXml(int localPort) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<Body>hello</Body>");
		String xml = sb.toString();
		return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
	}

	private void cleanup() throws IOException {
		for (Path path : tmpFiles) {
			Files.delete(path);
		}
	}

	public void testParseXmlWithExternalEntity(SAXParser parser, IntFunction<InputStream> xmlSupplier)
			throws Exception {
		Collection<Throwable> exceptionsInOtherThreads = new ConcurrentLinkedQueue<>();
		Thread httpServerThread = null;
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			int localPort = serverSocket.getLocalPort();
			httpServerThread = new Thread("httpServerThread") {
				@Override
				public void run() {
					try (Socket socket = serverSocket.accept()) {
						try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
							String firstLine = in.readLine();
							try (OutputStream outputStream = socket.getOutputStream()) {
								outputStream.write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
							}
							assertTrue(firstLine, firstLine.startsWith("GET"));
							assertFalse("Server received secret: " + firstLine, firstLine.contains("secret")); // var3
							assertFalse("Server was contacted", true);
						}
					} catch (SocketException closed) {
						// expected
					} catch (Throwable e) {
						exceptionsInOtherThreads.add(e);
					}
				}
			};
			httpServerThread.start();
			List<String> elements = new ArrayList<>();
			DefaultHandler handler = new DefaultHandler() {
				@Override
				public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes)
						throws org.xml.sax.SAXException {
					elements.add(qName);
				}

				@Override
				public void characters(char ch[], int start, int length) throws SAXException {
					String content = new String(ch, start, length);
					assertFalse("Secret was injected into xml: " + content, content.contains("secret")); // var4
				}

				@Override
				public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
					// implementation that would do any remote call:
					try {
						return new InputSource(URI.create(systemId).toURL().openStream());
					} catch (IOException exception) {
						throw new SAXException(exception);
					}
					// Also the default impl injects files:
					// return null;

					// Does also prevent access to external files:
					// return new InputSource(new StringReader(""));
				}

			};
			try (InputStream xmlStream = xmlSupplier.apply(localPort)) {
				parser.parse(xmlStream, handler);
			}
			assertEquals(List.of("Body"), elements);
		} finally {
			cleanup();
		}
		httpServerThread.join(5000);
		assertFalse(httpServerThread.isAlive());
		for (Throwable e : exceptionsInOtherThreads) {
			throw new InvocationTargetException(e, e.getMessage());
		}
	}

	static volatile Object sink;

	public static void main(String[] args) throws Exception {
		// testParserCreatePerformance
		for (int i = 1; i < 1000; i++) {
			long n0 = System.nanoTime();
			sink = XmlParserFactory.createSAXParserIgnoringDOCTYPE();
			long n1 = System.nanoTime();
			System.out.println("run " + i + ": " + (n1 - n0) + "ns");
			// ~ run 999: 60000ns
		}
	}
}
