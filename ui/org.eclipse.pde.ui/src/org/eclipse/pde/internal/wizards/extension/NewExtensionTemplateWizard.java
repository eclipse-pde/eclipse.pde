package org.eclipse.pde.internal.wizards.extension;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.BuildPathUtil;
import org.eclipse.pde.IPluginContentWizard;
import org.eclipse.pde.IPluginStructureData;
import org.eclipse.pde.IProjectProvider;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.pde.internal.util.CoreUtility;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.core.resources.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.pde.internal.model.plugin.*;
import org.eclipse.pde.internal.wizards.templates.*;
import java.util.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.wizards.PluginPathUpdater;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.PDEPluginImages;
import org.eclipse.pde.internal.wizards.project.ProjectStructurePage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.IExtensionWizard;

/**
 * This wizard should be used as a base class for 
 * wizards that provide new plug-in templates. 
 * These wizards are loaded during new plug-in or fragment
 * creation and are used to provide initial
 * content (Java classes, directory structure and
 * extensions).
 * <p>
 * The wizard provides a common first page that will
 * initialize the plug-in itself. This plug-in will
 * be passed on to the templates to generate additional
 * content. After all templates have executed, 
 * the wizard will use the collected list of required
 * plug-ins to set up Java buildpath so that all the
 * generated Java classes can be resolved during the build.
 */

public class NewExtensionTemplateWizard
	extends Wizard
	implements IExtensionWizard {
	private ITemplateSection section;
	IProject project;
	IPluginModelBase model;
	/**
	 * Creates a new template wizard.
	 */

	public NewExtensionTemplateWizard(ITemplateSection section) {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEX_WIZ);
		setNeedsProgressMonitor(true);
		this.section = section;
	}
	
	public void init(IProject project, IPluginModelBase model) {
		this.project = project;
		this.model = model;
	}

	public void setSection(ITemplateSection section) {
		this.section = section;
	}

	public ITemplateSection getSection() {
		return section;
	}

	public void addPages() {
		section.addPages(this);
	}

	public boolean performFinish() {
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					doFinish(monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	protected void doFinish(IProgressMonitor monitor) throws CoreException {
		int totalWork = section.getNumberOfWorkUnits();
		monitor.beginTask("Generating content...", totalWork);
		updateDependencies();
		section.execute(project, model, monitor); // nsteps
	}

	private void updateDependencies() throws CoreException {
		IPluginReference[] refs = section.getDependencies();
		for (int i = 0; i < refs.length; i++) {
			IPluginReference ref = refs[i];
			if (modelContains(ref) == false) {
				IPluginImport iimport = model.getFactory().createImport();
				iimport.setId(ref.getId());
				iimport.setMatch(ref.getMatch());
				iimport.setVersion(ref.getVersion());
				model.getPluginBase().add(iimport);
			}
		}
	}

	private boolean modelContains(IPluginReference ref) {
		IPluginBase plugin = model.getPluginBase();
		IPluginImport[] imports = plugin.getImports();
		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			if (iimport.getId().equals(ref.getId())) {
				// good enough
				return true;
			}
		}
		return false;
	}
}