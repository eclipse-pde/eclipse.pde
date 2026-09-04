package org.eclipse.e4.tools.emf.ui.internal.common.component.tabs.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.emf.ui.internal.common.component.tabs.XmiTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.FrameworkUtil;

public class XmiTabTest {

	private static final String MODEL_EDITOR_ID = "org.eclipse.e4.tools.emf.editor3x.e4wbm"; //$NON-NLS-1$

	private IProject project;
	private XmiTab xmiTab;
	private Text searchTextWidget;
	private StyledText sourceViewerTextWidget;

	@BeforeEach
	void setup() throws IOException, CoreException {
		project = importTestProject("/tests/projects/xmitabtest");
		waitForUI();

		IFile file = project.getFile("src/Application.e4xmi");
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart part = IDE.openEditor(page, file, MODEL_EDITOR_ID, true);
		IEclipseContext context = part.getSite().getService(IEclipseContext.class);
		Composite composite = context.get(Composite.class);
		waitForUI();

		CTabFolder folder = (CTabFolder) composite.getChildren()[0];
		folder.setSelection(2 /* XMI tab index */);
		waitForUI();

		xmiTab = (XmiTab) folder.getSelection().getControl();
		searchTextWidget = (Text) xmiTab.getChildren()[0];
		Composite sourceViewerComposite = (Composite) xmiTab.getChildren()[1];
		sourceViewerTextWidget = (StyledText) sourceViewerComposite.getChildren()[0];
	}

	@AfterEach
	void tearDown() throws CoreException {
		if (project != null) {
			project.delete(IProject.FORCE | IProject.NEVER_DELETE_PROJECT_CONTENT, null);
		}
	}

	@Test
	void testSearchHighlight() {
		searchTextWidget.forceFocus();
		waitForUI();

		Display display = Display.getDefault();
		display.post(keyEvent('a', SWT.KeyDown, display.getFocusControl()));
		waitForUI();

		int[] ranges = sourceViewerTextWidget.getSelectionRanges();
		assertEquals(ranges[0], 41);
		assertEquals(ranges[1], 1);

		display.post(keyEvent('d', SWT.KeyDown, display.getFocusControl()));
		waitForUI();

		ranges = sourceViewerTextWidget.getSelectionRanges();
		assertEquals(ranges[0], 262);
		assertEquals(ranges[1], 2);

		// find next occurrence
		display.post(keyEvent(SWT.CR, SWT.KeyDown, display.getFocusControl()));
		waitForUI();

		ranges = sourceViewerTextWidget.getSelectionRanges();
		assertEquals(ranges[0], 467);
		assertEquals(ranges[1], 2);
	}

	private static void waitForUI() {
		while (Display.getDefault().readAndDispatch()) {
			// wait for UI
		}
	}

	private static Event keyEvent(int key, int type, Widget widget) {
		Event e = new Event();
		e.keyCode = key;
		e.character = (char) key;
		e.type = type;
		e.widget = widget;
		return e;
	}

	private static IProject importTestProject(String path) throws IOException, CoreException {
		URL entry = FileLocator
				.toFileURL(FileLocator.find(FrameworkUtil.getBundle(XmiTabTest.class), IPath.fromOSString(path)));
		if (entry == null) {
			throw new IllegalArgumentException(path + " does not exist");
		}
		IPath projectFile = IPath.fromPortableString(entry.getPath()).append(IProjectDescription.DESCRIPTION_FILE_NAME);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProjectDescription projectDescription = workspace.loadProjectDescription(projectFile);
		IProject project = workspace.getRoot().getProject(projectDescription.getName());
		project.create(projectDescription, null);
		project.open(null);
		return project;
	}
}
