package org.eclipse.pde.ui.tests.imports;

import java.util.ArrayList;

import junit.framework.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.wizards.imports.*;


public class PluginImportTest extends TestCase {

	public static Test suite() {
		return new TestSuite(PluginImportTest.class); 
	}
	
	public void testImportBinary() {
		try {
			importPlugin("org.eclipse.jdt.ui", true, false); //$NON-NLS-1$
		} catch (Exception e) {
			fail("testImportBinary: " + e); //$NON-NLS-1$
		} 	
	}
	
	public void testImportBinaryWithLinks() {
		try {
			importPlugin("org.eclipse.pde.core", false, false); //$NON-NLS-1$
		} catch (Exception e) {
			fail("testImportBinaryWithLinks: " + e); //$NON-NLS-1$
		} 			
	}
	
	public void testImportWithSource() {
		try {
			importPlugin("org.eclipse.team.core", true, true); //$NON-NLS-1$
		} catch (Exception e) {
			fail("testImportWithSource: " + e); //$NON-NLS-1$
		} 			
	}
	private void importPlugin(String id, boolean doImport, boolean doExtractSource) throws OperationCanceledException, CoreException  {
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(id, "");
		assertNotNull(entry);
		
		IPluginModelBase model = entry.getExternalModel();
		assertNotNull(model);
		ArrayList ids = new ArrayList();
		ids.add(model.getPluginBase().getId());
		PluginImportOperation op =
			new PluginImportOperation(
				new IPluginModelBase[] { model },
				ids,
				doImport,
				doExtractSource,
				new PluginImportOperation.IReplaceQuery() {
				public int doQuery(IProject project) {
					return PluginImportOperation.IReplaceQuery.YES;
				}

			});
		op.run(null);
			
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = workspaceRoot.getProject(id);
		assertTrue(project.exists());
		assertTrue(project.hasNature(JavaCore.NATURE_ID));
		assertTrue(checkSourceAttached(JavaCore.create(project)));
	}
	
	private boolean checkSourceAttached(IJavaProject jProject) throws CoreException {
		IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			IClasspathEntry entry = roots[i].getRawClasspathEntry();
			if (entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER || !entry.getPath().equals(new Path(PDECore.CLASSPATH_CONTAINER_ID)))
				continue;
			if (roots[i].getSourceAttachmentPath() == null)
				return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		try {
			for (int i = 0; i < projects.length; i++) {
				projects[i].delete(true, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			// do nothing if deletion fails.  No need to fail the test.
		}
	}
}
