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
		layout.leftMargin = layout.rightMargin = 10;
		layout.topMargin = 15;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;

		FormWidgetFactory factory = getFactory();

		FormEngine text;
		HyperlinkAction action;
		SharedLabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		Image pageImage = provider.get(PDEPluginImages.DESC_PAGE_OBJ);
		Image runImage = provider.get(PDEPluginImages.DESC_RUN_EXC);
		Image runWorkbenchImage = provider.get(PDEPluginImages.DESC_WORKBENCH_LAUNCHER_WIZ);

		HyperlinkAction pageAction = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				String pageId = link.getObjectId();
				((PDEFormPage)page).getEditor().showPage(pageId);
			}
		};
		text = factory.createFormEngine(parent);
		text.setHyperlinkSettings(factory.getHyperlinkHandler());
		text.load(PDEPlugin.getResourceString(KEY_TEXT), true, false);
		text.registerTextObject("ExtensionsPage", pageAction);
		text.registerTextObject("OverviewPage", pageAction);
		text.registerTextObject("pageImage", pageImage);
		text.registerTextObject("runImage", runImage);
		text.registerTextObject("runTimeWorkbenchImage", runWorkbenchImage);
		TableData td = new TableData();
		td.grabHorizontal = true;
		text.setLayoutData(td);
	}

	public void initialize(Object model) {
		super.initialize(model);
		IPluginModelBase modelBase = (IPluginModelBase) model;
		IPluginBase plugin = modelBase.getPluginBase();
		setHeadingText(
			PDEPlugin.getFormattedMessage(KEY_HEADING, plugin.getTranslatedName()));
	}
}