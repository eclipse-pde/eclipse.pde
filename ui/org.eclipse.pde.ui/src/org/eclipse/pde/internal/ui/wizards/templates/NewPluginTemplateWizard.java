package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.ui.BuildPathUtil;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.pde.ui.IPluginStructureData;
import org.eclipse.pde.ui.IProjectProvider;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.CoreUtility;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.core.resources.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.pde.internal.core.plugin.*;
import java.util.*;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.ui.wizards.PluginPathUpdater;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.wizards.project.ProjectStructurePage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.manifest.ManifestEditor;

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

public abstract class NewPluginTemplateWizard
	extends AbstractNewPluginTemplateWizard {
	private ITemplateSection[] sections;

	/**
	 * Creates a new template wizard.
	 */

	public NewPluginTemplateWizard() {
		sections = createTemplateSections();
	}

	public abstract ITemplateSection[] createTemplateSections();

	public ITemplateSection[] getTemplateSections() {
		return sections;
	}

	public void addAdditionalPages() {
		// add template pages
		for (int i = 0; i < sections.length; i++) {
			sections[i].addPages(this);
		}
	}
}