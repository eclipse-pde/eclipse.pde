/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.samples;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.IStandbyContentPart;
import org.eclipse.ui.part.ISetSelectionTarget;

public class SampleStandbyContent implements IStandbyContentPart {
	private ScrolledForm form;
	private Hyperlink moreLink;
	private String helpURL;
	private String launcher;
	private String launchTarget;
	private FormText descText;
	private FormText instText;
	private ILaunchShortcut defaultShortcut;
	private IConfigurationElement sample;
	// cached input.
	private String input;

	private static String MEMENTO_SAMPLE_ID_ATT = "sampleId"; //$NON-NLS-1$

	/**
	 *
	 */
	public SampleStandbyContent() {
		defaultShortcut = new EclipseLaunchShortcut();
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	@Override
	public void createPartControl(Composite parent, FormToolkit toolkit) {
		form = toolkit.createScrolledForm(parent);
		//form.setBackgroundImage(PDEPlugin.getDefault().getLabelProvider().get(
		//		PDEPluginImages.DESC_FORM_BANNER));
		TableWrapLayout layout = new TableWrapLayout();
		layout.verticalSpacing = 10;
		layout.topMargin = 10;
		layout.bottomMargin = 10;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		form.getBody().setLayout(layout);
		descText = toolkit.createFormText(form.getBody(), true);
		descText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		descText.setText("", false, false); //$NON-NLS-1$
		moreLink = toolkit.createHyperlink(form.getBody(), "Read More", //$NON-NLS-1$
				SWT.NULL);
		moreLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (helpURL != null)
					PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpURL);
			}
		});
		instText = toolkit.createFormText(form.getBody(), true);
		instText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		StringBuilder buf = new StringBuilder();
		buf.append(PDEUIMessages.SampleStandbyContent_content);
		instText.setText(buf.toString(), true, false);
		instText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object href = e.getHref();
				if (href.equals("help")) { //$NON-NLS-1$
					PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpURL);
				} else if (href.equals("browse")) { //$NON-NLS-1$
					doBrowse();
				} else if (href.equals("run")) { //$NON-NLS-1$
					doRun(launcher, launchTarget, false);
				} else if (href.equals("debug")) { //$NON-NLS-1$
					doRun(launcher, launchTarget, true);
				}
			}
		});
		instText.setImage("run", PDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				PDEPluginImages.DESC_RUN_EXC));
		instText.setImage("debug", PDEPlugin.getDefault().getLabelProvider() //$NON-NLS-1$
				.get(PDEPluginImages.DESC_DEBUG_EXC));
		instText.setImage("help", PlatformUI.getWorkbench().getSharedImages() //$NON-NLS-1$
				.getImage(ISharedImages.IMG_OBJS_INFO_TSK));
	}

	private void doRun(String launcher, String target, final boolean debug) {
		ILaunchShortcut shortcut = defaultShortcut;
		final ISelection selection;
		if (target != null) {
			selection = new StructuredSelection();
		} else
			selection = new StructuredSelection();
		final ILaunchShortcut fshortcut = shortcut;
		BusyIndicator.showWhile(form.getDisplay(),
				() -> fshortcut.launch(selection, debug ? ILaunchManager.DEBUG_MODE : ILaunchManager.RUN_MODE));
	}

	private void doBrowse() {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		ISetSelectionTarget target = findTarget();
		if (target == null)
			return;
		String sid = sample.getAttribute("id"); //$NON-NLS-1$
		if (sid == null)
			return;
		ArrayList<IResource> items = new ArrayList<>();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (!project.exists() || !project.isOpen())
				continue;
			IFile pfile = project.getFile("sample.properties"); //$NON-NLS-1$
			if (pfile.exists()) {
				try (InputStream is = pfile.getContents()) {
					Properties prop = new Properties();
					prop.load(is);
					String id = prop.getProperty("id"); //$NON-NLS-1$
					if (id != null && id.equals(sid)) {
						//match
						IResource res = findSelectReveal(project, prop.getProperty("projectName")); //$NON-NLS-1$
						if (res != null)
							items.add(res);
					}
				} catch (IOException | CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
		if (!items.isEmpty())
			target.selectReveal(new StructuredSelection(items));
	}

	private ISetSelectionTarget findTarget() {
		String id = sample.getAttribute("targetViewId"); //$NON-NLS-1$
		if (id == null)
			return null;
		IViewPart view = PDEPlugin.getActivePage().findView(id);
		if (view == null || !(view instanceof ISetSelectionTarget))
			return null;
		return (ISetSelectionTarget) view;
	}

	private IResource findSelectReveal(IProject project, String originalName) {
		IConfigurationElement[] projects = sample.getChildren("project"); //$NON-NLS-1$
		for (int i = 0; i < projects.length; i++) {
			if (originalName.equals(projects[i].getAttribute("name"))) { //$NON-NLS-1$
				String path = projects[i].getAttribute("selectReveal"); //$NON-NLS-1$
				if (path == null)
					continue;
				IResource res = project.findMember(path);
				if (res != null && res.exists())
					return res;
			}
		}
		return null;
	}

	@Override
	public Control getControl() {
		return form;
	}

	public void init(IIntroPart introPart) {
	}

	@Override
	public void setInput(Object input) {
		// if the new input is null, use cached input from momento.
		if (input != null)
			this.input = (String) input;
		String sampleId = this.input.toString();
		IConfigurationElement[] samples = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.pde.ui.samples"); //$NON-NLS-1$
		for (int i = 0; i < samples.length; i++) {
			IConfigurationElement sample = samples[i];
			String id = sample.getAttribute("id"); //$NON-NLS-1$
			if (id != null && id.equals(sampleId)) {
				update(sample);
				return;
			}
		}
		update(null);
	}

	private void update(IConfigurationElement sample) {
		this.sample = sample;
		if (form == null)
			return;
		String title = sample != null ? sample.getAttribute("name") : ""; //$NON-NLS-1$ //$NON-NLS-2$
		form.setText(title);
		if (sample != null) {
			launcher = sample.getAttribute("launcher"); //$NON-NLS-1$
			launchTarget = sample.getAttribute("launchTarget"); //$NON-NLS-1$
		} else {
			launcher = null;
			launchTarget = null;
		}
		IConfigurationElement[] descConfig = sample != null ? sample.getChildren("description") : null; //$NON-NLS-1$
		if (descConfig != null && descConfig.length == 1) {
			String desc = descConfig[0].getValue();
			String content = NLS.bind(PDEUIMessages.SampleStandbyContent_desc, (desc != null ? desc : "")); //$NON-NLS-1$
			helpURL = descConfig[0].getAttribute("helpHref"); //$NON-NLS-1$
			moreLink.setVisible(helpURL != null);
			descText.setText(content, true, false);
		} else {
			moreLink.setVisible(false);
			descText.setText("", false, false); //$NON-NLS-1$
		}
		form.reflow(true);
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	@Override
	public void init(IIntroPart introPart, IMemento memento) {
		// try to restore last state.
		input = getCachedInput(memento);

	}

	/**
	* Tries to create the last content part viewed, based on sample id.
	*
	* @param memento
	*/
	private String getCachedInput(IMemento memento) {
		if (memento == null)
			return null;
		return memento.getString(MEMENTO_SAMPLE_ID_ATT);

	}

	@Override
	public void saveState(IMemento memento) {
		String currentSampleId = input;
		if (input != null)
			memento.putString(MEMENTO_SAMPLE_ID_ATT, currentSampleId);

	}
}
