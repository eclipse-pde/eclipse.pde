/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

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
	public void testCleanUpMmanager() {
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
	public void assertTestSource(IPackageFragmentRoot root, String packagename, String sourcename) throws CoreException, InvocationTargetException, IOException {
		IPackageFragment fragment = root.getPackageFragment(packagename);
		FileUtils.importFileFromDirectory(SRC_LOC.append(sourcename+".java").toFile(), fragment.getPath(), new NullProgressMonitor());
	}
	
	/**
	 * Adds the package with the given name to the given package fragment root
	 * @param the project to add the package to
	 * @param srcroot the absolute path to the package fragment root to add the new package to
	 * @param packagename the name of the new package
	 * @return the new {@link IPackageFragment} or <code>null</code>
	 */
	public IPackageFragment assertTestPackage(IJavaProject project, IPath srcroot, String packagename) throws JavaModelException {
		IPackageFragment fragment = null;
		IPackageFragmentRoot root = project.findPackageFragmentRoot(srcroot);
		assertNotNull("the 'src' package fragment root must exist", root);
		fragment = root.createPackageFragment(packagename, true, new NullProgressMonitor());
		assertNotNull("the new package '"+packagename+"' should have been created", fragment);
		return fragment;
	}
	
	/**
	 * Adds a test library with the given name to the test projects' class path. The library is imported from 
	 * the {@link #PLUGIN_LOC} location.
	 * @param project the project to add the library classpath entry to
	 * @param folderpath the path in the project where the library should be imported to
	 * @param libname the name of the library
	 */
	public IFolder assertTestLibrary(IJavaProject project, IPath folderpath, String libname) throws CoreException, InvocationTargetException, IOException {
		IFolder folder = null;
		//import library
		folder = project.getProject().getFolder(folderpath);
		if(!folder.exists()) {
			folder.create(false, true, null);
		}
		FileUtils.importFileFromDirectory(PLUGIN_LOC.append(libname).toFile(), folder.getFullPath(), null);
		IPath libPath = folder.getFullPath().append(libname);
		
		// add to manifest bundle classpath
		ProjectUtils.addBundleClasspathEntry(project.getProject(), ProjectUtils.getBundleProjectService().newBundleClasspathEntry(null, null, libPath.removeFirstSegments(1)));
		waitForAutoBuild();
		return folder;
	}
	
	/**
	 * Asserts if the given restriction is on the specified source
	 * @param packagename
	 * @param sourcename
	 */
	public void assertSourceResctriction(String packagename, String sourcename, int restriction) throws CoreException {
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor(packagename+"."+sourcename));
		assertNotNull("the annotations for "+packagename+"."+sourcename+" cannot be null", annot);
		assertTrue("there must be a noinstantiate setting for TestClass1", annot.getRestrictions() == restriction);
	}
	
	/**
	 * Tests that closing an API aware project causes the workspace description to be updated
	 */
	public void testWPUpdateProjectClosed() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		assertNotNull("the workspace baseline must not be null", getWorkspaceBaseline());
		IApiComponent component  = getWorkspaceBaseline().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the change project api component must exist in the workspace baseline", component);
		project.getProject().close(new NullProgressMonitor());
		component = getWorkspaceBaseline().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNull("the test project api component should no longer exist in the workspace baseline", component);
	}
	
	/**
	 * Tests that opening an API aware project causes the workspace description to be updated
	 */
	public void testWPUpdateProjectOpen() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		if(project.getProject().isAccessible()) {
			project.getProject().close(new NullProgressMonitor());
		}
		project.getProject().open(new NullProgressMonitor());
		IApiBaseline baseline = getWorkspaceBaseline();
		assertNotNull("the workspace baseline must not be null", baseline);
		IApiComponent component = baseline.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the test project api component must exist in the workspace baseline", component);
	}
	
	/**
	 * Tests that adding a source file to an API aware project causes the workspace description
	 * to be updated
	 * This test adds <code>a.b.c.TestClass1</code> to the plug-in project
	 */
	public void testWPUpdateSourceAdded() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute().makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestClass1");
		assertSourceResctriction(TESTING_PACKAGE, "TestClass1", RestrictionModifiers.NO_INSTANTIATE);
	}
	
	/**
	 * Tests that removing a source file from an API aware project causes the workspace description
	 * to be updated
	 */
	public void testWPUpdateSourceRemoved() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestClass1");
		IJavaElement element = project.findElement(new Path("a/b/c/TestClass1.java"));
		assertNotNull("the class a.b.c.TestClass1 must exist in the project", element);
		element.getResource().delete(true, new NullProgressMonitor());
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass1"));
		assertNull("the annotations for a.b.c.TestClass1 should no longer be present", annot);
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
		ASTParser parser = ASTParser.newParser(AST.JLS4);
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
	}
	
	/**
	 * Tests that making Javadoc changes to the source file TestClass2 cause the workspace baseline to be 
	 * updated. 
	 * 
	 * This test adds a @noinstantiate tag to the source file TestClass2
	 */
	public void testWPUpdateSourceTypeChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		NullProgressMonitor monitor = new NullProgressMonitor();
		IPackageFragment fragment = root.getPackageFragment("a.b.c");
		FileUtils.importFileFromDirectory(SRC_LOC.append("TestClass2.java").toFile(), fragment.getPath(), monitor);
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass2.java"));
		assertNotNull("TestClass2 must exist in the test project", element);
		updateTagInSource(element, "TestClass2", null, "@noinstantiate", false);
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass2"));
		assertNotNull("the annotations for a.b.c.TestClass2 cannot be null", annot);
		assertTrue("there must be a noinstantiate setting for TestClass2", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) != 0);
		assertTrue("there must be a noextend setting for TestClass2", (annot.getRestrictions() & RestrictionModifiers.NO_EXTEND) != 0);
	}
	
	/**
	 * Tests that tags updated on an inner type are updated in the workspace description.
	 * 
	 * This test adds a @noinstantiate tag to an inner class in TestClass3
	 */
	public void testWPUpdateSourceInnerTypeChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestClass3");
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass3.java"));
		assertNotNull("TestClass3 must exist in the test project", element);
		updateTagInSource(element, "InnerTestClass3", null, "@noinstantiate", false);
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3"));
		assertNotNull("the annotations for a.b.c.TestClass3$InnerTestClass3 cannot be null", annot);
		assertTrue("there must be a noinstantiate setting for TestClass3$InnerTestClass3", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) != 0);
		assertTrue("there must be a noextend setting for TestClass3$InnerTestClass3", (annot.getRestrictions() & RestrictionModifiers.NO_EXTEND) != 0);
	}
	
	/**
	 * Tests that changing the javadoc for a method updates the workspace baseline
	 * 
	 * This test adds a @noextend tag to the method foo() in TestClass1
	 */
	public void testWPUpdateSourceMethodChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestClass1");
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass1.java"));
		assertNotNull("TestClass1 must exist in the test project", element);
		updateTagInSource(element, "foo", "()V", "@nooverride", false);
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V"));
		assertNotNull("the annotations for foo() cannot be null", annot);
		assertTrue("there must be a nooverride setting for foo()", (annot.getRestrictions() & RestrictionModifiers.NO_OVERRIDE) != 0);
	}
	
	/**
	 * Tests that changing the javadoc for a field updates the workspace baseline
	 * 
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	public void testWPUpdateSourceFieldChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestField9");
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestField9.java"));
		assertNotNull("TestField9 must exist in the test project", element);
		updateTagInSource(element, "field", null, "@noreference", false);
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField9", "field"));
		assertNotNull("the annotations for 'field' cannot be null", annot);
		assertTrue("there must be a noreference setting for 'field'", (annot.getRestrictions() & RestrictionModifiers.NO_REFERENCE) != 0);
	}
	
	/**
	 * Tests that removing a tag from a method updates the workspace baseline
	 * 
	 * This test removes a @noextend tag to the method foo() in TestClass1
	 */
	public void testWPUpdateSourceMethodRemoveTag() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestClass1");
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass1.java"));
		assertNotNull("TestClass1 must exist in the test project", element);
		updateTagInSource(element, "foo", "()V", "@nooverride", true);
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V"));
		assertNotNull("the annotations for foo() cannot be null", annot);
		assertTrue("there must be no restrictions for foo()", annot.getRestrictions() == 0);
	}
	
	/**
	 * Tests that removing a tag from a type updates the workspace baseline
	 * 
	 * This test removes a @noinstantiate tag to an inner class in TestClass3
	 */
	public void testWPUpdateSourceTypeRemoveTag() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestClass3");
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass3.java"));
		assertNotNull("TestClass3 must exist in the test project", element);
		updateTagInSource(element, "InnerTestClass3", null, "@noextend", true);
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3"));
		assertNotNull("the annotations for 'InnerTestClass3' cannot be null", annot);
		assertTrue("there must be a no restrictions for 'InnerTestClass3'", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) == 0);
	}
	
	/**
	 * Tests that removing a tag from a field updates the workspace baseline
	 * 
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	public void testWPUpdateSourceFieldRemoveTag() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root);
		assertTestSource(root, TESTING_PACKAGE, "TestField9");
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestField9.java"));
		assertNotNull("TestField9 must exist in the test project", element);
		updateTagInSource(element, "field1", null, "@noreference", true);
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField9", "field"));
		assertNotNull("the annotations for 'field' cannot be null", annot);
		assertTrue("there must be a no restrictions for 'field'", annot.getRestrictions() == 0);
	}
	
	/**
	 * Tests that a library added to the build and bundle class path of a project causes the 
	 * class file containers for the project to need to be recomputed
	 * @throws IOException 
	 * @throws InvocationTargetException 
	 * @throws CoreException 
	 */
	public void testWPUpdateLibraryAddedToClasspath() throws Exception  {
		IFolder folder = null;
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getApiTypeContainers().length;
			
			// add to classpath
			folder = assertTestLibrary(project, new Path("libx"), "component.a_1.0.0.jar");
			assertNotNull("The new library path should not be null", folder);
			
			// re-retrieve updated component
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertTrue("there must be more containers after the addition", before < component.getApiTypeContainers().length);
		} finally {
			if(folder != null) {
				FileUtils.delete(folder);
			}
		}
	}
	
	/**
	 * Tests removing a library from the classpath of a project
	 */
	public void testWPUpdateLibraryRemovedFromClasspath() throws Exception {
		IPath libPath = null;
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project);
			
			//add to classpath
			IFolder folder = assertTestLibrary(project, new Path("libx"), "component.a_1.0.0.jar");
			IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getApiTypeContainers().length;
			libPath = folder.getFullPath().append("component.a_1.0.0.jar");
			
			//remove classpath entry
			ProjectUtils.removeFromClasspath(project, JavaCore.newLibraryEntry(libPath, null, null));
			waitForAutoBuild();
			
			// remove from bundle class path
			IBundleProjectService service = ProjectUtils.getBundleProjectService();
			IBundleProjectDescription description = service.getDescription(project.getProject());
			description.setBundleClassath(new IBundleClasspathEntry[]{service.newBundleClasspathEntry(new Path(ProjectUtils.SRC_FOLDER), null, null)});
			description.apply(null);
			waitForAutoBuild();
						
			// retrieve updated component
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertTrue("there must be less containers after the removal", before > component.getApiTypeContainers().length);
		}
		finally {
			if(libPath != null) {
				FileUtils.delete(libPath.toOSString());
			}
		}
	}
	
	/**
	 * Tests that changing the output folder settings for a project cause the class file containers 
	 * to be updated
	 */
	public void testWPUpdateDefaultOutputFolderChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		IContainer container = ProjectUtils.addFolderToProject(project.getProject(), "bin2");
		assertNotNull("the new output folder cannot be null", container);
		IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
		assertNotNull("the workspace component must exist", component);
		int before  = component.getApiTypeContainers().length;
		project.setOutputLocation(container.getFullPath(), new NullProgressMonitor());
		waitForAutoBuild();
		assertTrue("there must be the same number of containers after the change", before == component.getApiTypeContainers().length);
		assertTrue("the new output location should be 'bin2'", "bin2".equalsIgnoreCase(project.getOutputLocation().toFile().getName()));
	}
	
	/**
	 * Tests that the output folder settings for a source folder cause the class file containers to 
	 * be updated
	 */
	public void testWPUpdateOutputFolderSrcFolderChanged() throws Exception {
		IJavaProject project = getTestingProject();
		IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
		assertNotNull("the workspace component must exist", component);
		int before  = component.getApiTypeContainers().length;
		
		ProjectUtils.addFolderToProject(project.getProject(), "bin3");
		IContainer container = ProjectUtils.addFolderToProject(project.getProject(), "src2");
		// add to bundle class path
		IBundleProjectService service = ProjectUtils.getBundleProjectService();
		IBundleClasspathEntry next = service.newBundleClasspathEntry(new Path("src2"), new Path("bin3"), new Path("next.jar"));
		ProjectUtils.addBundleClasspathEntry(project.getProject(), next);
		waitForAutoBuild();
		
		// retrieve updated component
		component = getWorkspaceBaseline().getApiComponent(project.getElementName());
		assertTrue("there must be one more container after the change", before < component.getApiTypeContainers().length);
		IPackageFragmentRoot root = project.getPackageFragmentRoot(container);
		assertTrue("the class file container for src2 must be 'bin3'", "bin3".equals(root.getRawClasspathEntry().getOutputLocation().toFile().getName()));
	}
	
	/**
	 * Tests that adding a package does not update the workspace baseline
	 */
	public void testWPUpdatePackageAdded() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		
		//add the package
		assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "a.test1.c.d");
		
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("a.test1.c.d"));
		assertNotNull("the annotations for package "+TESTING_PACKAGE+" should exist", annot);
	}
	
	/**
	 * Tests that removing a package updates the workspace baseline
	 * This test removes the a.b.c package being used in all tests thus far,
	 * and should be run last
	 */
	public void testWPUpdatePackageRemoved() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		
		//add the package
		IPath srcroot = new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute();
		IPackageFragment fragment = assertTestPackage(project, srcroot, "a.test2");
		assertNotNull("the package "+TESTING_PACKAGE+" must exist", fragment);
		
		//remove the package
		fragment.delete(true, new NullProgressMonitor());
		
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc);
		IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("a.test2"));
		assertNull("the annotations for package "+TESTING_PACKAGE+" should not exist", annot);
	}
	
	/**
	 * Tests that an exported package addition in the PDE model is reflected in the workspace
	 * api baseline
	 */
	public void testWPUpdateExportPackageAdded() throws Exception {
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
	
	/**
	 * Tests that changing a directive to x-internal on an exported package causes the workspace 
	 * api baseline to be updated
	 */
	public void testWPUPdateExportPackageDirectiveChangedToInternal() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		
		//add package
		assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1");
		
		//export the package
		ProjectUtils.addExportedPackage(project.getProject(), "export1", true, null);
		
		//check the description
		IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
		assertNotNull("there must be an annotation for the new exported package", annot);
		assertTrue("the changed exported package must be PRIVATE visibility", annot.getVisibility() == VisibilityModifiers.PRIVATE);
	}
	
	/**
	 * Tests that an exported package removal in the PDE model is reflected in the workspace
	 * api baseline
	 */
	public void testWPUpdateExportPackageRemoved() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project);
		
		//add package
		assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1");
		
		setPackageToApi(project, "export1");
		IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
		assertNotNull("there must be an annotation for the new exported package", annot);
		assertTrue("the newly exported package must be API visibility", annot.getVisibility() == VisibilityModifiers.API);
		
		// remove exported packages
		IBundleProjectService service = ProjectUtils.getBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project.getProject());
		description.setPackageExports(null);
		description.apply(null);
		
		// check the API description
		annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
		assertNotNull("should still be an annotation for the package", annot);
		assertTrue("unexported package must be private", VisibilityModifiers.isPrivate(annot.getVisibility()));
	}
	
	/**
	 * sets the given package name to be an Exported-Package
	 * @param name
	 */
	private void setPackageToApi(IJavaProject project, String name) throws CoreException {
		ProjectUtils.addExportedPackage(project.getProject(), name, false, null);
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
		setPackageToApi(getTestingProject(), TESTING_PACKAGE);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		deleteProject(TESTING_PLUGIN_PROJECT_NAME);
		getWorkspaceBaseline().dispose();
	}
}
