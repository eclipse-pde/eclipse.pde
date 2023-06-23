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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.pde.internal.core.util.XmlDocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;
public class XmlDocumentBuilderFactoryTest {

	List<Path> tmpFiles = new ArrayList<>();

	@Test
	public void testTransformXmlWithExternalEntity() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = XmlDocumentBuilderFactory
				.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
		try {
			testParseXmlWithExternalEntity(documentBuilderFactory, this::createMalciousXml);
			assertTrue("SAXParseException expected", false);
		} catch (SAXParseException e) {
			String message = e.getMessage();
			assertTrue(message, message.contains("DOCTYPE"));
		}
	}

	@Test
	public void testTransformXmlWithoutExternalEntity() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = XmlDocumentBuilderFactory
				.createDocumentBuilderFactoryWithErrorOnDOCTYPE();
		testParseXmlWithExternalEntity(documentBuilderFactory, this::createNormalXml);
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

	public void testParseXmlWithExternalEntity(DocumentBuilderFactory documentBuilderFactory,
			IntFunction<InputStream> xmlSupplier)
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
			Document document;

			try (InputStream xmlStream = xmlSupplier.apply(localPort)) {
				DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
				document = builder.parse(xmlStream);
			}
			Element root = document.getDocumentElement();

			assertEquals("Body", root.getTagName());
			String value = root.getChildNodes().item(0).getNodeValue();
			assertFalse("Parser injected secret: " + value, value.contains("secret"));
		} finally {
			cleanup();
		}
		httpServerThread.join(5000);
		assertFalse(httpServerThread.isAlive());
		for (Throwable e : exceptionsInOtherThreads) {
			throw new InvocationTargetException(e, e.getMessage());
		}
	}
}
