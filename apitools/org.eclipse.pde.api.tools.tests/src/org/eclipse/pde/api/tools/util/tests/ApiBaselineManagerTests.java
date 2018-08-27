/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link ApiBaselineManager} without the framework running
 */
@SuppressWarnings("unchecked")
public class ApiBaselineManagerTests extends AbstractApiTest {

	static final String THREE = "three"; //$NON-NLS-1$
	static final String TESTDEFAULT = "testdefault"; //$NON-NLS-1$
	static final String ADDTEST = "addtest"; //$NON-NLS-1$

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

		@Override
		public boolean visit(FieldDeclaration node) {
			if (signature != null) {
				return false;
			}
			List<VariableDeclarationFragment> fields = node.fragments();
			VariableDeclarationFragment fragment = null;
			for (Iterator<VariableDeclarationFragment> iter = fields.iterator(); iter.hasNext();) {
				fragment = iter.next();
				if (fragment.getName().getFullyQualifiedName().equals(name)) {
					break;
				}
			}
			if (fragment != null) {
				updateTag(node);
			}
			return false;
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			if (name.equals(node.getName().getFullyQualifiedName())) {
				if (signature.equals(Signatures.getMethodSignatureFromNode(node, true))) {
					updateTag(node);
				}
			}
			return false;
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			if (name.equals(node.getName().getFullyQualifiedName())) {
				updateTag(node);
				return false;
			}
			return true;
		}

		/**
		 * Updates a javadoc tag, by either adding a new one or removing an
		 * existing one
		 *
		 * @param body
		 */
		private void updateTag(BodyDeclaration body) {
			Javadoc docnode = body.getJavadoc();
			AST ast = body.getAST();
			if (docnode == null) {
				docnode = ast.newJavadoc();
				rewrite.set(body, body.getJavadocProperty(), docnode, null);
			}
			ListRewrite lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
			if (remove) {
				List<TagElement> tags = (List<TagElement>) docnode.getStructuralProperty(Javadoc.TAGS_PROPERTY);
				if (tags != null) {
					TagElement tag = null;
					for (int i = 0; i < tags.size(); i++) {
						tag = tags.get(i);
						if (tagname.equals(tag.getTagName())) {
							lrewrite.remove(tag, null);
						}
					}
				}
			} else {
				TagElement newtag = ast.newTagElement();
				newtag.setTagName(tagname);
				lrewrite.insertLast(newtag, null);
			}
		}
	}

	private IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-source").append("a").append("b").append("c"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private IPath PLUGIN_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-plugins"); //$NON-NLS-1$
	private IApiBaselineManager fPMmanager = ApiPlugin.getDefault().getApiBaselineManager();
	private final String TESTING_PACKAGE = "a.b.c"; //$NON-NLS-1$

	/**
	 * @return the {@link IApiDescription} for the testing project
	 */
	private IApiDescription getTestProjectApiDescription() throws CoreException {
		IApiBaseline baseline = getWorkspaceBaseline();
		assertNotNull("the workspace baseline must exist", baseline); //$NON-NLS-1$
		IApiComponent component = baseline.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		if (component != null) {
			return component.getApiDescription();
		}
		return null;
	}

	/**
	 * Creates and returns a test baseline with the given id. Also adds it to
	 * the baseline manager
	 *
	 * @param id
	 * @return
	 */
	protected IApiBaseline getTestBaseline(String id) {
		IApiBaseline baseline = ApiModelFactory.newApiBaseline(id);
		fPMmanager.addApiBaseline(baseline);
		return baseline;
	}

	/**
	 * Tests trying to get the workspace baseline without the framework running
	 */
	@Test
	public void testGetWorkspaceComponent() {
		IApiBaseline baseline = getWorkspaceBaseline();
		assertNotNull("the workspace baseline must not be null", baseline); //$NON-NLS-1$
	}

	/**
	 * Tests that an API baseline can be added and retrieved successfully
	 */
	@Test
	public void testAddBaseline() {
		IApiBaseline baseline = getTestBaseline(ADDTEST);
		assertNotNull("the test baseline must have been created", baseline); //$NON-NLS-1$
		assertTrue("the testadd baseline must be in the manager", fPMmanager.removeApiBaseline(ADDTEST)); //$NON-NLS-1$
	}

	/**
	 * Tests that an API baseline can be added/removed successfully
	 */
	@Test
	public void testRemoveBaseline() {
		IApiBaseline baseline = getTestBaseline("removetest"); //$NON-NLS-1$
		assertNotNull("the testremove baseline must exist", baseline); //$NON-NLS-1$
		baseline = fPMmanager.getApiBaseline("removetest"); //$NON-NLS-1$
		assertNotNull("the testremove baseline must be in the manager", baseline); //$NON-NLS-1$
		assertTrue("the testremove baseline should have been removed", fPMmanager.removeApiBaseline("removetest")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the default baseline can be set/retrieved
	 */
	@Test
	public void testSetDefaultBaseline() {
		try {
			IApiBaseline baseline = getTestBaseline(TESTDEFAULT);
			assertNotNull("the testdefault baseline must exist", baseline); //$NON-NLS-1$
			fPMmanager.setDefaultApiBaseline(TESTDEFAULT);
			baseline = fPMmanager.getDefaultApiBaseline();
			assertNotNull("the default baseline must be the testdefault baseline", baseline); //$NON-NLS-1$
		} finally {
			fPMmanager.removeApiBaseline(TESTDEFAULT);
		}
	}

	/**
	 * Tests that all baselines added to the manager can be retrieved
	 */
	@Test
	public void testGetAllBaselines() {
		try {
			fPMmanager.addApiBaseline(getTestBaseline(ADDTEST));
			fPMmanager.addApiBaseline(getTestBaseline(TESTDEFAULT));
			fPMmanager.addApiBaseline(getTestBaseline(THREE));
			IApiBaseline[] baselines = fPMmanager.getApiBaselines();
			assertEquals("there should be three baselines", 3, baselines.length); //$NON-NLS-1$
		} finally {
			fPMmanager.removeApiBaseline(ADDTEST);
			fPMmanager.removeApiBaseline(TESTDEFAULT);
			fPMmanager.removeApiBaseline(THREE);
		}
	}

	/**
	 * Tests that all of the baselines have been removed
	 */
	@Test
	public void testCleanUpMmanager() {
		try {
			fPMmanager.addApiBaseline(getTestBaseline(ADDTEST));
			fPMmanager.addApiBaseline(getTestBaseline(TESTDEFAULT));
			fPMmanager.addApiBaseline(getTestBaseline(THREE));
			IApiBaseline[] baselines = fPMmanager.getApiBaselines();
			assertEquals("there should be three baselines", 3, baselines.length); //$NON-NLS-1$
			assertTrue("the testadd baseline should have been removed", fPMmanager.removeApiBaseline(ADDTEST)); //$NON-NLS-1$
			assertTrue("the testdefault baseline should have been removed", fPMmanager.removeApiBaseline(TESTDEFAULT)); //$NON-NLS-1$
			assertTrue("the three baseline should have been removed", fPMmanager.removeApiBaseline(THREE)); //$NON-NLS-1$
			assertEquals("There sould be no more baselines", 0, fPMmanager.getApiBaselines().length); //$NON-NLS-1$
		} finally {
			fPMmanager.removeApiBaseline(ADDTEST);
			fPMmanager.removeApiBaseline(TESTDEFAULT);
			fPMmanager.removeApiBaseline(THREE);
		}
	}

	/**
	 * Adds the given source to the given package in the given fragment root
	 *
	 * @param root the root to add the source to
	 * @param packagename the name of the package e.g. a.b.c
	 * @param sourcename the name of the source file without an extension e.g.
	 *            TestClass1
	 */
	public void assertTestSource(IPackageFragmentRoot root, String packagename, String sourcename) throws InvocationTargetException, IOException {
		IPackageFragment fragment = root.getPackageFragment(packagename);
		FileUtils.importFileFromDirectory(SRC_LOC.append(sourcename + ".java").toFile(), fragment.getPath(), new NullProgressMonitor()); //$NON-NLS-1$
	}

	/**
	 * Adds the package with the given name to the given package fragment root
	 *
	 * @param the project to add the package to
	 * @param srcroot the absolute path to the package fragment root to add the
	 *            new package to
	 * @param packagename the name of the new package
	 * @return the new {@link IPackageFragment} or <code>null</code>
	 */
	public IPackageFragment assertTestPackage(IJavaProject project, IPath srcroot, String packagename) throws JavaModelException {
		IPackageFragment fragment = null;
		IPackageFragmentRoot root = project.findPackageFragmentRoot(srcroot);
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		fragment = root.createPackageFragment(packagename, true, new NullProgressMonitor());
		assertNotNull("the new package '" + packagename + "' should have been created", fragment); //$NON-NLS-1$ //$NON-NLS-2$
		return fragment;
	}

	/**
	 * Adds a test library with the given name to the test projects' class path.
	 * The library is imported from the {@link #PLUGIN_LOC} location.
	 *
	 * @param project the project to add the library classpath entry to
	 * @param folderpath the path in the project where the library should be
	 *            imported to
	 * @param libname the name of the library
	 */
	public IFolder assertTestLibrary(IJavaProject project, IPath folderpath, String libname) throws CoreException, InvocationTargetException, IOException {
		IFolder folder = null;
		// import library
		folder = project.getProject().getFolder(folderpath);
		if (!folder.exists()) {
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
	 *
	 * @param packagename
	 * @param sourcename
	 */
	public void assertSourceResctriction(String packagename, String sourcename, int restriction) throws CoreException {
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor(packagename + "." + sourcename)); //$NON-NLS-1$
		assertNotNull("the annotations for " + packagename + "." + sourcename + " cannot be null", annot); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("there must be a noinstantiate setting for TestClass1", annot.getRestrictions(), restriction); //$NON-NLS-1$
	}

	/**
	 * Tests that closing an API aware project causes the workspace description
	 * to be updated
	 */
	@Test
	public void testWPUpdateProjectClosed() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		assertNotNull("the workspace baseline must not be null", getWorkspaceBaseline()); //$NON-NLS-1$
		IApiComponent component = getWorkspaceBaseline().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the change project api component must exist in the workspace baseline", component); //$NON-NLS-1$
		project.getProject().close(new NullProgressMonitor());
		component = getWorkspaceBaseline().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNull("the test project api component should no longer exist in the workspace baseline", component); //$NON-NLS-1$
	}

	/**
	 * Tests that opening an API aware project causes the workspace description
	 * to be updated
	 */
	@Test
	public void testWPUpdateProjectOpen() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		if (project.getProject().isAccessible()) {
			project.getProject().close(new NullProgressMonitor());
		}
		project.getProject().open(new NullProgressMonitor());
		IApiBaseline baseline = getWorkspaceBaseline();
		assertNotNull("the workspace baseline must not be null", baseline); //$NON-NLS-1$
		IApiComponent component = baseline.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the test project api component must exist in the workspace baseline", component); //$NON-NLS-1$
	}

	/**
	 * Tests that adding a source file to an API aware project causes the
	 * workspace description to be updated This test adds
	 * <code>a.b.c.TestClass1</code> to the plug-in project
	 */
	@Test
	public void testWPUpdateSourceAdded() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute().makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestClass1"); //$NON-NLS-1$
		assertSourceResctriction(TESTING_PACKAGE, "TestClass1", RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
	}

	/**
	 * Tests that removing a source file from an API aware project causes the
	 * workspace description to be updated
	 */
	@Test
	public void testWPUpdateSourceRemoved() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestClass1"); //$NON-NLS-1$
		IJavaElement element = project.findElement(new Path("a/b/c/TestClass1.java")); //$NON-NLS-1$
		assertNotNull("the class a.b.c.TestClass1 must exist in the project", element); //$NON-NLS-1$
		element.getResource().delete(true, new NullProgressMonitor());
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass1")); //$NON-NLS-1$
		assertNull("the annotations for a.b.c.TestClass1 should no longer be present", annot); //$NON-NLS-1$
	}

	/**
	 * Adds the specified tag to the source member defined by the member name
	 * and signature
	 *
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
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(unit);
		CompilationUnit cunit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
		assertNotNull("the ast compilation unit cannot be null", cunit); //$NON-NLS-1$
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
	 * Tests that making Javadoc changes to the source file TestClass2 cause the
	 * workspace baseline to be updated.
	 *
	 * This test adds a @noinstantiate tag to the source file TestClass2
	 */
	@Test
	public void testWPUpdateSourceTypeChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		NullProgressMonitor monitor = new NullProgressMonitor();
		IPackageFragment fragment = root.getPackageFragment("a.b.c"); //$NON-NLS-1$
		FileUtils.importFileFromDirectory(SRC_LOC.append("TestClass2.java").toFile(), fragment.getPath(), monitor); //$NON-NLS-1$
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass2.java")); //$NON-NLS-1$
		assertNotNull("TestClass2 must exist in the test project", element); //$NON-NLS-1$
		updateTagInSource(element, "TestClass2", null, "@noinstantiate", false); //$NON-NLS-1$ //$NON-NLS-2$
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass2")); //$NON-NLS-1$
		assertNotNull("the annotations for a.b.c.TestClass2 cannot be null", annot); //$NON-NLS-1$
		assertTrue("there must be a noinstantiate setting for TestClass2", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) != 0); //$NON-NLS-1$
		assertTrue("there must be a noextend setting for TestClass2", (annot.getRestrictions() & RestrictionModifiers.NO_EXTEND) != 0); //$NON-NLS-1$
	}

	/**
	 * Tests that tags updated on an inner type are updated in the workspace
	 * description.
	 *
	 * This test adds a @noinstantiate tag to an inner class in TestClass3
	 */
	@Test
	public void testWPUpdateSourceInnerTypeChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestClass3"); //$NON-NLS-1$
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass3.java")); //$NON-NLS-1$
		assertNotNull("TestClass3 must exist in the test project", element); //$NON-NLS-1$
		updateTagInSource(element, "InnerTestClass3", null, "@noinstantiate", false); //$NON-NLS-1$ //$NON-NLS-2$
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3")); //$NON-NLS-1$
		assertNotNull("the annotations for a.b.c.TestClass3$InnerTestClass3 cannot be null", annot); //$NON-NLS-1$
		assertFalse("there must not be a noinstantiate setting for TestClass3$InnerTestClass3", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) != 0); //$NON-NLS-1$
		assertFalse("there must not be a noextend setting for TestClass3$InnerTestClass3", (annot.getRestrictions() & RestrictionModifiers.NO_EXTEND) != 0); //$NON-NLS-1$
	}

	/**
	 * Tests that changing the javadoc for a method updates the workspace
	 * baseline
	 *
	 * This test adds a @noextend tag to the method foo() in TestClass1
	 */
	@Test
	public void testWPUpdateSourceMethodChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestClass1"); //$NON-NLS-1$
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass1.java")); //$NON-NLS-1$
		assertNotNull("TestClass1 must exist in the test project", element); //$NON-NLS-1$
		updateTagInSource(element, "foo", "()V", "@nooverride", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("the annotations for foo() cannot be null", annot); //$NON-NLS-1$
		assertTrue("there must be a nooverride setting for foo()", (annot.getRestrictions() & RestrictionModifiers.NO_OVERRIDE) != 0); //$NON-NLS-1$
	}

	/**
	 * Tests that changing the javadoc for a field updates the workspace
	 * baseline
	 *
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	@Test
	public void testWPUpdateSourceFieldChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestField9"); //$NON-NLS-1$
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestField9.java")); //$NON-NLS-1$
		assertNotNull("TestField9 must exist in the test project", element); //$NON-NLS-1$
		updateTagInSource(element, "field", null, "@noreference", false); //$NON-NLS-1$ //$NON-NLS-2$
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField9", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the annotations for 'field' cannot be null", annot); //$NON-NLS-1$
		assertTrue("there must be a noreference setting for 'field'", (annot.getRestrictions() & RestrictionModifiers.NO_REFERENCE) != 0); //$NON-NLS-1$
	}

	/**
	 * Tests that removing a tag from a method updates the workspace baseline
	 *
	 * This test removes a @noextend tag to the method foo() in TestClass1
	 */
	@Test
	public void testWPUpdateSourceMethodRemoveTag() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestClass1"); //$NON-NLS-1$
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass1.java")); //$NON-NLS-1$
		assertNotNull("TestClass1 must exist in the test project", element); //$NON-NLS-1$
		updateTagInSource(element, "foo", "()V", "@nooverride", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("the annotations for foo() cannot be null", annot); //$NON-NLS-1$
		assertTrue("there must be no restrictions for foo()", annot.getRestrictions() == 0); //$NON-NLS-1$
	}

	/**
	 * Tests that removing a tag from a type updates the workspace baseline
	 *
	 * This test removes a @noinstantiate tag to an inner class in TestClass3
	 */
	@Test
	public void testWPUpdateSourceTypeRemoveTag() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestClass3"); //$NON-NLS-1$
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestClass3.java")); //$NON-NLS-1$
		assertNotNull("TestClass3 must exist in the test project", element); //$NON-NLS-1$
		updateTagInSource(element, "InnerTestClass3", null, "@noextend", true); //$NON-NLS-1$ //$NON-NLS-2$
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3")); //$NON-NLS-1$
		assertNotNull("the annotations for 'InnerTestClass3' cannot be null", annot); //$NON-NLS-1$
		assertTrue("there must be a no restrictions for 'InnerTestClass3'", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) == 0); //$NON-NLS-1$
	}

	/**
	 * Tests that removing a tag from a field updates the workspace baseline
	 *
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	@Test
	public void testWPUpdateSourceFieldRemoveTag() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IPackageFragmentRoot root = project.findPackageFragmentRoot(new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute());
		assertNotNull("the 'src' package fragment root must exist", root); //$NON-NLS-1$
		assertTestSource(root, TESTING_PACKAGE, "TestField9"); //$NON-NLS-1$
		ICompilationUnit element = (ICompilationUnit) project.findElement(new Path("a/b/c/TestField9.java")); //$NON-NLS-1$
		assertNotNull("TestField9 must exist in the test project", element); //$NON-NLS-1$
		updateTagInSource(element, "field1", null, "@noreference", true); //$NON-NLS-1$ //$NON-NLS-2$
		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField9", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the annotations for 'field' cannot be null", annot); //$NON-NLS-1$
		assertTrue("there must be a no restrictions for 'field'", annot.getRestrictions() == 0); //$NON-NLS-1$
	}

	/**
	 * Tests that a library added to the build and bundle class path of a
	 * project causes the class file containers for the project to need to be
	 * recomputed
	 *
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws CoreException
	 */
	@Test
	public void testWPUpdateLibraryAddedToClasspath() throws Exception {
		IFolder folder = null;
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project); //$NON-NLS-1$
			IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component); //$NON-NLS-1$
			int before = component.getApiTypeContainers().length;

			// add to classpath
			folder = assertTestLibrary(project, new Path("libx"), "component.a_1.0.0.jar"); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull("The new library path should not be null", folder); //$NON-NLS-1$

			// re-retrieve updated component
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertTrue("there must be more containers after the addition", before < component.getApiTypeContainers().length); //$NON-NLS-1$
		} finally {
			if (folder != null) {
				FileUtils.delete(folder);
			}
		}
	}

	/**
	 * Tests removing a library from the classpath of a project
	 */
	@Test
	public void testWPUpdateLibraryRemovedFromClasspath() throws Exception {
		IPath libPath = null;
		try {
			IJavaProject project = getTestingProject();
			assertNotNull("The testing project must exist", project); //$NON-NLS-1$

			// add to classpath
			IFolder folder = assertTestLibrary(project, new Path("libx"), "component.a_1.0.0.jar"); //$NON-NLS-1$ //$NON-NLS-2$
			IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertNotNull("the workspace component must exist", component); //$NON-NLS-1$
			int before = component.getApiTypeContainers().length;
			libPath = folder.getFullPath().append("component.a_1.0.0.jar"); //$NON-NLS-1$

			// remove classpath entry
			ProjectUtils.removeFromClasspath(project, JavaCore.newLibraryEntry(libPath, null, null));
			waitForAutoBuild();

			// remove from bundle class path
			IBundleProjectService service = ProjectUtils.getBundleProjectService();
			IBundleProjectDescription description = service.getDescription(project.getProject());
			description.setBundleClasspath(new IBundleClasspathEntry[] { service.newBundleClasspathEntry(new Path(ProjectUtils.SRC_FOLDER), null, null) });
			description.apply(null);
			waitForAutoBuild();

			// retrieve updated component
			component = getWorkspaceBaseline().getApiComponent(project.getElementName());
			assertTrue("there must be less containers after the removal", before > component.getApiTypeContainers().length); //$NON-NLS-1$
		} finally {
			if (libPath != null) {
				FileUtils.delete(libPath.toOSString());
			}
		}
	}

	/**
	 * Tests that changing the output folder settings for a project cause the
	 * class file containers to be updated
	 */
	@Test
	public void testWPUpdateDefaultOutputFolderChanged() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$
		IContainer container = ProjectUtils.addFolderToProject(project.getProject(), "bin2"); //$NON-NLS-1$
		assertNotNull("the new output folder cannot be null", container); //$NON-NLS-1$
		IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
		assertNotNull("the workspace component must exist", component); //$NON-NLS-1$
		int before = component.getApiTypeContainers().length;
		project.setOutputLocation(container.getFullPath(), new NullProgressMonitor());
		waitForAutoBuild();
		assertTrue("there must be the same number of containers after the change", before == component.getApiTypeContainers().length); //$NON-NLS-1$
		assertTrue("the new output location should be 'bin2'", "bin2".equalsIgnoreCase(project.getOutputLocation().toFile().getName())); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the output folder settings for a source folder cause the class
	 * file containers to be updated
	 */
	@Test
	public void testWPUpdateOutputFolderSrcFolderChanged() throws Exception {
		IJavaProject project = getTestingProject();
		IApiComponent component = getWorkspaceBaseline().getApiComponent(project.getElementName());
		assertNotNull("the workspace component must exist", component); //$NON-NLS-1$
		int before = component.getApiTypeContainers().length;

		ProjectUtils.addFolderToProject(project.getProject(), "bin3"); //$NON-NLS-1$
		IContainer container = ProjectUtils.addFolderToProject(project.getProject(), "src2"); //$NON-NLS-1$
		// add to bundle class path
		IBundleProjectService service = ProjectUtils.getBundleProjectService();
		IBundleClasspathEntry next = service.newBundleClasspathEntry(new Path("src2"), new Path("bin3"), new Path("next.jar")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ProjectUtils.addBundleClasspathEntry(project.getProject(), next);
		waitForAutoBuild();

		// retrieve updated component
		component = getWorkspaceBaseline().getApiComponent(project.getElementName());
		assertTrue("there must be one more container after the change", before < component.getApiTypeContainers().length); //$NON-NLS-1$
		IPackageFragmentRoot root = project.getPackageFragmentRoot(container);
		assertTrue("the class file container for src2 must be 'bin3'", "bin3".equals(root.getRawClasspathEntry().getOutputLocation().toFile().getName())); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that adding a package does not update the workspace baseline
	 */
	@Test
	public void testWPUpdatePackageAdded() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$

		// add the package
		assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "a.test1.c.d"); //$NON-NLS-1$

		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("a.test1.c.d")); //$NON-NLS-1$
		assertNotNull("the annotations for package " + TESTING_PACKAGE + " should exist", annot); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that removing a package updates the workspace baseline This test
	 * removes the a.b.c package being used in all tests thus far, and should be
	 * run last
	 */
	@Test
	public void testWPUpdatePackageRemoved() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$

		// add the package
		IPath srcroot = new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute();
		IPackageFragment fragment = assertTestPackage(project, srcroot, "a.test2"); //$NON-NLS-1$
		assertNotNull("the package " + TESTING_PACKAGE + " must exist", fragment); //$NON-NLS-1$ //$NON-NLS-2$

		// remove the package
		fragment.delete(true, new NullProgressMonitor());

		IApiDescription desc = getTestProjectApiDescription();
		assertNotNull("the testing project api description must exist", desc); //$NON-NLS-1$
		IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("a.test2")); //$NON-NLS-1$
		assertNull("the annotations for package " + TESTING_PACKAGE + " should not exist", annot); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that an exported package addition in the PDE model is reflected in
	 * the workspace api baseline
	 */
	@Test
	public void testWPUpdateExportPackageAdded() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$

		// add package
		assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1"); //$NON-NLS-1$

		// update
		setPackageToApi(project, "export1"); //$NON-NLS-1$
		IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1")); //$NON-NLS-1$
		assertNotNull("there must be an annotation for the new exported package", annot); //$NON-NLS-1$
		assertTrue("the newly exported package must be API visibility", annot.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that changing a directive to x-internal on an exported package
	 * causes the workspace api baseline to be updated
	 */
	@Test
	public void testWPUPdateExportPackageDirectiveChangedToInternal() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$

		// add package
		assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1"); //$NON-NLS-1$

		// export the package
		ProjectUtils.addExportedPackage(project.getProject(), "export1", true, null); //$NON-NLS-1$

		// check the description
		IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1")); //$NON-NLS-1$
		assertNotNull("there must be an annotation for the new exported package", annot); //$NON-NLS-1$
		assertTrue("the changed exported package must be PRIVATE visibility", annot.getVisibility() == VisibilityModifiers.PRIVATE); //$NON-NLS-1$
	}

	/**
	 * Tests that an exported package removal in the PDE model is reflected in
	 * the workspace api baseline
	 */
	@Test
	public void testWPUpdateExportPackageRemoved() throws Exception {
		IJavaProject project = getTestingProject();
		assertNotNull("The testing project must exist", project); //$NON-NLS-1$

		// add package
		assertTestPackage(project, new Path(project.getElementName()).append(ProjectUtils.SRC_FOLDER).makeAbsolute(), "export1"); //$NON-NLS-1$

		setPackageToApi(project, "export1"); //$NON-NLS-1$
		IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1")); //$NON-NLS-1$
		assertNotNull("there must be an annotation for the new exported package", annot); //$NON-NLS-1$
		assertTrue("the newly exported package must be API visibility", annot.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$

		// remove exported packages
		IBundleProjectService service = ProjectUtils.getBundleProjectService();
		IBundleProjectDescription description = service.getDescription(project.getProject());
		description.setPackageExports(null);
		description.apply(null);

		// check the API description
		annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1")); //$NON-NLS-1$
		assertNotNull("should still be an annotation for the package", annot); //$NON-NLS-1$
		assertTrue("unexported package must be private", VisibilityModifiers.isPrivate(annot.getVisibility())); //$NON-NLS-1$
	}

	/**
	 * sets the given package name to be an Exported-Package
	 *
	 * @param name
	 */
	private void setPackageToApi(IJavaProject project, String name) throws CoreException {
		ProjectUtils.addExportedPackage(project.getProject(), name, false, null);
	}

	IJavaProject getTestingProject() {
		return JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(TESTING_PLUGIN_PROJECT_NAME));
	}

	@Before
	public void setUp() throws Exception {
		createProject(TESTING_PLUGIN_PROJECT_NAME, new String[] { TESTING_PACKAGE });
		setPackageToApi(getTestingProject(), TESTING_PACKAGE);
	}

	@After
	public void tearDown() throws Exception {
		deleteProject(TESTING_PLUGIN_PROJECT_NAME);
		getWorkspaceBaseline().dispose();
	}
}
