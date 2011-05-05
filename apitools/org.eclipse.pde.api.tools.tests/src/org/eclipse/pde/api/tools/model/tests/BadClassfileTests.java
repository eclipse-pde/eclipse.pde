/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.model.Component;
import org.eclipse.pde.api.tools.internal.model.ApiBaseline;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.provisional.search.ApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;

/**
 * Tests that our framework properly handles bad class files.
 * I.e. class files that are not well-formed
 */
public class BadClassfileTests extends TestCase {

	IPath source = null;
	DirectoryApiTypeContainer container = null;
	String CLASSFILE = "nobytecodes";
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		if(source == null) {
			source = TestSuiteHelper.getPluginDirectoryPath().append("test-classes").append("bad");
			container = new DirectoryApiTypeContainer(null, source.toOSString());
		}
	}
	
	/**
	 * Writes any expected error pre-amble prior to the test running
	 * @param test
	 */
	void writePreamble(String test) {
		System.err.println("Expected 'java.lang.ArrayIndexOutOfBoundsException: 34' in "+test+" from ASM ClassReader");
		System.err.flush();
	}
	
	/**
	 * Tests trying to get the structure for a bad classfile
	 * 
	 * @throws Exception if something bad happens
	 */
	public void testClassfileScanner() throws Exception {
		writePreamble("testClassfileScanner()");
		IApiTypeRoot root = container.findTypeRoot(CLASSFILE);
		IApiType type = root.getStructure();
		assertNull("The type must be null", type);
	}
	
	/**
	 * Tests trying to search a bad class file
	 * 
	 * @throws Exception
	 */
	public void testSearchEngine() throws Exception {
		writePreamble("testSearchEngine()");
		final Component component = new Component(null) {
			public boolean isSystemComponent() {return false;}
			public boolean isSourceComponent() throws CoreException {return false;}
			public boolean isFragment() throws CoreException {return false;}
			public boolean hasFragments() throws CoreException {return false;}
			public boolean hasApiDescription() {return false;}
			public String getVersion() {return "1.0.0";}
			public IRequiredComponentDescription[] getRequiredComponents() throws CoreException {return null;}
			public String[] getLowestEEs() throws CoreException {return null;}
			public String getLocation() {return null;}
			public String getSymbolicName() {return "test";}
			public String[] getExecutionEnvironments() throws CoreException {return null;}
			public ResolverError[] getErrors() throws CoreException {return null;}
			protected List createApiTypeContainers() throws CoreException {
				ArrayList<IApiTypeContainer> containers = new ArrayList<IApiTypeContainer>();
				containers.add(BadClassfileTests.this.container);
				return containers;
			}
			public IApiBaseline getBaseline() {
				return new ApiBaseline("testbaseline");
			}
			protected IApiFilterStore createApiFilterStore() throws CoreException {return null;}
			protected IApiDescription createApiDescription() throws CoreException {return null;}
		};
		
		ApiSearchEngine engine = new ApiSearchEngine();
		IApiBaseline baseline = new ApiBaseline("testbaseline") {
			public IApiComponent[] getApiComponents() {
				return new IApiComponent[] {component};
			}
		};
		IApiSearchRequestor requestor = new IApiSearchRequestor() {
			public boolean includesInternal() {return true;}
			public boolean includesAPI() {return true;}
			public boolean includesIllegalUse() {return false;}
			public IApiScope getScope() {
				ApiScope scope = new ApiScope();
				scope.addElement(component);
				return scope;
			}
			
			public int getReferenceKinds() {return Reference.MASK_REF_ALL;}
			public boolean acceptReference(IReference reference) {return true;}
			public boolean acceptMember(IApiMember member) {return true;}
			public boolean acceptComponent(IApiComponent component) {return true;}
			public boolean acceptContainer(IApiTypeContainer container) {return false;}
		};
		IApiSearchReporter reporter = new IApiSearchReporter() {
			public void reportResults(IApiElement element, IReference[] references) {}
			public void reportNotSearched(IApiElement[] elements) {}
			public void reportMetadata(IMetadata data) {}
			public void reportCounts() {}
		};
		engine.search(baseline, requestor, reporter, null);
	}
	
	/**
	 * Tests that the {@link org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner}
	 * handles ad class files
	 */
	public void testTagScanner() throws Exception {
		writePreamble("testTagScanner()");
		CompilationUnit unit = new CompilationUnit(TestSuiteHelper.getPluginDirectoryPath().append("test-classes").append("bad").append("nobytecodes.java").toOSString());
		TagScanner scanner = TagScanner.newScanner();
		try {
			scanner.scan(unit, new ApiDescription("test"), this.container, null, null);
		}
		catch(CoreException ce) {
			assertTrue("the message should be about nobytecodes#method() not resolving", ce.getMessage().equals("Unable to resolve method signature: nobytecodes#void method()"));
		}
	}
}
