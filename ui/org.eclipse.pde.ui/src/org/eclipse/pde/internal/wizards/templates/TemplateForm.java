package org.eclipse.pde.internal.wizards.templates;

import org.eclipse.update.ui.forms.internal.WebForm;
import org.eclipse.update.ui.forms.internal.IFormPage;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.update.ui.forms.internal.engine.*;
import org.eclipse.pde.internal.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.util.SharedLabelProvider;
import org.eclipse.pde.internal.editor.PDEFormPage;
import org.eclipse.pde.internal.editor.manifest.*;
import org.eclipse.debug.ui.actions.RunAction;
import org.eclipse.debug.ui.actions.DebugAction;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import java.util.*;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.ui.part.*;

public class TemplateForm extends WebForm {
	private static final String KEY_HEADING = "ManifestEditor.templatePage.heading";
	private static final String KEY_TEXT = "ManifestEditor.TemplatePage.text";
	private IFormPage page;
	/**
	 * Constructor for TemplateForm.
	 */
	public TemplateForm(IFormPage page) {
		this.page = page;
	}

	protected void createContents(Composite parent) {
		HTMLTableLayout layout = new HTMLTableLayout();
		parent.setLayout(layout);
		layout.leftMargin = 10;
		layout.rightMargin = 0;
		layout.topMargin = 15;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;

		FormWidgetFactory factory = getFactory();

		FormEngine text;
		HyperlinkAction action;
		SharedLabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		Image pageImage = provider.get(PDEPluginImages.DESC_PAGE_OBJ);
		Image runImage = provider.get(PDEPluginImages.DESC_RUN_EXC);
		Image debugImage = provider.get(PDEPluginImages.DESC_DEBUG_EXC);
		Image runWorkbenchImage =
			provider.get(PDEPluginImages.DESC_WORKBENCH_LAUNCHER_WIZ);

		HyperlinkAction pageAction = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				String pageId = link.getObjectId();
				((PDEFormPage) page).getEditor().showPage(pageId);
			}
		};
		HyperlinkAction debugAction = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				String id = link.getObjectId();
				if (id.equals("action.run")) {
					RunAction action = new RunAction();
					action.runWithEvent(null, null);
				} else if (id.equals("action.debug")) {
					DebugAction action = new DebugAction();
					action.runWithEvent(null, null);
				}
			}
		};
		HyperlinkAction expandSource = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				expandSourceFolders();
			}
		};

		HyperlinkAction newExtensionWizard = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				openNewExtensionWizard();
			}
		};
		text = factory.createFormEngine(parent);
		text.setHyperlinkSettings(factory.getHyperlinkHandler());
		text.load(PDEPlugin.getResourceString(KEY_TEXT), true, false);

		// register hyperlink actions
		text.registerTextObject("ExtensionsPage", pageAction);
		text.registerTextObject("OverviewPage", pageAction);
		text.registerTextObject("action.run", debugAction);
		text.registerTextObject("action.debug", debugAction);
		text.registerTextObject("action.expandSource", expandSource);
		text.registerTextObject("action.newExtension", newExtensionWizard);

		// register images
		text.registerTextObject("pageImage", pageImage);
		text.registerTextObject("runImage", runImage);
		text.registerTextObject("debugImage", debugImage);
		text.registerTextObject("runTimeWorkbenchImage", runWorkbenchImage);
		TableData td = new TableData();
		td.grabHorizontal = true;
		text.setLayoutData(td);
	}

	private void expandSourceFolders() {
		IPluginModelBase model = (IPluginModelBase)((PDEFormPage)page).getModel();
		IProject project = model.getUnderlyingResource().getProject();
		IJavaProject javaProject = JavaCore.create(project);
		try {
			IClasspathEntry [] entries = javaProject.getRawClasspath();
			ArrayList sourceFolders = new ArrayList();
			for (int i=0; i<entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind()==IClasspathEntry.CPE_SOURCE) {
					IFolder folder = project.getFolder(entry.getPath());
					if (folder.exists()) sourceFolders.add(folder);
				}
			}
			if (sourceFolders.size()>0) {
				StructuredSelection selection = new StructuredSelection(sourceFolders.toArray());
				IWorkbenchPage page = PDEPlugin.getActivePage();
				IWorkbenchPart part = page.getActivePart();
				if (part instanceof ISetSelectionTarget) {
					((ISetSelectionTarget)part).selectReveal(selection);
				}
			}
		}
		catch (JavaModelException e) {
		}
	}

	private void openNewExtensionWizard() {
		ManifestEditor editor = (ManifestEditor) ((PDEFormPage) page).getEditor();
		ManifestExtensionsPage exPage =
			(ManifestExtensionsPage) editor.getPage(ManifestEditor.EXTENSIONS_PAGE);
		editor.showPage(exPage);
		exPage.openNewExtensionWizard();
	}

	public void initialize(Object model) {
		super.initialize(model);
		IPluginModelBase modelBase = (IPluginModelBase) model;
		IPluginBase plugin = modelBase.getPluginBase();
		setHeadingText(
			PDEPlugin.getFormattedMessage(KEY_HEADING, plugin.getTranslatedName()));
	}
}