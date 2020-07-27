/*******************************************************************************
 * Copyright (c) 2020, Alex Blewitt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Blewitt - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.p2.core.helpers.URLUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.core.plugin.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.*;

/**
 * Tests for target platform.
 */
public class TargetPlatformTests {
	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	@Deprecated
	public void testCreatePlatformConfigurationEmpty() throws Exception {
		File location = temp.newFolder("testCreatePlatformConfigurationEmpty");
		IPluginModelBase[] plugins = new IPluginModelBase[0];
		TargetPlatform.createPlatformConfiguration(location, plugins, null);
		checkPlatformXML(location, plugins);
	}

	@Test
	@Deprecated
	public void testCreatePlatformConfigurationSingle() throws Exception {
		File location = temp.newFolder("testCreatePlatformConfigurationEmpty");
		IPluginModelBase[] plugins = PluginRegistry.getExternalModels();
		Location platform = Platform.getInstallLocation();
		File platfile = URLUtil.toFile(platform.getURL());
		System.out.println(platfile);
		TargetPlatform.createPlatformConfiguration(location, plugins, null);
		checkPlatformXML(location, plugins);
	}

	private void checkPlatformXML(File location, IPluginModelBase[] plugins) throws Exception {
		File parent = new File(location, "org.eclipse.update");
		File platform_xml = new File(parent, "platform.xml");
		assertThat(platform_xml).exists();
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(platform_xml);
		assertEquals("1.0", document.getXmlVersion());
		assertEquals("UTF-8", document.getXmlEncoding());
		Element config = document.getDocumentElement();
		assertEquals("config", config.getNodeName());
		assertEquals("true", config.getAttribute("transient"));
		assertEquals("3.0", config.getAttribute("version"));
		Date date = new Date(Long.parseLong(config.getAttribute("date")));
		assertThat(date).isBeforeOrEqualTo(new Date());
		NodeList sites = config.getElementsByTagName("site");
		if (plugins.length == 0) {
			assertEquals(0, sites.getLength());
		} else {
			assertNotEquals(0, sites.getLength());
		}
		int pluginCount = plugins.length;
		for (int i = 0; i < sites.getLength(); i++) {
			Element node = (Element) sites.item(i);
			assertEquals("true", node.getAttribute("enabled"));
			assertTrueIfNotEmpty(node.getAttribute("updatable"));
			assertEquals("USER-INCLUDE", node.getAttribute("policy"));
			assertThat(node.getAttribute("url")).startsWith("file:/");
			assertNotNull(node.getAttribute("list"));
			assertTrue(hasNoChildren(node));
			String[] list = node.getAttribute("list").split(",");
			pluginCount -= list.length;
		}
		assertEquals(0, pluginCount);
		NodeList features = config.getElementsByTagName("feature");
		if (plugins.length == 0) {
			assertEquals(0, features.getLength());
		} else {
			assertNotEquals(0, features.getLength());
		}
		for (int i = 0; i < features.getLength(); i++) {
			Element node = (Element) features.item(i);
			assertTrueIfNotEmpty(node.getAttribute("enabled"));
			String url = node.getAttribute("url");
			assertThat(url).startsWith("features");
			Matcher matcher = featurePattern.matcher(url);
			if (matcher.matches()) {
				// Relies on naming convention feature/id_version
				assertEquals(matcher.group(1), node.getAttribute("id"));
				assertEquals(matcher.group(2), node.getAttribute("version"));
			}
			assertTrue(hasNoChildren(node));
		}
		return;
	}

	Pattern featurePattern = Pattern.compile(".*/([^_/]*)_([^/]*)");

	private static void assertTrueIfNotEmpty(String string) {
		if (string != null && !string.isEmpty()) {
			assertEquals("true", string);
		}
	}

	private static boolean hasNoChildren(Element node) {
		NodeList children = node.getChildNodes();
		if (children != null && children.getLength() == 1) {
			Node child = children.item(0);
			return child == null || "#text".equals(child.getNodeName());
		}
		return true;
	}
}
