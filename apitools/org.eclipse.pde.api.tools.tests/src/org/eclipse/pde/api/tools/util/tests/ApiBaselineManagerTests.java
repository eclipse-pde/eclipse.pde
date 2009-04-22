/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundleModel;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

/**
 * Tests the {@link ApiBaselineManager} without the framework running
 */
public class ApiBaselineManagerTests extends AbstractApiTest {

	class SourceChangeVisitor extends ASTVisitor {
		String name = null;
		String signature = null;
		String tagname = null;
		ASTRewrite rewrite = null;
		boolean remove = false;
		
		public SourceChangeVisitor(String name, String signature, String tagname, boolean remove, ASTRewrite rewrite) {
			this.name = name;
			this.signature = signature;
			this.tagname = tagname;
			this.rewrite = rewrite;
			this.remove = remove;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.FieldDeclaration)
		 */
		public boolean visit(FieldDeclaration node) {
			if(signature != null) {
				return false;
			}
			List<VariableDeclarationFragment> fields = node.fragments();
			VariableDeclarationFragment fragment = null;
			for(Iterator<VariableDeclarationFragment> iter = fields.iterator(); iter.hasNext();) {
				fragment = iter.next();
				if(fragment.getName().getFullyQualifiedName().equals(name)) {
					break;
				}
			}
			if(fragment != null) {
				updateTag(node);
			}
			return false;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodDeclaration)
		 */
		public boolean visit(MethodDeclaration node) {
			if(name.equals(node.getName().getFullyQualifiedName())) {
				if(signature.equals(Signatures.getMethodSignatureFromNode(node))) {
					updateTag(node);
				}
			}
			return false;
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeDeclaration)
		 */
		public boolean visit(TypeDeclaration node) {
			if(name.equals(node.getName().getFullyQualifiedName())) {
				updateTag(node);
				return false;
			}
			return true;
		}
		/**
		 * Updates a javadoc tag, by either adding a new one or removing
		 * an existing one
		 * @param body
		 */
		private void updateTag(BodyDeclaration body) {
			Javadoc docnode = body.getJavadoc();
			AST ast = body.getAST();
			if(docnode == null) {
				docnode = ast.newJavadoc();
				rewrite.set(body, body.getJavadocProperty(), docnode, null);
			}
			ListRewrite lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
			if(remove) {
				List<TagElement> tags = (List<TagElement>) docnode.getStructuralProperty(Javadoc.TAGS_PROPERTY);
				if(tags != null) {
					TagElement tag = null;
					for(int i = 0 ; i < tags.size(); i++) {
						tag = tags.get(i);
						if(tagname.equals(tag.getTagName())) {
							lrewrite.remove(tag, null);
						}
					}
				}
			}
			else {
				TagElement newtag = ast.newTagElement();
				newtag.setTagName(tagname);
				lrewrite.insertLast(newtag, null);
			}
		}
	}
	
	private IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-source").append("a").append("b").append("c");
	private IPath PLUGIN_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-plugins");
	private IApiBaselineManager fPMmanager = ApiPlugin.getDefault().getApiBaselineManager();
	private final String TESTING_PACKAGE = "a.b.c";
	
	/**
	 * @return the {@link IApiDescription} for the testing project
	 */
	private IApiDescription getTestProjectApiDescription()  throws CoreException {
		IApiBaseline baseline = getWorkspaceBaseline();
		assertNotNull("the workspace baseline must exist", baseline);
		IApiComponent component = baseline.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		if(component != null) {
			return component.getApiDescription();
		}
		return null;
	}
	
	/**
	 * Creates and returns a test baseline with the given id. Also adds it to the baseline manager
	 * 
	 * @param id
	 * @return
	 */
	protected IApiBaseline getTestBaseline(String id) {
		IApiBaseline baseline = null;
		baseline = ApiModelFactory.newApiBaseline(id);
		fPMmanager.addApiBaseline(baseline);
		return baseline;
	}
	
	/**
	 * Tests trying to get the workspace baseline without the framework running 
	 */
	public void testGetWorkspaceComponent() {
		IApiBaseline baseline = getWorkspaceBaseline();
		assertTrue("the workspace baseline must not be null", baseline != null);
	}
	
	/**
	 * Tests that an api baseline can be added and retrieved successfully 
	 */
	public void testAddBaseline() {
		IApiBaseline baseline = getTestBaseline("addtest");
		assertTrue("the test baseline must have been created", baseline != null);
		baseline = fPMmanager.getApiBaseline("addtest");
		assertTrue("the testadd baseline must be in the manager", baseline != null);
	}
	
	/**
	 * Tests that an api baseline can be added/removed successfully
	 */
	public void testRemoveBaseline() {
		IApiBaseline baseline = getTestBaseline("removetest");
		assertTrue("the testremove baseline must exist", baseline != null);
		baseline = fPMmanager.getApiBaseline("removetest");
		assertTrue("the testremove baseline must be in the manager", baseline != null);
		assertTrue("the testremove baseline should have been removed", fPMmanager.removeApiBaseline("removetest"));
	}
	
	/**
	 * Tests that the default baseline can be set/retrieved
	 */
	public void testSetDefaultBaseline() {
		IApiBaseline baseline = getTestBaseline("testdefault");
		assertTrue("the testdefault baseline must exist", baseline != null);
		fPMmanager.setDefaultApiBaseline("testdefault");
		baseline = fPMmanager.getDefaultApiBaseline();
		assertTrue("the default baseline must be the testdefault baseline", baseline != null);
	}
	
	/**
	 * Tests that all baselines added to the manager can be retrieved
	 */
	public void testGetAllBaselines() {
		getTestBaseline("three");
		IApiBaseline[] baselines = fPMmanager.getApiBaselines();
		assertTrue("there should be three baselines", baselines.length == 3);
	}
	
	/**
	 * Tests that all of the baselines have been removed
	 */
	public void testCleanUfPMmanager() {
		assertTrue("the testadd baseline should have been removed", fPMmanager.removeApiBaseline("addtest"));
		assertTrue("the testdefault baseline should have been removed", fPMmanager.removeApiBaseline("testdefault"));
		assertTrue("the three baseline should have been removed", fPMmanager.removeApiBaseline("three"));
	}
	
	/**
	 * Adds the given source to the given package in the given fragment root
	 * @param root the root to add the source to
	 * @param packagename the name of the package e.g. a.b.c
	 * @param sourcename the name of the source file without an extension e.g. TestClass1
	 */
	public void assertTestSource(IPackageFragmentRoot root, String packagename, String sourcename) {
		try {
			IPackageFragment fragment = root.getPackageFragment(packagename);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(sourcename+".java", IJavaElementDelta.ADDED, 0, IJavaElement.COMPILATION_UNIT);
			FileUtils.importFileFromDirectory(SRC_LOC.append(sourcename+".java").toFile(), fragment.getPath(), new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the added event for the compilation unit was not received", obj);
		} catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Adds the package with the given name to the given package fragment root
	 * @param the project to add the package to
	 * @param srcroot the absolute path to the package fragment root to add the new package to
	 * @param packagename the name of the new package
	 * @return the new {@link IPackageFragment} or <code>null</code>
	 */
	public IPackageFragment assertTestPackage(IJavaProject project, IPath srcroot, String packagename) {
		IPackageFragment fragment = null;
		try {
			IPackageFragmentRoot root = project.findPackageFragmentRoot(srcroot);
			assertNotNull("the 'src' package fragment root must exist", root);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(packagename, IJavaElementDelta.ADDED, 0, IJavaElement.PACKAGE_FRAGMENT);
			fragment = root.createPackageFragment(packagename, true, new NullProgressMonitor());
			assertNotNull("the new package '"+packagename+"' should have been created", fragment);
			Object obj = waiter.waitForEvent();
			assertNotNull("the added event for the package fragment "+packagename+" was not received", obj);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
		return fragment;
	}
	
	/**
	 * Adds a test library with the given name to the test projects' class path. The library is imported from 
	 * the {@link #PLUGIN_LOC} location.
	 * @param project the project to add the library classpath entry to
	 * @param folderpath the path in the project where the library should be imported to
	 * @param libname the name of the library
	 */
	public IFolder assertTestLibrary(IJavaProject project, IPath folderpath, String libname) {
		IFolder folder = null;
		try {
			//add to project
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(libname, IJavaElementDelta.CHANGED, IJavaElementDelta.F_ADDED_TO_CLASSPATH, IJavaElement.PACKAGE_FRAGMENT_ROOT);
			folder = project.getProject().getFolder(folderpath);
			if(!folder.exists()) {
				folder.create(false, true, null);
			}
			FileUtils.importFileFromDirectory(PLUGIN_LOC.append(libname).toFile(), folder.getFullPath(), null);
			IPath libPath = folder.getFullPath().append(libname);
			IClasspathEntry entry = JavaCore.newLibraryEntry(libPath, null, null);
			ProjectUtils.addToClasspath(project, entry);
			Object obj = waiter.waitForEvent();
			assertNotNull("the event for class path addition of "+libname+" not received", obj);
			// add to manifest bundle classpath
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			assertNotNull("the plugin model for the testing project must exist", model);
			IFile file = (IFile) model.getUnderlyingResource();
			assertNotNull("the underlying model file must exist", file);
			WorkspaceBundleModel manifest = new WorkspaceBundleModel(file);
			manifest.getBundle().setHeader(Constants.BUNDLE_CLASSPATH, ".," + libPath.removeFirstSegments(1).toString());
			PluginModelEventWaiter waiter2 = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			manifest.save();
			Object object = waiter2.waitForEvent();
			assertNotNull("the event for manifest modification was not received", object);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
		return folder;
	}
	
	/**
	 * Asserts if the given restriction is on the specified source
	 * @param packagename
	 * @param sourcename
	 */
	public void assertSourceResctriction(String packagename, String sourcename, int restriction) {
		try {
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor(packagename+"."+sourcename));
			assertNotNull("the annotations for "+packagename+"."+sourcename+" cannot be null", annot);
			assertTrue("there must be a noinstantiate setting for TestClass1", annot.getRestrictions() == restriction);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that closing an API aware project causes the workspace description to be updated
	 */
	public void testWPUpdateProjectClosed() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			assertNotNull("the workspace baseline must not be null", getWorkspaceBaseline());
			IApiComponent component  = getWorkspaceBaseline().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNotNull("the change project api component must exist in the workspace baseline", component);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(TESTING_PLUGIN_PROJECT_NAME, IJavaElementDelta.CHANGED, IJavaElementDelta.F_CLOSED, IJavaElement.JAVA_PROJECT);
			project.getProject().close(new NullProgressMonitor());
			//might need a waiter to ensure the model changed event has been processed
			Object obj = waiter.waitForEvent();
			assertNotNull("the closed event was not received", obj);
			component = getWorkspaceBaseline().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNull("the test project api component should no longer exist in the workspace baseline", component);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that opening an API aware project causes the workspace description to be updated
	 */
	public void testWPUpdateProjectOpen() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			if(project.getProject().isAccessible()) {
				project.getProject().close(new NullProgressMonitor());
			}
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(TESTING_PLUGIN_PROJECT_NAME, IJavaElementDelta.CHANGED, IJavaElementDelta.F_OPENED, IJavaElement.JAVA_PROJECT);
			project.getProject().open(new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the opened event was not received", obj);
			IApiBaseline baseline = getWorkspaceBaseline();
			assertNotNull("the workspace baseline must not be null", baseline);
			IApiComponent component = baseline.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNotNull("the test project api component must exist in the workspace baseline", component);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that adding a source file to an API aware project causes the workspace description
	 * to be updated
	 * This test adds <code>a.b.c.TestClass1</code> to the plug-in project
	 */
	public void testWPUpdateSourceAdded() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute().makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestClass1");
			assertSourceResctriction(TESTING_PACKAGE, "TestClass1", RestrictionModifiers.NO_INSTANTIATE);
		} catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that removing a source file from an API aware project causes the workspace description
	 * to be updated
	 */
	public void testWPUpdateSourceRemoved() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestClass1");
			IJavaElement element = project.findElement(new Path("a/b/c/TestClass1.java"));
			assertNotNull("the class a.b.c.TestClass1 must exist in the project", element);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass1.java", IJavaElementDelta.REMOVED, 0, IJavaElement.COMPILATION_UNIT);
			element.getResource().delete(true, new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the removed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass1"));
			assertNull("the annotations for a.b.c.TestClass1 should no longer be present", annot);
		} catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Adds the specified tag to the source member defined by the member name and signature
	 * @param unit
	 * @param membername
	 * @param signature
	 * @param tagname
	 * @param remove
	 * @throws CoreException
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 */
	private void updateTagInSource(ICompilationUnit unit, String membername, String signature, String tagname, boolean remove) throws CoreException, MalformedTreeException, BadLocationException {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(unit);
		CompilationUnit cunit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
		assertNotNull("the ast compilation unit cannot be null", cunit);
		cunit.recordModifications();
		ASTRewrite rewrite = ASTRewrite.create(cunit.getAST());
		cunit.accept(new SourceChangeVisitor(membername, signature, tagname, remove, rewrite));
		ITextFileBufferManager bm = FileBuffers.getTextFileBufferManager(); 
		IPath path = cunit.getJavaElement().getPath(); 
		try {
			bm.connect(path, LocationKind.IFILE, null);
			ITextFileBuffer tfb = bm.getTextFileBuffer(path, LocationKind.IFILE);
			IDocument document = tfb.getDocument(); 
			TextEdit edits = rewrite.rewriteAST(document, null);
			edits.apply(document);
			tfb.commit(new NullProgressMonitor(), true);
		} finally {
			bm.disconnect(path, LocationKind.IFILE, null);
		}
		waitForAutoBuild();
	}
	
	/**
	 * Tests that making Javadoc changes to the source file TestClass2 cause the workspace baseline to be 
	 * updated. 
	 * 
	 * This test adds a @noinstantiate tag to the source file TestClass2
	 */
	public void testWPUpdateSourceTypeChanged() {
		try {		
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			NullProgressMonitor monitor = new NullProgressMonitor();
			IPackageFragment fragment = root.getPackageFragment("a.b.c");
			FileUtils.importFileFromDirectory(SRC_LOC.append("TestClass2.java").toFile(), fragment.getPath(), monitor);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass2.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass2.java"));
			assertNotNull("TestClass2 must exist in the test project", element);
			updateTagInSource(element, "TestClass2", null, "@noinstantiate", false);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass2"));
			assertNotNull("the annotations for a.b.c.TestClass2 cannot be null", annot);
			assertTrue("there must be a noinstantiate setting for TestClass2", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) != 0);
			assertTrue("there must be a noextend setting for TestClass2", (annot.getRestrictions() & RestrictionModifiers.NO_EXTEND) != 0);
		}
		catch(Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that tags updated on an inner type are updated in the workspace description.
	 * 
	 * This test adds a @noinstantiate tag to an inner class in TestClass3
	 */
	public void testWPUpdateSourceInnerTypeChanged() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestClass3");
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass3.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass3.java"));
			assertNotNull("TestClass3 must exist in the test project", element);
			updateTagInSource(element, "InnerTestClass3", null, "@noinstantiate", false);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3"));
			assertNotNull("the annotations for a.b.c.TestClass3$InnerTestClass3 cannot be null", annot);
			assertTrue("there must be a noinstantiate setting for TestClass3$InnerTestClass3", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) != 0);
			assertTrue("there must be a noextend setting for TestClass3$InnerTestClass3", (annot.getRestrictions() & RestrictionModifiers.NO_EXTEND) != 0);
		}
		catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that changing the javadoc for a method updates the workspace baseline
	 * 
	 * This test adds a @noextend tag to the method foo() in TestClass1
	 */
	public void testWPUpdateSourceMethodChanged() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestClass1");
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass1.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass1.java"));
			assertNotNull("TestClass1 must exist in the test project", element);
			updateTagInSource(element, "foo", "()V", "@nooverride", false);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V"));
			assertNotNull("the annotations for foo() cannot be null", annot);
			assertTrue("there must be a nooverride setting for foo()", (annot.getRestrictions() & RestrictionModifiers.NO_OVERRIDE) != 0);
		}
		catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that changing the javadoc for a field updates the workspace baseline
	 * 
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	public void testWPUpdateSourceFieldChanged() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestField9");
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestField9.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestField9.java"));
			assertNotNull("TestField9 must exist in the test project", element);
			updateTagInSource(element, "field", null, "@noreference", false);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField9", "field"));
			assertNotNull("the annotations for 'field' cannot be null", annot);
			assertTrue("there must be a noreference setting for 'field'", (annot.getRestrictions() & RestrictionModifiers.NO_REFERENCE) != 0);
		}
		catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that removing a tag from a method updates the workspace baseline
	 * 
	 * This test removes a @noextend tag to the method foo() in TestClass1
	 */
	public void testWPUpdateSourceMethodRemoveTag() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestClass1");
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass1.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass1.java"));
			assertNotNull("TestClass1 must exist in the test project", element);
			updateTagInSource(element, "foo", "()V", "@nooverride", true);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V"));
			assertNotNull("the annotations for foo() cannot be null", annot);
			assertTrue("there must be no restrictions for foo()", annot.getRestrictions() == 0);
		}
		catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that removing a tag from a type updates the workspace baseline
	 * 
	 * This test removes a @noinstantiate tag to an inner class in TestClass3
	 */
	public void testWPUpdateSourceTypeRemoveTag() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestClass3");
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass3.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass3.java"));
			assertNotNull("TestClass3 must exist in the test project", element);
			updateTagInSource(element, "InnerTestClass3", null, "@noextend", true);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3"));
			assertNotNull("the annotations for 'InnerTestClass3' cannot be null", annot);
			assertTrue("there must be a no restrictions for 'InnerTestClass3'", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) == 0);
		}
		catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that removing a tag from a field updates the workspace baseline
	 * 
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	public void testWPUpdateSourceFieldRemoveTag() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
			assertNotNull("the 'src' package fragment root must exist", root);
			assertTestSource(root, TESTING_PACKAGE, "TestField9");
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestField9.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestField9.java"));
			assertNotNull("TestField9 must exist in the test project", element);
			updateTagInSource(element, "field1", null, "@noreference", true);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField9", "field"));
			assertNotNull("the annotations for 'field' cannot be null", annot);
			assertTrue("there must be a no restrictions for 'field'", annot.getRestrictions() == 0);
		}
		catch (Exception e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that a library added to the build and bundle class path of a project causes the 
	 * class file containers for the project to need to be recomputed
	 * @throws IOException 
	 * @throws InvocationTargetException 
	 * @throws CoreException 
	 */
	public void testWPUpdateLibraryAddedToClasspath() throws InvocationTargetException, IOException, CoreException {
		IFolder folder = null;
		IApiComponent component = null;
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getApiTypeContainers().length;
			
			// add to classpath
			folder = assertTestLibrary(project, new Path("libx"), "component.a_1.0.0.jar");
			assertNotNull("The new library path should not be null", folder);
			
			// re-retrieve updated component
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertTrue("there must be more containers after the addition", before < component.getApiTypeContainers().length);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
		finally {
			if(component != null) {
				component.dispose();
			}
			if(folder != null) {
				FileUtils.delete(folder);
			}
		}
	}
	
	/**
	 * Tests removing a library from the classpath of a project
	 */
	public void testWPUpdateLibraryRemovedFromClasspath() {
		IPath libPath = null;
		IApiComponent component = null;
		IPluginModelBase model = null;
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			
			//add to classpath
			IFolder folder = assertTestLibrary(project, new Path("libx"), "component.a_1.0.0.jar");
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getApiTypeContainers().length;
			libPath = folder.getFullPath().append("component.a_1.0.0.jar");
			
			//remove classpath entry
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("component.a_1.0.0.jar", IJavaElementDelta.CHANGED, IJavaElementDelta.F_REMOVED_FROM_CLASSPATH, IJavaElement.PACKAGE_FRAGMENT_ROOT);
			ProjectUtils.removeFromClasspath(project, JavaCore.newLibraryEntry(libPath, null, null));
			Object obj = waiter.waitForEvent();
			assertNotNull("the added event for the package fragment was not received", obj);
			
			// remove from bundle class path
			model = PluginRegistry.findModel(project.getProject());
			assertNotNull("the plugin model for the testing project must exist", model);
			IFile file = (IFile) model.getUnderlyingResource();
			assertNotNull("the underlying model file must exist", file);
			WorkspaceBundleModel manifest = new WorkspaceBundleModel(file);
			manifest.getBundle().setHeader(Constants.BUNDLE_CLASSPATH, ".");
			PluginModelEventWaiter waiter2 = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			manifest.save();
			Object object = waiter2.waitForEvent();
			assertNotNull("the event for the manifest modification was not received", object);
			
			// retrieve updated component
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertTrue("there must be less containers after the removal", before > component.getApiTypeContainers().length);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
		finally {
			if(component != null) {
				component.dispose();
			}
			if(model != null) {
				model.dispose();
			}
			if(libPath != null) {
				FileUtils.delete(libPath.toOSString());
			}
		}
	}
	
	/**
	 * Tests that changing the output folder settings for a project cause the class file containers 
	 * to be updated
	 */
	public void testWPUpdateDefaultOutputFolderChanged() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IContainer container = ProjectUtils.addFolderToProject(project.getProject(), "bin2");
			assertNotNull("the new output folder cannot be null", container);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(TESTING_PLUGIN_PROJECT_NAME, IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED | IJavaElementDelta.F_CLASSPATH_CHANGED, IJavaElement.JAVA_PROJECT);
			IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getApiTypeContainers().length;
			project.setOutputLocation(container.getFullPath(), new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the changed event for the project (classpath) was not received", obj);
			assertTrue("there must be the same number of containers after the change", before == component.getApiTypeContainers().length);
			assertTrue("the new output location should be 'bin2'", "bin2".equalsIgnoreCase(project.getOutputLocation().toFile().getName()));
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that the output folder settings for a source folder cause the class file containers to 
	 * be updated
	 */
	public void testWPUpdateOutputFolderSrcFolderChanged() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IContainer container = ProjectUtils.addFolderToProject(project.getProject(), "bin3");
			assertNotNull("the new output location cannot be null", container);
			IPackageFragmentRoot src2 = ProjectUtils.addSourceContainer(project, "src2");
			assertNotNull("the new source folder cannot be null", src2);
			assertNull("the default output location should be 'bin2' (implicit as null)", src2.getRawClasspathEntry().getOutputLocation());
			IClasspathEntry entry = JavaCore.newSourceEntry(src2.getPath(), new IPath[]{}, container.getFullPath());
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("src2", IJavaElementDelta.CHANGED, IJavaElementDelta.F_ADDED_TO_CLASSPATH | IJavaElementDelta.F_REMOVED_FROM_CLASSPATH, IJavaElement.PACKAGE_FRAGMENT_ROOT);
			IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getApiTypeContainers().length;
			ProjectUtils.addToClasspath(project, entry);
			Object obj = waiter.waitForEvent();
			assertNotNull("the changed event for the package fragment root (classpath) was not received", obj);
			// add to bundle class path
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			assertNotNull("the plugin model for the testing project must exist", model);
			IFile file = (IFile) model.getUnderlyingResource();
			assertNotNull("the underlying model file must exist", file);
			WorkspaceBundleModel manifest = new WorkspaceBundleModel(file);
			manifest.getBundle().setHeader(Constants.BUNDLE_CLASSPATH, ".,next.jar");
			PluginModelEventWaiter waiter2 = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			manifest.save();
			Object object = waiter2.waitForEvent();
			assertNotNull("the event for manifest modification was not received", object);
			// add to build.properties
			WorkspaceBuildModel prop = new WorkspaceBuildModel(project.getProject().getFile("build.properties"));
			IBuildEntry newEntry = prop.getFactory().createEntry("source.next.jar");
			newEntry.addToken("src2/");
			prop.getBuild().add(newEntry);
			PluginModelEventWaiter waiter3 = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			prop.save();
			Object object3 = waiter3.waitForEvent();
			assertNotNull("the event for biuld.properties modification was not received", object3);
			// retrieve updated component
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertTrue("there must be one more container after the change", before < component.getApiTypeContainers().length);
			assertTrue("the class file container for src2 must be 'bin3'", "bin3".equals(src2.getRawClasspathEntry().getOutputLocation().toFile().getName()));
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that adding a package does not update the workspace baseline
	 */
	public void testWPUpdatePackageAdded() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			
			//add the package
			assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "a.test1.c.d");
			
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("a.test1.c.d"));
			assertNotNull("the annotations for package "+TESTING_PACKAGE+" should exist", annot);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that removing a package updates the workspace baseline
	 * This test removes the a.b.c package being used in all tests thus far,
	 * and should be run last
	 */
	public void testWPUpdatePackageRemoved() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			
			//add the package
			IPath srcroot = new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute();
			IPackageFragment fragment = assertTestPackage(project, srcroot, "a.test2");
			assertNotNull("the package "+TESTING_PACKAGE+" must exist", fragment);
			
			//remove the package
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("a.test2", IJavaElementDelta.REMOVED, 0, IJavaElement.PACKAGE_FRAGMENT);
			fragment.delete(true, new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the removed event for the package fragment was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("a.test2"));
			assertNull("the annotations for package "+TESTING_PACKAGE+" should not exist", annot);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that an exported package addition in the PDE model is reflected in the workspace
	 * api baseline
	 */
	public void testWPUpdateExportPackageAdded() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			
			//add package
			assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1");
			
			//update
			setPackageToApi(project, "export1");
			IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
			assertNotNull("there must be an annotation for the new exported package", annot);
			assertTrue("the newly exported package must be API visibility", annot.getVisibility() == VisibilityModifiers.API);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that changing a directive to x-internal on an exported package causes the workspace 
	 * api baseline to be updated
	 */
	public void testWPUPdateExportPackageDirectiveChangedToInternal() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			
			//add package
			assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1");
			
			//update the model
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			assertNotNull("the plugin model for the testing project must exist", model);
			IFile file = (IFile) model.getUnderlyingResource();
			assertNotNull("the underlying model file must exist", file);
			WorkspaceBundleModel newmodel = new WorkspaceBundleModel(file);
			newmodel.getBundle().setHeader(Constants.EXPORT_PACKAGE, "export1;x-internal:=true");
			PluginModelEventWaiter waiter = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			newmodel.save();
			Object object = waiter.waitForEvent();
			assertNotNull("the changed event for the exported package change was not received", object);
			IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
			assertNotNull("there must be an annotation for the new exported package", annot);
			assertTrue("the changed exported package must be PRIVATE visibility", annot.getVisibility() == VisibilityModifiers.PRIVATE);
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that an exported package removal in the PDE model is reflected in the workspace
	 * api baseline
	 */
	public void testWPUpdateExportPackageRemoved() {
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			
			//add package
			assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1");
			
			setPackageToApi(project, "export1");
			IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
			assertNotNull("there must be an annotation for the new exported package", annot);
			assertTrue("the newly exported package must be API visibility", annot.getVisibility() == VisibilityModifiers.API);
			IPluginModelBase model = PluginRegistry.findModel(project.getProject());
			assertNotNull("the plugin model for the testing project must exist", model);
			IFile file = (IFile) model.getUnderlyingResource();
			assertNotNull("the underlying model file must exist", file);
			WorkspaceBundleModel newmodel = new WorkspaceBundleModel(file);
			newmodel.getBundle().setHeader(Constants.EXPORT_PACKAGE, null);
			PluginModelEventWaiter waiter = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			newmodel.save();
			Object object = waiter.waitForEvent();
			assertNotNull("the changed event for the exported package change was not received", object);
			annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
			assertNotNull("should still be an annotation for the package", annot);
			assertTrue("unexported package must be private", VisibilityModifiers.isPrivate(annot.getVisibility()));
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * sets the given package name to be an Exported-Package
	 * @param name
	 */
	private void setPackageToApi(IJavaProject project, String name) {
		IPluginModelBase model = PluginRegistry.findModel(project.getProject());
		assertNotNull("the plugin model for the testing project must exist", model);
		IFile file = (IFile) model.getUnderlyingResource();
		assertNotNull("the underlying model file must exist", file);
		WorkspaceBundleModel newmodel = new WorkspaceBundleModel(file);
		newmodel.getBundle().setHeader(Constants.EXPORT_PACKAGE, "export1");
		PluginModelEventWaiter waiter = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
		newmodel.save();
		Object object = waiter.waitForEvent();
		assertNotNull("the changed event for the exported package change was not received", object);
	}
	
	IJavaProject getTestingProject() {
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(TESTING_PLUGIN_PROJECT_NAME));
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		createProject(TESTING_PLUGIN_PROJECT_NAME, new String[] {TESTING_PACKAGE});
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		deleteProject(TESTING_PLUGIN_PROJECT_NAME);
	}
}
