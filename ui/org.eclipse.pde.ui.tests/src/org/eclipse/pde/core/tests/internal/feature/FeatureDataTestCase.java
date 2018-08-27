/*******************************************************************************
 * Copyright (c) 2016, 2017 Martin Karpisek and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Martin Karpisek <martin.karpisek@gmail.com> - initial API and implementationBug 322975
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.feature;

import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import junit.framework.TestCase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.feature.FeatureData;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
public class FeatureDataTestCase extends TestCase {

	private static final String FEATURE_INDENT = "      ";

	public void testSerialization() throws Exception {
		String expected = String.format("<data%n" +
				FEATURE_INDENT + "id=\"id.1\"%n" + FEATURE_INDENT + "os=\"win32\"%n" + FEATURE_INDENT
				+ "arch=\"x86_64\"%n" + FEATURE_INDENT
				+ "download-size=\"1000\"%n" +
				FEATURE_INDENT + "install-size=\"0\"/>%n"
				);
		FeatureData data = newFeatureData();
		String actual = toXml(data);
		assertEquals(expected, actual);

		FeatureData data2 = fromXml(actual);
		assertEquals(data.getId(), data2.getId());
		assertEquals(data.getOS(), data2.getOS());
		assertEquals(data.getArch(), data2.getArch());
		assertEquals(data.getFilter(), data2.getFilter());
		assertEquals(data.getDownloadSize(), data2.getDownloadSize());
	}

	public void testSerializationWithLdapFilter() throws CoreException {
		String expected = String.format("<data%n" + FEATURE_INDENT + "id=\"id.1\"%n" + FEATURE_INDENT + "os=\"win32\"%n"
				+ FEATURE_INDENT + "arch=\"x86_64\"%n" + FEATURE_INDENT
				+ "filter=\"(&amp; (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86))\"%n" + FEATURE_INDENT
				+ "download-size=\"1000\"%n" + FEATURE_INDENT
				+ "install-size=\"0\"/>%n");
		FeatureData data = newFeatureData();
		data.setFilter("(& (osgi.ws=win32) (osgi.os=win32) (osgi.arch=x86))");
		String actual = toXml(data);
		assertEquals(expected, actual);
	}

	private FeatureData newFeatureData() throws CoreException {
		IFeatureModel mock = new WorkspaceFeatureModel();

		FeatureData data = new FeatureData();
		data.setModel(mock);
		data.setId("id.1");
		data.setLabel("Feature1");
		data.setArch("x86_64");
		data.setOS("win32");
		data.setDownloadSize(1000);
		return data;
	}

	private String toXml(FeatureData data) {
		StringWriter sw = new StringWriter();
		data.write("", new PrintWriter(sw));
		String actual = sw.toString();
		return actual;
	}

	public static FeatureData fromXml(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		Document doc = builder.parse(is);

		assertNotNull(doc);

		NodeList nodes = doc.getElementsByTagName("data");
		assertEquals(1, nodes.getLength());
		Node node = nodes.item(0);

		FeatureDataForTesting data = new FeatureDataForTesting();
		data.parse(node);
		return data;
	}

	@SuppressWarnings("serial")
	public static class FeatureDataForTesting extends FeatureData {
		@Override
		public void parse(Node node) {
			super.parse(node);
		}
	}
}
