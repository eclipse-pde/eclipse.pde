package org.eclipse.pde.ui.internal.samples;
import java.io.*;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.launcher.RuntimeWorkbenchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorPart;
/**
 * @see EditorPart
 */
public class SampleEditor extends EditorPart {
	private FormToolkit toolkit;
	private ScrolledForm form;
	private FormText descText;
	private FormText instText;
	private ILaunchShortcut defaultShortcut;
	/**
	 *  
	 */
	public SampleEditor() {
		defaultShortcut = new RuntimeWorkbenchShortcut();
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	/**
	 * @see EditorPart#createPartControl
	 */
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		form.setBackgroundImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FORM_BANNER));
		Properties properties = loadContent();
		form.setText(properties.getProperty("name"));
		TableWrapLayout layout = new TableWrapLayout();
		layout.verticalSpacing = 10;
		layout.topMargin = 10;
		layout.bottomMargin = 10;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		form.getBody().setLayout(layout);
		
		final String launcher = properties.getProperty("launcher");
		final String launchTarget = properties.getProperty("launchTarget");
		
		descText = toolkit.createFormText(form.getBody(), true);
		descText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		String desc = properties.getProperty("description");
		String content = "<form>"+(desc!=null?desc:"")+"</form>";
		descText.setText(content, true, false);
		final String helpURL = properties.getProperty("helpHref");
		if (helpURL!=null) {
			Hyperlink moreLink = toolkit.createHyperlink(form.getBody(), "Read More", SWT.NULL);
			moreLink.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					WorkbenchHelp.displayHelpResource(helpURL);
				}
			});
		}
		instText = toolkit.createFormText(form.getBody(), true);
		instText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		StringBuffer buf = new StringBuffer();
		buf.append("<form>");
		buf.append("<p><b>What you can do with the sample</b></p>");
		buf.append("<li>Browse the source code in the workspace.</li>");
		buf.append("<li>When ready, <a href=\"run\">run the sample</a> and follow instructions in the <img href=\"help\"/><a href=\"help\">help document.</a></li>");
		buf.append("<li>Later on, you can re-run the sample by pressing the <img href=\"run\"/><b>Run</b> icon on the tool bar.</li>");
		buf.append("<li>If you place breakpoints in the code, you can <a href=\"debug\">debug it.</a></li>");
		buf.append("<li>Later on, you can debug the sample by pressing the <img href=\"debug\"/><b>Debug</b> icon on the tool bar.</li>");
		buf.append("</form>");
		instText.setText(buf.toString(), true, false);
		instText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				Object href = e.getHref();
				if (href.equals("help")) {
					WorkbenchHelp.displayHelpResource(helpURL);
				}
				else if (href.equals("run")) {
					doRun(launcher, launchTarget, false);
				}
				else if (href.equals("debug")) {
					doRun(launcher, launchTarget, true);
				}
			}
		});
		instText.setImage("run", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_RUN_EXC));
		instText.setImage("debug", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DEBUG_EXC));
		instText.setImage("help", PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK));
	}
	
	private void doRun(String launcher, String target, final boolean debug) {
		ILaunchShortcut shortcut = defaultShortcut;
		final ISelection selection;
		if (target!=null) {
			selection = new StructuredSelection();
		}
		else
			selection = new StructuredSelection();
		final ILaunchShortcut fshortcut = shortcut;
		BusyIndicator.showWhile(form.getDisplay(), new Runnable() {
			public void run() {
				fshortcut.launch(selection, debug?ILaunchManager.DEBUG_MODE:ILaunchManager.RUN_MODE);
			}
		});
	}
	
	private Properties loadContent() {
		IStorageEditorInput input = (IStorageEditorInput)getEditorInput();
		Properties properties = new Properties();
		try {
			IStorage storage = input.getStorage();
			InputStream is = storage.getContents();
			properties.load(is);
			is.close();
		}
		catch (IOException e) {
			PDEPlugin.logException(e);
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return properties;
	}
	
	public void dispose() {
		toolkit.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();		
	}
	/**
	 * @see EditorPart#setFocus
	 */
	public void setFocus() {
		form.setFocus();
	}
	/**
	 * @see EditorPart#doSave
	 */
	public void doSave(IProgressMonitor monitor) {
	}
	/**
	 * @see EditorPart#doSaveAs
	 */
	public void doSaveAs() {
	}
	/**
	 * @see EditorPart#isDirty
	 */
	public boolean isDirty() {
		return false;
	}
	/**
	 * @see EditorPart#isSaveAsAllowed
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	/**
	 * @see EditorPart#init
	 */
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}
}