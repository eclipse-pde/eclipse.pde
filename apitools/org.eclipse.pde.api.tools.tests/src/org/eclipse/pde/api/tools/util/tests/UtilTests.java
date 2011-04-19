/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.search.IReferenceCollection;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.SinceTagVersion;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Tests the methods in our utility class: {@link Util}
 * 
 * @since 1.0.0
 */
public class UtilTests extends TestCase {

	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test_source");
	
	/**
	 * Tests that passing in <code>null</code> to the getAllFiles(..) method
	 * will return all of the files in that directory
	 */
	public void getAllFilesNullFilter() {
		File root = new File(SRC_LOC.toOSString());
		assertTrue("The test source  directory must exist", root.exists());
		assertTrue("The source location should be a directory", root.isDirectory());
		File[] files = Util.getAllFiles(root, null);
		assertTrue("There should be more than one file in the test source directory", files.length > 1);
	}
	
	/**
	 * Tests that passing an illegal argument when creating a new {@link SinceTagVersion} throws an exception
	 */
	public void testIllegalArgSinceTagVersion() {
		try {
			new SinceTagVersion(null);
			fail("creating a since tag version with a null value should have thrown an exception");
		}
		catch(IllegalArgumentException iae) {
			
		}
	}
	
	/**
	 * Tests that a filter passed to getALlFiles(..) returns only the files 
	 * it is supposed to. In this test only files that end with <code>TestClass1.java</code>
	 * pass the filter (there is only one).
	 */
	public void getAllFilesSpecificFilter() {
		File root = new File(SRC_LOC.toOSString());
		assertTrue("The test source  directory must exist", root.exists());
		assertTrue("The source location should be a directory", root.isDirectory());
		File[] files = Util.getAllFiles(root, new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith("TestClass1.java");
			}
		});
		assertTrue("There should be only one file in the test source directory named 'TestClass1.java'", files.length == 1);
	}
	
	/**
	 * Tests that the isClassFile method works as expected when passed a valid name (*.class)
	 */
	public void testIsClassfile() {
		assertTrue("Test.class is a class file", Util.isClassFile("Test.class"));
	}
	
	/**
	 * Tests that the isClassFile method works as expected when passed an invalid name (not *.class)
	 */
	public void testIsNotClassfile() {
		assertTrue("Test.notclass is not a classfile", !Util.isClassFile("Test.notclass"));
	}
	
	/**
	 * Tests that the isArchive method works as expected when passed a valid archive 
	 * name (*.zip or *.jar)
	 */
	public void testIsArchive() {
		assertTrue("Test.zip is an archive", Util.isArchive("Test.zip"));
		assertTrue("Test.jar is an archive", Util.isArchive("Test.jar"));
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.tar.gz"));
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.tgz"));
	}
	
	/**
	 * Tests that the isTGZFile method works as expected
	 */
	public void testIsTar() {
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.tar.gz"));
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.TAR.GZ"));
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.Tar.Gz"));
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.tar.GZ"));
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.TAR.gz"));
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.tgz"));
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.TGZ"));
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.Tgz"));
	}
	
	/**
	 * Tests that the isZipJarFile method works as expected
	 */
	public void testIsJar() {
		assertTrue("Test.tar.gz is an archive", Util.isZipJarFile("Test.zip"));
		assertTrue("Test.tar.gz is an archive", Util.isZipJarFile("Test.ZIP"));
		assertTrue("Test.tar.gz is an archive", Util.isZipJarFile("Test.Zip"));
		assertTrue("Test.tgz is an archive", Util.isZipJarFile("Test.jar"));
		assertTrue("Test.tgz is an archive", Util.isZipJarFile("Test.JAR"));
		assertTrue("Test.tgz is an archive", Util.isZipJarFile("Test.Jar"));
	}
	
	/**
	 * Tests that the isArchive method works as expected when passed an invalid archive 
	 * name (*.notzip)
	 */
	public void testisNotArchive() {
		assertTrue("Test.notzip is not an archive", !Util.isArchive("Test.notzip"));
	}
	
	/*
	 * Test org.eclipse.pde.api.tools.internal.util.Util.getFragmentNumber(String)
	 * org.eclipse.pde.api.tools.internal.util.Util.isGreatherVersion(String, String)
	 */
	public void testGetFragmentNumber() {
		assertEquals("wrong value", 2, Util.getFragmentNumber("org.eclipse.core.filesystem 1.0"));
		assertEquals("wrong value", 2, Util.getFragmentNumber("1.0"));
		assertEquals("wrong value", 3, Util.getFragmentNumber("1.0.0"));
		assertEquals("wrong value", 1, Util.getFragmentNumber("1"));
		assertEquals("wrong value", 4, Util.getFragmentNumber("org.test 1.0.0.0"));
		try {
			Util.getFragmentNumber((String) null);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		assertEquals("wrong value", -1, Util.getFragmentNumber("org.test "));
		assertEquals("wrong value", -1, Util.getFragmentNumber("org.test"));
	}

	/*
	 * Test org.eclipse.pde.api.tools.internal.util.Util.isGreatherVersion(String, String)
	 */
	public void testIsGreatherVersion() {
		assertEquals("wrong value", 1, 1);
	}
	
	public void testSinceTagVersion() {
		try {
			new SinceTagVersion(null);
			assertTrue("Should not reach there", false);
		} catch (IllegalArgumentException e) {
			// expected exception
		}

		SinceTagVersion sinceTagVersion = new SinceTagVersion(" org.eclipse.jdt.core 3.4. test plugin");
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", " org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " test plugin", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.");
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.qualifier");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.qualifier postfix");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0");
		assertEquals("wrong version string", "3.4.0", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0.");
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.");
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.");
		assertEquals("wrong version string", "3.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3");
		assertEquals("wrong version string", "3", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0.qualifier");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0.qualifier postfix");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0. postfix");
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0 postfix");
		assertEquals("wrong version string", "3.4.0", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4. postfix");
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4 postfix");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3. postfix");
		assertEquals("wrong version string", "3.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3 postfix");
		assertEquals("wrong version string", "3", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("prefix");
		assertNull("wrong version string", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", "prefix", sinceTagVersion.postfixString());
		
		sinceTagVersion = new SinceTagVersion("test 3.4 protected (was added in 2.1 as private class)");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "test ", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " protected (was added in 2.1 as private class)", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4 protected (was added in 2.1 as private class)");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " protected (was added in 2.1 as private class)", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("abc1.0");
		assertNull("Wrong version string", sinceTagVersion.getVersionString());
		assertNull("Wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("Wrong postfix string", "abc1.0", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4, was added in 3.1 as private method");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", ", was added in 3.1 as private method", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("abc1.0, was added in 3.1 as private method");
		assertEquals("wrong version string", "3.1", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "abc1.0, was added in ", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " as private method", sinceTagVersion.postfixString());
	}
	
	public void testRegexExcludeList() {
		String line = "R:org\\.eclipse\\.swt[a-zA-Z_0-9\\.]*";
		class LocalApiComponent implements IApiComponent {
			String symbolicName;

			public LocalApiComponent(String symbolicName) {
				this.symbolicName = symbolicName;
			}
			public String[] getPackageNames() throws CoreException {
				return null;
			}
			public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
				return null;
			}
			public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
				return null;
			}
			public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
			}
			public void close() throws CoreException {
			}
			public int getContainerType() {
				return 0;
			}
			public String getName() {
				return null;
			}
			public int getType() {
				return 0;
			}
			public IApiElement getParent() {
				return null;
			}
			public IApiElement getAncestor(int ancestorType) {
				return null;
			}
			public IApiComponent getApiComponent() {
				return null;
			}
			public String getSymbolicName() {
				return this.symbolicName;
			}
			public IApiDescription getApiDescription() throws CoreException {
				return null;
			}
			public boolean hasApiDescription() {
				return false;
			}
			public String getVersion() {
				return null;
			}
			public String[] getExecutionEnvironments() throws CoreException {
				return null;
			}
			public IApiTypeContainer[] getApiTypeContainers() throws CoreException {
				return null;
			}
			public IApiTypeContainer[] getApiTypeContainers(String id) throws CoreException {
				return null;
			}
			public IRequiredComponentDescription[] getRequiredComponents() throws CoreException {
				return null;
			}
			public String getLocation() {
				return null;
			}
			public boolean isSystemComponent() {
				return false;
			}
			public boolean isSourceComponent() throws CoreException {
				return false;
			}
			public void dispose() {
			}
			public IApiBaseline getBaseline() throws CoreException {
				return null;
			}
			public IApiFilterStore getFilterStore() throws CoreException {
				return null;
			}
			public boolean isFragment() throws CoreException {
				return false;
			}
			public IApiComponent getHost() throws CoreException {
				return null;
			}
			public boolean hasFragments() throws CoreException {
				return false;
			}
			public String[] getLowestEEs() throws CoreException {
				return null;
			}
			public ResolverError[] getErrors() throws CoreException {
				return null;
			}
			public IElementDescriptor getHandle() {
				return null;
			}
			public IReferenceCollection getExternalDependencies() {
				return null;
			}
		}
		List<IApiComponent> allComponents = new ArrayList<IApiComponent>();
		String[] componentNames = new String[] {
				"org.eclipse.swt",
				"org.eclipse.equinox.simpleconfigurator.manipulator",
				"org.eclipse.team.ui",
				"org.eclipse.ecf",
				"org.eclipse.core.commands",
				"org.eclipse.equinox.util",
				"org.eclipse.equinox.p2.jarprocessor",
				"org.eclipse.equinox.security",
				"org.eclipse.sdk",
				"org.eclipse.help.ui",
				"org.eclipse.jdt.doc.isv",
				"org.eclipse.equinox.p2.core",
				"org.eclipse.debug.ui",
				"org.eclipse.ui.navigator",
				"org.eclipse.update.core",
				"javax.servlet.jsp",
				"org.eclipse.ui.workbench",
				"org.eclipse.equinox.event",
				"org.eclipse.jdt.core",
				"JavaSE-1.7",
				"org.apache.commons.codec",
				"org.apache.commons.logging",
				"org.objectweb.asm",
				"org.eclipse.core.filebuffers",
				"org.eclipse.jsch.ui",
				"org.eclipse.platform",
				"org.eclipse.pde.ua.core",
				"org.eclipse.help",
				"org.eclipse.ecf.provider.filetransfer",
				"org.eclipse.equinox.preferences",
				"org.eclipse.equinox.p2.reconciler.dropins",
				"org.eclipse.team.cvs.ui",
				"org.eclipse.equinox.p2.metadata.generator",
				"org.eclipse.equinox.registry",
				"org.eclipse.update.ui",
				"org.eclipse.swt",
				"org.eclipse.ui.console",
				"org.junit4",
				"org.eclipse.ui.views.log",
				"org.eclipse.equinox.p2.touchpoint.natives",
				"org.eclipse.equinox.ds",
				"org.eclipse.help.base",
				"org.eclipse.equinox.frameworkadmin.equinox",
				"org.eclipse.jdt",
				"org.eclipse.osgi.util",
				"org.sat4j.pb",
				"org.hamcrest.core",
				"org.eclipse.jdt.junit4.runtime",
				"org.eclipse.equinox.p2.artifact.repository",
				"org.eclipse.core.databinding.property",
				"org.eclipse.core.databinding",
				"org.eclipse.equinox.concurrent",
				"org.eclipse.pde.ua.ui",
				"org.eclipse.ui.navigator.resources",
				"org.eclipse.equinox.http.servlet",
				"org.eclipse.equinox.p2.ql",
				"org.eclipse.jsch.core",
				"javax.servlet",
				"org.eclipse.jface",
				"org.eclipse.equinox.p2.updatesite",
				"org.eclipse.jface.databinding",
				"org.eclipse.ui.browser",
				"org.eclipse.ui",
				"org.eclipse.core.databinding.beans",
				"org.eclipse.search",
				"org.eclipse.equinox.jsp.jasper.registry",
				"org.eclipse.jdt.debug",
				"org.eclipse.ecf.provider.filetransfer.ssl",
				"org.eclipse.platform.doc.isv",
				"org.eclipse.update.core.win32",
				"org.eclipse.pde.api.tools",
				"org.eclipse.ui.ide.application",
				"org.eclipse.equinox.p2.metadata",
				"org.eclipse.equinox.security.win32.x86",
				"org.eclipse.core.contenttype",
				"org.eclipse.equinox.p2.ui.sdk",
				"org.eclipse.core.resources",
				"org.eclipse.pde.launching",
				"org.eclipse.ui.externaltools",
				"org.eclipse.cvs",
				"org.eclipse.equinox.p2.repository",
				"org.eclipse.core.resources.win32.x86",
				"org.eclipse.pde.ui",
				"org.eclipse.core.databinding.observable",
				"org.eclipse.pde.doc.user",
				"org.eclipse.ui.editors",
				"org.eclipse.jdt.compiler.tool",
				"org.eclipse.jdt.apt.ui",
				"org.eclipse.rcp",
				"org.eclipse.ui.presentations.r21",
				"org.eclipse.pde.runtime",
				"org.eclipse.equinox.security.ui",
				"org.eclipse.core.jobs",
				"org.eclipse.update.configurator",
				"org.eclipse.equinox.http.jetty",
				"org.eclipse.pde.ds.ui",
				"org.apache.lucene.analysis",
				"org.eclipse.ui.views",
				"org.eclipse.equinox.common",
				"org.apache.lucene",
				"org.eclipse.ecf.identity",
				"org.eclipse.ui.workbench.texteditor",
				"org.eclipse.equinox.p2.ui",
				"org.eclipse.core.runtime.compatibility.auth",
				"org.eclipse.ltk.core.refactoring",
				"org.eclipse.ant.core",
				"org.eclipse.ant.launching",
				"com.jcraft.jsch",
				"org.eclipse.ui.win32",
				"org.eclipse.pde.core",
				"org.eclipse.pde.build",
				"org.eclipse.core.runtime.compatibility.registry",
				"org.eclipse.ui.workbench.compatibility",
				"org.eclipse.ltk.ui.refactoring",
				"org.eclipse.jface.text",
				"org.apache.commons.el",
				"org.eclipse.compare.win32",
				"org.eclipse.core.runtime",
				"org.eclipse.jdt.ui",
				"org.eclipse.compare",
				"org.eclipse.ui.forms",
				"org.eclipse.equinox.p2.extensionlocation",
				"org.mortbay.jetty.util",
				"org.eclipse.equinox.p2.director",
				"org.eclipse.core.filesystem",
				"org.eclipse.jdt.junit.core",
				"org.eclipse.jdt.junit.runtime",
				"org.eclipse.team.cvs.ssh2",
				"org.eclipse.core.variables",
				"org.eclipse.platform.doc.user",
				"org.eclipse.equinox.p2.operations",
				"org.eclipse.core.externaltools",
				"org.eclipse.equinox.simpleconfigurator",
				"org.eclipse.equinox.p2.touchpoint.eclipse",
				"org.eclipse.equinox.p2.metadata.repository",
				"org.eclipse.pde.ds.core",
				"org.eclipse.jdt.apt.pluggable.core",
				"org.eclipse.team.cvs.core",
				"org.mortbay.jetty.server",
				"org.eclipse.text",
				"org.eclipse.jdt.compiler.apt",
				"org.eclipse.equinox.p2.director.app",
				"org.eclipse.jdt.debug.ui",
				"org.eclipse.equinox.p2.repository.tools",
				"org.apache.commons.httpclient",
				"org.eclipse.equinox.p2.garbagecollector",
				"org.eclipse.ui.ide",
				"org.eclipse.equinox.p2.engine",
				"org.apache.ant",
				"org.eclipse.jdt.junit",
				"org.eclipse.ecf.filetransfer",
				"org.eclipse.core.filesystem.win32.x86",
				"org.eclipse.core.net",
				"org.eclipse.equinox.jsp.jasper",
				"org.eclipse.equinox.p2.directorywatcher",
				"org.eclipse.equinox.http.registry",
				"org.junit",
				"org.eclipse.pde.junit.runtime",
				"org.eclipse.equinox.launcher",
				"org.eclipse.jdt.launching",
				"org.eclipse.core.expressions",
				"org.eclipse.ui.intro",
				"org.eclipse.team.core",
				"org.eclipse.ui.intro.universal",
				"org.eclipse.swt.win32.win32.x86",
				"org.eclipse.osgi.services",
				"org.eclipse.pde",
				"org.eclipse.ui.views.properties.tabbed",
				"org.eclipse.core.runtime.compatibility",
				"org.eclipse.ant.ui",
				"org.eclipse.ecf.provider.filetransfer.httpclient.ssl",
				"org.eclipse.equinox.launcher.win32.win32.x86",
				"org.eclipse.core.boot",
				"org.apache.jasper",
				"org.eclipse.help.webapp",
				"org.sat4j.core",
				"org.eclipse.pde.api.tools.ui",
				"org.eclipse.equinox.p2.ui.sdk.scheduler",
				"org.eclipse.debug.core",
				"org.eclipse.jdt.core.manipulation",
				"org.eclipse.osgi",
				"org.eclipse.update.scheduler",
				"org.eclipse.equinox.p2.updatechecker",
				"org.eclipse.equinox.p2.console",
				"org.eclipse.equinox.frameworkadmin",
				"org.eclipse.compare.core",
				"org.eclipse.jdt.apt.core",
				"org.eclipse.help.appserver",
				"org.eclipse.pde.ui.templates",
				"org.eclipse.ecf.ssl",
				"org.eclipse.ui.cheatsheets",
				"com.ibm.icu",
				"org.eclipse.core.net.win32.x86",
				"org.eclipse.jdt.doc.user",
				"org.eclipse.equinox.app",
				"org.eclipse.ui.net",
				"org.eclipse.equinox.p2.publisher",
				"org.eclipse.ecf.provider.filetransfer.httpclient",
		};
		for (int i = 0, max = componentNames.length; i < max; i++) {
			allComponents.add(new LocalApiComponent(componentNames[i]));
		}
		IApiComponent[] components = new IApiComponent[allComponents.size()];
		allComponents.toArray(components);
		FilteredElements excludedElements = new FilteredElements();
		try {
			Util.collectRegexIds(line, excludedElements, components, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals("Wrong size", 2, excludedElements.getPartialMatches().size());
		assertFalse("Wrong result", excludedElements.containsPartialMatch("org.eclipse.jdt.core"));
		assertFalse("Wrong result", excludedElements.containsExactMatch("org.eclipse.jdt.core"));
	}
	public void testRegexExcludeList2() {
		FilteredElements excludedElements = new FilteredElements();
		assertEquals("Wrong size", 0, excludedElements.getPartialMatches().size());
		assertEquals("Wrong size", 0, excludedElements.getExactMatches().size());
		assertFalse("Wrong result", excludedElements.containsPartialMatch("org.eclipse.jdt.core"));
		assertFalse("Wrong result", excludedElements.containsExactMatch("org.eclipse.jdt.core"));
	}
	
	public void testPluginXmlDecoding() {
		InputStream stream = UtilTests.class.getResourceAsStream("plugin.xml.zip");
		ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(stream));
		String s = null;
		try {
			ZipEntry zEntry;
			while ((zEntry = inputStream.getNextEntry()) != null) {
				// if it is empty directory, continue
				if (zEntry.isDirectory() || !zEntry.getName().endsWith(".xml")) {
					continue;
				}
				s = new String(Util.getInputStreamAsCharArray(inputStream, -1, IApiCoreConstants.UTF_8));
			}
		} catch (IOException e) {
			// ignore
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException ioe) {
				// ignore
			}
		}
		assertNotNull("Should not be null", s);
		try {
			Util.parseDocument(s);
		} catch(CoreException ce) {
			assertTrue("Should not happen", false);
		}
	}
}
