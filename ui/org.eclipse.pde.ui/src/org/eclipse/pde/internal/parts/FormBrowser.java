
package org.eclipse.pde.internal.parts;

import org.eclipse.update.ui.forms.internal.engine.FormEngine;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.*;

public class FormBrowser {
	ScrolledComposite scomp;
	FormEngine engine;
	String text;
	FormWidgetFactory factory;
	
	public FormBrowser() {
	}

	public void createControl(Composite parent) {
		factory = new FormWidgetFactory(parent.getDisplay());
		scomp = new ScrolledComposite(parent, SWT.BORDER);
		scomp.setBackground(factory.getBackgroundColor());
		engine = factory.createFormEngine(scomp);
		engine.setMarginWidth(2);
		engine.setMarginHeight(2);
		engine.setHyperlinkSettings(factory.getHyperlinkHandler());
		scomp.setContent(engine);
     	scomp.addListener (SWT.Resize,  new Listener () {
			public void handleEvent (Event e) {
				updateSize();
			}
		});
		if (text!=null) loadText(text);
	}
	
	public Control getControl() {
		return scomp;
	}
	
	public void setText(String text) {
		this.text = text;
		loadText(text);
	}
	
	private void loadText(String text) {
		if (engine!=null) {
			String markup = "<form>"+text+"</form>";
			engine.load(markup, true, false);
			updateSize();
			engine.redraw();
			scomp.layout();
		}
	}
	private void updateSize() {
		Rectangle ssize = scomp.getClientArea();
		int swidth = ssize.width;
		Point size = engine.computeSize(swidth, SWT.DEFAULT, true);
		engine.setSize(size);
	}
}