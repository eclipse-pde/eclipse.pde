package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;

public class DependencyAnalysisSection extends PDESection implements IPartSelectionListener {
	private FormText formText;
	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public DependencyAnalysisSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			if (ssel != null && ssel.size() == 1) {
				formText.setText(getFormText((ImportObject)ssel.getFirstElement()), true, false);
			}
		}
		getForm().reflow(false);
	}
	
	private String getFormText(ImportObject dependency) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<form>");
		buffer.append("<p><img href=\"loops\"/> <a href=\"loops\">Look for cycles in the dependency graph</a></p>");
		if (dependency != null) {
			buffer.append("<p><img href=\"search\"/> <a href=\"extent\">Why do I need ");
			buffer.append(dependency.getId());
			buffer.append("?</a></p>");
		}
		buffer.append("<p><img href=\"search\"/> <a href=\"unused\">Find unused dependencies</a></p>");
		buffer.append("</form>");
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Dependency Analysis");
		toolkit.createCompositeSeparator(section);
		
		formText = toolkit.createFormText(section, true);
		formText.setText(getFormText(null), true, false);		
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		formText.setImage("loops", lp.get(PDEPluginImages.DESC_LOOP_OBJ));
		formText.setImage("search", lp.get(PDEPluginImages.DESC_PSEARCH_OBJ));
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("extent"))
					doFindPlugins();
				else if (e.getHref().equals("unused"))
					doFindPlugins();
				else if (e.getHref().equals("loops"))
					doFindLoops();
			}
		});
		section.setClient(formText);
	}

	protected void doFindLoops() {
	}

	protected void doFindPlugins() {
	}
}
