/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.core.resources.IProject;
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
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.api.tools.internal.ApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;
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
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

/**
 * Tests the {@link ApiProfileManager} without the framework running
 */
public class ApiProfileManagerTests extends AbstractApiTest {

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
				if(signature.equals(Util.getMethodSignatureFromNode(node))) {
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
	private IApiProfileManager fPMmanager = ApiPlugin.getDefault().getApiProfileManager();
	private static IJavaProject fProject = null;
	private static IPackageFragmentRoot fSrcroot = null;
	
	/**
	 * @return the {@link IApiDescription} for the testing project
	 */
	private IApiDescription getTestProjectApiDescription()  throws CoreException {
		IApiProfile profile = getWorkspaceProfile();
		assertNotNull("the workspace profile must exist", profile);
		IApiComponent component = profile.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		if(component != null) {
			return component.getApiDescription();
		}
		return null;
	}
	
	/**
	 * Returns the workspace profile.
	 * 
	 * @return workspace profile
	 */
	private IApiProfile getWorkspaceProfile() {
		return fPMmanager.getWorkspaceProfile();
	}
	
	/**
	 * Creates and returns a test profile with the given id. Also adds it to the profile manager
	 * 
	 * @param id
	 * @return
	 */
	protected IApiProfile getTestProfile(String id) {
		IApiProfile profile = null;
		profile = Factory.newApiProfile(id);
		fPMmanager.addApiProfile(profile);
		return profile;
	}
	
	/**
	 * Tests trying to get the workspace profile without the framework running 
	 */
	public void testGetWorkspaceComponent() {
		IApiProfile profile = getWorkspaceProfile();
		assertTrue("the workspace profile must not be null", profile != null);
	}
	
	/**
	 * Tests that an api profile can be added and retrieved successfully 
	 */
	public void testAddProfile() {
		IApiProfile profile = getTestProfile("addtest");
		assertTrue("the test profile must have been created", profile != null);
		profile = fPMmanager.getApiProfile("addtest");
		assertTrue("the testadd profile must be in the manager", profile != null);
	}
	
	/**
	 * Tests that an api profile can be added/removed successfully
	 */
	public void testRemoveProfile() {
		IApiProfile profile = getTestProfile("removetest");
		assertTrue("the testremove profile must exist", profile != null);
		profile = fPMmanager.getApiProfile("removetest");
		assertTrue("the testremove profile must be in the manager", profile != null);
		assertTrue("the testremove profile should have been removed", fPMmanager.removeApiProfile("removetest"));
	}
	
	/**
	 * Tests that the default profile can be set/retrieved
	 */
	public void testSetDefaultProfile() {
		IApiProfile profile = getTestProfile("testdefault");
		assertTrue("the testdefault profile must exist", profile != null);
		fPMmanager.setDefaultApiProfile("testdefault");
		profile = fPMmanager.getDefaultApiProfile();
		assertTrue("the default profile must be the testdefault profile", profile != null);
	}
	
	/**
	 * Tests that all profiles added to the manager can be retrieved
	 */
	public void testGetAllProfiles() {
		getTestProfile("three");
		IApiProfile[] profiles = fPMmanager.getApiProfiles();
		assertTrue("there should be three profiles", profiles.length == 3);
	}
	
	/**
	 * Tests that all of the profiles have been removed
	 */
	public void testCleanUfPMmanager() {
		assertTrue("the testadd profile should have been removed", fPMmanager.removeApiProfile("addtest"));
		assertTrue("the testdefault profile should have been removed", fPMmanager.removeApiProfile("testdefault"));
		assertTrue("the three profile should have been removed", fPMmanager.removeApiProfile("three"));
	}
	
	/**
	 * Tests creating a modifiable project, and making sure it is added to the workspace
	 * profile
	 * 
	 * @throws CoreException
	 */
	public void testWPUpdateProjectCreation() throws CoreException {
		NullProgressMonitor monitor = new NullProgressMonitor();
		// delete any pre-existing project
        IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(TESTING_PLUGIN_PROJECT_NAME);
        if (pro.exists()) {
            pro.delete(true, true, monitor);
        }
        JavaModelEventWaiter waiter = new JavaModelEventWaiter(TESTING_PLUGIN_PROJECT_NAME, IJavaElementDelta.ADDED, 0, IJavaElement.JAVA_PROJECT);
        // create project and import source
        fProject = ProjectUtils.createPluginProject(TESTING_PLUGIN_PROJECT_NAME, new String[] {PDE.PLUGIN_NATURE, ApiPlugin.NATURE_ID});
        Object obj = waiter.waitForEvent();
        assertNotNull("the added event was not received", obj);
        assertNotNull("The java project must have been created", fProject);
        fSrcroot = ProjectUtils.addSourceContainer(fProject, ProjectUtils.SRC_FOLDER);
        assertNotNull("the src root must have been created", fSrcroot);
        IPackageFragment fragment = fSrcroot.createPackageFragment("a.b.c", true, monitor);
		assertNotNull("the package fragment a.b.c cannot be null", fragment);
        
        // add rt.jar
        IVMInstall vm = JavaRuntime.getDefaultVMInstall();
        assertNotNull("No default JRE", vm);
        ProjectUtils.addContainerEntry(fProject, new Path(JavaRuntime.JRE_CONTAINER));
        IApiProfile profile = getWorkspaceProfile();
        assertNotNull("the workspace profile cannot be null", profile);
        IApiComponent component = profile.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
        assertNotNull("the test project api component must exist in the workspace profile", component);
	}
	
	/**
	 * Tests that closing an api aware project causes the workspace description to be updated
	 */
	public void testWPUpdateProjectClosed() {
		try {
			assertNotNull("the workspace profile must not be null", getWorkspaceProfile());
			IApiComponent component  = getWorkspaceProfile().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNotNull("the change project api component must exist in the workspace profile", component);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(TESTING_PLUGIN_PROJECT_NAME, IJavaElementDelta.CHANGED, IJavaElementDelta.F_CLOSED, IJavaElement.JAVA_PROJECT);
			fProject.getProject().close(new NullProgressMonitor());
			//might need a waiter to ensure the model changed event has been processed
			Object obj = waiter.waitForEvent();
			assertNotNull("the closed event was not received", obj);
			component = getWorkspaceProfile().getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNull("the test project api component should no longer exist in the workspace profile", component);
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that opening an api aware project causes the workspace description to be updated
	 */
	public void testWPUpdateProjectOpen() {
		try {
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(TESTING_PLUGIN_PROJECT_NAME, IJavaElementDelta.CHANGED, IJavaElementDelta.F_OPENED, IJavaElement.JAVA_PROJECT);
			fProject.getProject().open(new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the opened event was not received", obj);
			IApiProfile profile = getWorkspaceProfile();
			assertNotNull("the workspace profile must not be null", profile);
			IApiComponent component = profile.getApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNotNull("the test project api component must exist in the workspace profile", component);
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that adding a source file to an api aware project causes the workspace description
	 * to be updated
	 * This test adds <code>a.b.c.TestClass1</code> to the plugin project
	 */
	public void testWPUpdateSourceAdded() {
		try {
			IPackageFragment fragment = fSrcroot.getPackageFragment("a.b.c");
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass1.java", IJavaElementDelta.ADDED, 0, IJavaElement.COMPILATION_UNIT);
			FileUtils.importFileFromDirectory(SRC_LOC.append("TestClass1.java").toFile(), fragment.getPath(), new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the added event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass1"));
			assertNotNull("the annotations for a.b.c.TestClass1 cannot be null", annot);
			assertTrue("there must be a noinstantiate setting for TestClass1", annot.getRestrictions() == RestrictionModifiers.NO_INSTANTIATE);
		} catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that removing a source file from an api aware project causes the workspace description
	 * to be updated
	 */
	public void testWPUpdateSourceRemoved() {
		try {
			IJavaElement element = fProject.findElement(new Path("a/b/c/TestClass1.java"));
			assertNotNull("the class a.b.c.TestClass1 must exist in the project", element);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass1.java", IJavaElementDelta.REMOVED, 0, IJavaElement.COMPILATION_UNIT);
			element.getResource().delete(true, new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the removed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass1"));
			assertNull("the annotations for a.b.c.TestClass1 should no longer be present", annot);
		} catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
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
	 * Tests that making javadoc changes to the source file TestClass2 cause the workspace profile to be 
	 * updated. 
	 * 
	 * This test adds a @noinstatiate tag to the source file TestClass2
	 */
	public void testWPUpdateSourceTypeChanged() {
		try {			
			NullProgressMonitor monitor = new NullProgressMonitor();
			IPackageFragment fragment = fSrcroot.getPackageFragment("a.b.c");
			FileUtils.importFileFromDirectory(SRC_LOC.append("TestClass2.java").toFile(), fragment.getPath(), monitor);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass2.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) fProject.findElement(new Path("a/b/c/TestClass2.java"));
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
		catch(CoreException e) {
			fail(e.getMessage());
		} catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} catch(BadLocationException e) {
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
			NullProgressMonitor monitor = new NullProgressMonitor();
			IPackageFragment fragment = fSrcroot.getPackageFragment("a.b.c");
			FileUtils.importFileFromDirectory(SRC_LOC.append("TestClass3.java").toFile(), fragment.getPath(), monitor);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass3.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) fProject.findElement(new Path("a/b/c/TestClass3.java"));
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
		catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} catch (BadLocationException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that changing the javadoc for a method updates the workspace profile
	 * 
	 * This test adds a @noextend tag to the method foo() in TestClass1
	 */
	public void testWPUpdateSourceMethodChanged() {
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			IPackageFragment fragment = fSrcroot.getPackageFragment("a.b.c");
			FileUtils.importFileFromDirectory(SRC_LOC.append("TestClass1.java").toFile(), fragment.getPath(), monitor);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass1.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) fProject.findElement(new Path("a/b/c/TestClass1.java"));
			assertNotNull("TestClass1 must exist in the test project", element);
			updateTagInSource(element, "foo", "()V", "@noextend", false);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V"));
			assertNotNull("the annotations for foo() cannot be null", annot);
			assertTrue("there must be a noextend setting for foo()", (annot.getRestrictions() & RestrictionModifiers.NO_EXTEND) != 0);
		}
		catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} catch (BadLocationException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that changing the javadoc for a field updates the workspace profile
	 * 
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	public void testWPUpdateSourceFieldChanged() {
		try {
			NullProgressMonitor monitor = new NullProgressMonitor();
			IPackageFragment fragment = fSrcroot.getPackageFragment("a.b.c");
			FileUtils.importFileFromDirectory(SRC_LOC.append("TestField9.java").toFile(), fragment.getPath(), monitor);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestField9.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) fProject.findElement(new Path("a/b/c/TestField9.java"));
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
		catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} catch (BadLocationException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that removing a tag from a method updates the workspace profile
	 * 
	 * This test removes a @noextend tag to the method foo() in TestClass1
	 */
	public void testWPUpdateSourceMethodRemoveTag() {
		try {
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass1.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) fProject.findElement(new Path("a/b/c/TestClass1.java"));
			assertNotNull("TestClass1 must exist in the test project", element);
			updateTagInSource(element, "foo", "()V", "@noextend", true);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestClass1", "foo", "()V"));
			assertNotNull("the annotations for foo() cannot be null", annot);
			assertTrue("there must be no restrictions for foo()", annot.getRestrictions() == 0);
		}
		catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} catch (BadLocationException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that removing a tag from a type updates the workspace profile
	 * 
	 * This test removes a @noinstantiate tag to an inner class in TestClass3
	 */
	public void testWPUpdateSourceTypeRemoveTag() {
		try {
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestClass3.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) fProject.findElement(new Path("a/b/c/TestClass3.java"));
			assertNotNull("TestClass3 must exist in the test project", element);
			updateTagInSource(element, "InnerTestClass3", null, "@noinstantiate", true);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3"));
			assertNotNull("the annotations for 'InnerTestClass3' cannot be null", annot);
			assertTrue("there must be a no restrictions for 'InnerTestClass3'", (annot.getRestrictions() & RestrictionModifiers.NO_INSTANTIATE) == 0);
		}
		catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} catch (BadLocationException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that removing a tag from a field updates the workspace profile
	 * 
	 * This test adds a @noextend tag to the field 'field' in TestField9
	 */
	public void testWPUpdateSourceFieldRemoveTag() {
		try {
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("TestField9.java", IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_PRIMARY_RESOURCE, IJavaElement.COMPILATION_UNIT);
			ICompilationUnit element = (ICompilationUnit) fProject.findElement(new Path("a/b/c/TestField9.java"));
			assertNotNull("TestField9 must exist in the test project", element);
			updateTagInSource(element, "field", null, "@noreference", true);
			Object obj = waiter.waitForEvent();
			assertNotNull("the content changed event for the compilation unit was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField9", "field"));
			assertNotNull("the annotations for 'field' cannot be null", annot);
			assertTrue("there must be a no restrictions for 'field'", annot.getRestrictions() == 0);
		}
		catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} catch (BadLocationException e) {
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
		try {
			IFolder folder = fProject.getProject().getFolder("libx");
			folder.create(false, true, null);
			FileUtils.importFileFromDirectory(PLUGIN_LOC.append("component.a_1.0.0.jar").toFile(), folder.getFullPath(), null);
			IPath libPath = folder.getFullPath().append("component.a_1.0.0.jar");
			IClasspathEntry entry = JavaCore.newLibraryEntry(libPath, null, null);
			IApiComponent component = getWorkspaceProfile().getApiComponent(fProject.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getClassFileContainers().length;
			// add to classpath
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("component.a_1.0.0.jar", IJavaElementDelta.CHANGED, IJavaElementDelta.F_ADDED_TO_CLASSPATH, IJavaElement.PACKAGE_FRAGMENT_ROOT);
			ProjectUtils.addToClasspath(fProject, entry);
			Object obj = waiter.waitForEvent();
			assertNotNull("the event for class path addition not received", obj);
			// add to manifest bundle classpath
			IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
			assertNotNull("the plugin model for the testing project must exist", model);
			IFile file = (IFile) model.getUnderlyingResource();
			assertNotNull("the underlying model file must exist", file);
			WorkspaceBundleModel manifest = new WorkspaceBundleModel(file);
			manifest.getBundle().setHeader(Constants.BUNDLE_CLASSPATH, ".," + libPath.removeFirstSegments(1).toString());
			PluginModelEventWaiter waiter2 = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			manifest.save();
			Object object = waiter2.waitForEvent();
			assertNotNull("the event for manifest modification was not received", object);
			// re-retrieve updated component
			component = getWorkspaceProfile().getApiComponent(fProject.getElementName());
			assertTrue("there must be more containers after the addition", before < component.getClassFileContainers().length);
		}
		catch(JavaModelException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests removing a library from the classpath of a project
	 */
	public void testWPUpdateLibraryRemovedFromClasspath() {
		try {
			IFile lib = fProject.getProject().getFolder("libx").getFile("component.a_1.0.0.jar");
			IClasspathEntry entry = JavaCore.newLibraryEntry(lib.getFullPath(), null, null);
			IApiComponent component = getWorkspaceProfile().getApiComponent(fProject.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getClassFileContainers().length;
			// remove classpath entry
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("component.a_1.0.0.jar", IJavaElementDelta.CHANGED, IJavaElementDelta.F_REMOVED_FROM_CLASSPATH, IJavaElement.PACKAGE_FRAGMENT_ROOT);
			ProjectUtils.removeFromClasspath(fProject, entry);
			Object obj = waiter.waitForEvent();
			assertNotNull("the added event for the package fragment was not received", obj);
			// remove from bundle class path
			IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
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
			component = getWorkspaceProfile().getApiComponent(fProject.getElementName());
			assertTrue("there must be more containers after the addition", before > component.getClassFileContainers().length);
		}
		catch(JavaModelException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that changing the output folder settings for a project cause the class file containers 
	 * to be updated
	 */
	public void testWPUpdateDefaultOutputFolderChanged() {
		try {
			IContainer container = ProjectUtils.addFolderToProject(fProject.getProject(), "bin2");
			assertNotNull("the new output folder cannot be null", container);
			JavaModelEventWaiter waiter = new JavaModelEventWaiter(TESTING_PLUGIN_PROJECT_NAME, IJavaElementDelta.CHANGED, IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED | IJavaElementDelta.F_CLASSPATH_CHANGED, IJavaElement.JAVA_PROJECT);
			IApiComponent component = getWorkspaceProfile().getApiComponent(fProject.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getClassFileContainers().length;
			fProject.setOutputLocation(container.getFullPath(), new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the changed event for the project (classpath) was not received", obj);
			assertTrue("there must be the same number of containers after the change", before == component.getClassFileContainers().length);
			assertTrue("the new output location should be 'bin2'", "bin2".equalsIgnoreCase(fProject.getOutputLocation().toFile().getName()));
		}
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that the output folder settings for a source folder cause the class file containers to 
	 * be updated
	 */
	public void testWPUpdateOutputFolderSrcFolderChanged() {
		try {
			IContainer container = ProjectUtils.addFolderToProject(fProject.getProject(), "bin3");
			assertNotNull("the new output location cannot be null", container);
			IPackageFragmentRoot src2 = ProjectUtils.addSourceContainer(fProject, "src2");
			assertNotNull("the new source folder cannot be null", src2);
			assertNull("the default output location should be 'bin2' (implicit as null)", src2.getRawClasspathEntry().getOutputLocation());
			IClasspathEntry entry = JavaCore.newSourceEntry(src2.getPath(), new IPath[]{}, container.getFullPath());
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("src2", IJavaElementDelta.CHANGED, IJavaElementDelta.F_ADDED_TO_CLASSPATH | IJavaElementDelta.F_REMOVED_FROM_CLASSPATH, IJavaElement.PACKAGE_FRAGMENT_ROOT);
			IApiComponent component = getWorkspaceProfile().getApiComponent(fProject.getElementName());
			assertNotNull("the workspace component must exist", component);
			int before  = component.getClassFileContainers().length;
			ProjectUtils.addToClasspath(fProject, entry);
			Object obj = waiter.waitForEvent();
			assertNotNull("the changed event for the package fragment root (classpath) was not received", obj);
			// add to bundle class path
			IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
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
			WorkspaceBuildModel prop = new WorkspaceBuildModel(fProject.getProject().getFile("build.properties"));
			IBuildEntry newEntry = prop.getFactory().createEntry("source.next.jar");
			newEntry.addToken("src2/");
			prop.getBuild().add(newEntry);
			PluginModelEventWaiter waiter3 = new PluginModelEventWaiter(PluginModelDelta.CHANGED);
			prop.save();
			Object object3 = waiter3.waitForEvent();
			assertNotNull("the event for biuld.properties modification was not received", object3);
			// retrieve updated component
			component = getWorkspaceProfile().getApiComponent(fProject.getElementName());
			assertTrue("there must be one more container after the change", before < component.getClassFileContainers().length);
			assertTrue("the class file container for src2 must be 'bin3'", "bin3".equals(src2.getRawClasspathEntry().getOutputLocation().toFile().getName()));
		}
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that adding a package does not update the workspace profile
	 */
	public void testWPUpdatePackageAdded() {
		try {
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("x.y.z", IJavaElementDelta.ADDED, 0, IJavaElement.PACKAGE_FRAGMENT);
			fSrcroot.createPackageFragment("x.y.z", true, new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the added event for the package fragment was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("x.y.z"));
			assertNotNull("the annotations for package 'x.y.z' should exist", annot);
		}
		catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that removing a package updates the workspace profile
	 * This test removes the a.b.c package being used in all tests thus far,
	 * and should be run last
	 */
	public void testWPUpdatePackageRemoved() {
		try {
			JavaModelEventWaiter waiter = new JavaModelEventWaiter("a.b.c", IJavaElementDelta.REMOVED, 0, IJavaElement.PACKAGE_FRAGMENT);
			IPackageFragment fragment = fSrcroot.getPackageFragment("a.b.c");
			assertNotNull("the package a.b.c must exist", fragment);
			fragment.delete(true, new NullProgressMonitor());
			Object obj = waiter.waitForEvent();
			assertNotNull("the removed event for the package fragment was not received", obj);
			IApiDescription desc = getTestProjectApiDescription();
			assertNotNull("the testing project api description must exist", desc);
			IApiAnnotations annot = desc.resolveAnnotations(Factory.packageDescriptor("a.b.c"));
			assertNull("the annotations for package 'a.b.c' should not exist", annot);
		}
		catch (JavaModelException e) {
			fail(e.getMessage());
		} catch (MalformedTreeException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		} 
	}
	
	/**
	 * Tests that an exported package addition in the PDE model is reflected in the workspace
	 * api profile
	 */
	public void testWPUpdateExportPackageAdded() {
		try {
			IPackageFragment fragment = fSrcroot.createPackageFragment("export1", true, new NullProgressMonitor());
			assertNotNull("the new package 'export1' must exist", fragment);
			setPackageToApi("export1");
			IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
			assertNotNull("there must be an annotation for the new exported package", annot);
			assertTrue("the newly exported package must be API visibility", annot.getVisibility() == VisibilityModifiers.API);
		}
		catch(JavaModelException e) {
			fail(e.getMessage());
		} catch (CoreException e) {
			fail(e.getMessage());
		}
		
	}
	
	/**
	 * Tests that changing a directive to x-internal on an exported package causes the workspace 
	 * api profile to be updated
	 */
	public void testWPUPdateExportPackageDirectiveChangedToInternal() {
		try {
			IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
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
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that an exported package removal in the PDE model is reflected in the workspace
	 * api profile
	 */
	public void testWPUpdateExportPackageRemoved() {
		try {
			setPackageToApi("export1");
			IApiAnnotations annot = getTestProjectApiDescription().resolveAnnotations(Factory.packageDescriptor("export1"));
			assertNotNull("there must be an annotation for the new exported package", annot);
			assertTrue("the newly exported package must be API visibility", annot.getVisibility() == VisibilityModifiers.API);
			IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
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
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * sets the given package name to be an Exported-Package
	 * @param name
	 */
	private void setPackageToApi(String name) {
		IPluginModelBase model = PluginRegistry.findModel(fProject.getProject());
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
}
