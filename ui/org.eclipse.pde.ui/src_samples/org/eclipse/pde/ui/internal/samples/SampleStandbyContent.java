/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.internal.samples;
import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.RuntimeWorkbenchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.IStandbyContentPart;
import org.eclipse.ui.part.ISetSelectionTarget;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
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
	/**
	 *  
	 */
	public SampleStandbyContent() {
		defaultShortcut = new RuntimeWorkbenchShortcut();
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#createPartControl(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
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
		descText.setText("", false, false);
		moreLink = toolkit.createHyperlink(form.getBody(), "Read More",
				SWT.NULL);
		moreLink.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				if (helpURL != null)
					WorkbenchHelp.displayHelpResource(helpURL);
			}
		});
		instText = toolkit.createFormText(form.getBody(), true);
		instText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		StringBuffer buf = new StringBuffer();
		buf.append("<form>");
		buf.append("<p><b>What you can do with the sample</b></p>");
		buf
				.append("<li><a href=\"browse\">Browse the source code</a> in the workspace.</li>");
		buf
				.append("<li>When ready, <a href=\"run\">run the sample</a> and follow instructions in the <img href=\"help\"/><a href=\"help\">help document.</a></li>");
		buf
				.append("<li>Later on, you can re-run the sample by pressing the <img href=\"run\"/><b>Run</b> icon on the tool bar.</li>");
		buf
				.append("<li>If you place breakpoints in the code, you can <a href=\"debug\">debug it.</a></li>");
		buf
				.append("<li>Later on, you can debug the sample by pressing the <img href=\"debug\"/><b>Debug</b> icon on the tool bar.</li>");
		buf.append("</form>");
		instText.setText(buf.toString(), true, false);
		instText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				Object href = e.getHref();
				if (href.equals("help")) {
					WorkbenchHelp.displayHelpResource(helpURL);
				} else if (href.equals("browse")) {
					doBrowse();
				} else if (href.equals("run")) {
					doRun(launcher, launchTarget, false);
				} else if (href.equals("debug")) {
					doRun(launcher, launchTarget, true);
				}
			}
		});
		instText.setImage("run", PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_RUN_EXC));
		instText.setImage("debug", PDEPlugin.getDefault().getLabelProvider()
				.get(PDEPluginImages.DESC_DEBUG_EXC));
		instText.setImage("help", PlatformUI.getWorkbench().getSharedImages()
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
		BusyIndicator.showWhile(form.getDisplay(), new Runnable() {
			public void run() {
				fshortcut.launch(selection, debug
						? ILaunchManager.DEBUG_MODE
						: ILaunchManager.RUN_MODE);
			}
		});
	}
	private void doBrowse() {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		ISetSelectionTarget target = findTarget();
		if (target == null)
			return;
		String sid = sample.getAttribute("id");
		if (sid == null)
			return;
		ArrayList items = new ArrayList();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (!project.exists() || !project.isOpen())
				continue;
			IFile pfile = project.getFile("sample.properties");
			if (pfile.exists()) {
				try {
					InputStream is = pfile.getContents();
					Properties prop = new Properties();
					prop.load(is);
					is.close();
					String id = prop.getProperty("id");
					if (id != null && id.equals(sid)) {
						//match
						IResource res = findSelectReveal(project, prop
								.getProperty("projectName"));
						if (res != null)
							items.add(res);
					}
				} catch (IOException e) {
					PDEPlugin.logException(e);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
		if (items.size() > 0)
			target.selectReveal(new StructuredSelection(items));
	}
	private ISetSelectionTarget findTarget() {
		String id = sample.getAttribute("targetViewId");
		if (id == null)
			return null;
		IViewPart view = PDEPlugin.getActivePage().findView(id);
		if (view == null || !(view instanceof ISetSelectionTarget))
			return null;
		return (ISetSelectionTarget) view;
	}
	private IResource findSelectReveal(IProject project, String originalName) {
		IConfigurationElement[] projects = sample.getChildren("project");
		for (int i = 0; i < projects.length; i++) {
			if (originalName.equals(projects[i].getAttribute("name"))) {
				String path = projects[i].getAttribute("selectReveal");
				if (path == null)
					continue;
				IResource res = project.findMember(path);
				if (res.exists())
					return res;
			}
		}
		return null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#getControl()
	 */
	public Control getControl() {
		return form;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#init(org.eclipse.ui.intro.IIntroPart)
	 */
	public void init(IIntroPart introPart) {
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#setInput(java.lang.Object)
	 */
	public void setInput(Object input) {
		String sampleId = input.toString();
		IConfigurationElement[] samples = Platform.getExtensionRegistry()
				.getConfigurationElementsFor("org.eclipse.pde.ui.samples");
		for (int i = 0; i < samples.length; i++) {
			IConfigurationElement sample = samples[i];
			String id = sample.getAttribute("id");
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
		String title = sample != null ? sample.getAttribute("name") : "";
		form.setText(title);
		if (sample != null) {
			launcher = sample.getAttribute("launcher");
			launchTarget = sample.getAttribute("launchTarget");
		} else {
			launcher = null;
			launchTarget = null;
		}
		IConfigurationElement[] descConfig = sample != null ? sample
				.getChildren("description") : null;
		if (descConfig.length == 1) {
			String desc = descConfig[0].getValue();
			String content = "<form>" + (desc != null ? desc : "") + "</form>";
			helpURL = descConfig[0].getAttribute("helpHref");
			moreLink.setVisible(helpURL != null);
			descText.setText(content, true, false);
		} else {
			moreLink.setVisible(false);
			descText.setText("", false, false);
		}
		form.reflow(true);
	} /*
	   * (non-Javadoc)
	   * 
	   * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#setFocus()
	   */
	public void setFocus() {
		form.setFocus();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.intro.internal.parts.IStandbyContentPart#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
}