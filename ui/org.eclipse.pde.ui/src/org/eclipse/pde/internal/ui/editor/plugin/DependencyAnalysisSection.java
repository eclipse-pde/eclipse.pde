package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.builders.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;

public class DependencyAnalysisSection extends PDESection implements IPartSelectionListener {
	private FormText formText;
	private ImportObject fSelectedDependency;
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
		if (selection == null || selection.isEmpty()) {
			fSelectedDependency = null;
		} else {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			fSelectedDependency = (ImportObject)ssel.getFirstElement();
		}
	}
	
	private String getFormText() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<form>");
		if (getPage().getModel() instanceof IPluginModel)
			buffer.append("<p><img href=\"loops\"/> <a href=\"loops\">Look for cycles in the dependency graph</a></p>");
		buffer.append("<p><img href=\"search\"/> <a href=\"extent\">Compute Dependency Extent</a></p>");
		buffer.append("<p><img href=\"search\"/> <a href=\"unused\">Find unused dependencies</a></p>");
		buffer.append("</form>");
		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Dependency Analysis");
		//toolkit.createCompositeSeparator(section);
		
		formText = toolkit.createFormText(section, true);
		formText.setText(getFormText(), true, false);		
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		formText.setImage("loops", lp.get(PDEPluginImages.DESC_LOOP_OBJ));
		formText.setImage("search", lp.get(PDEPluginImages.DESC_PSEARCH_OBJ));
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (e.getHref().equals("extent"))
					doFindPlugins();
				else if (e.getHref().equals("unused"))
					doFindUnusedDependencies();
				else if (e.getHref().equals("loops"))
					doFindLoops();
			}
		});
		section.setClient(formText);
	}

	protected void doFindLoops() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModel) {
			IPlugin plugin = ((IPluginModel)model).getPlugin();
			DependencyLoop[] loops = DependencyLoopFinder.findLoops(plugin);
			if (loops.length == 0)
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), "Dependency Loops", "The dependency graph of this plug-in does not contain cycles.");
			else {
				LoopDialog dialog = new LoopDialog(PDEPlugin.getActiveWorkbenchShell(), loops);
				dialog.open();
			}
		}	
	}

	protected void doFindPlugins() {
		if (fSelectedDependency != null)
			new DependencyExtentAction(fSelectedDependency.getImport()).run();
	}
	
	protected void doFindUnusedDependencies() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IPluginModelBase) {
			new UnusedDependenciesAction((IPluginModelBase)model).run();
		}
		
	}
}
