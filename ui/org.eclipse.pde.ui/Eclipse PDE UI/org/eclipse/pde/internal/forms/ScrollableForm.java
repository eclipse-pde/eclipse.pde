package org.eclipse.pde.internal.forms;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;

public class ScrollableForm extends Form {
	private Composite container;
	private boolean verticalFit;
	private boolean scrollable=true;

public ScrollableForm() {
}
public Control createControl(Composite parent) {
	container = createParent(parent);
	Control formControl = super.createControl(container);
	if (container instanceof ScrolledComposite) {
		ScrolledComposite sc = (ScrolledComposite)container;
		sc.setContent(formControl);
		/*
		Point formSize = formControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		sc.setMinWidth(formSize.x);
		sc.setMinHeight(formSize.y);
		*/
	}
	GridData gd = new GridData(GridData.FILL_BOTH);
	formControl.setLayoutData(gd);
	container.setBackground(formControl.getBackground());
	return container;
}
protected Composite createParent(Composite parent) {
	Composite result = null;
	if (isScrollable()) {
		ScrolledComposite scomp =
			new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		if (isVerticalFit()) {
			scomp.setExpandHorizontal(true);
			scomp.setExpandVertical(true);
		}
		result = scomp;
	} else {
		result = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		result.setLayout(layout);
	}
	return result;
}
public boolean isScrollable() {
	return scrollable;
}
public boolean isVerticalFit() {
	return verticalFit;
}
public void setScrollable(boolean newScrollable) {
	scrollable = newScrollable;
}
public void setVerticalFit(boolean newVerticalFit) {
	verticalFit = newVerticalFit;
}
public void update() {
	super.update();
	if (container instanceof ScrolledComposite) {
		updateScrolledComposite();
	} else {
		container.layout(true);
	}
}
public void updateScrollBars() {
	if (container instanceof ScrolledComposite) {
		updateScrolledComposite();
	}
}
public void updateScrolledComposite() {
	ScrolledComposite sc = (ScrolledComposite) container;
	Control formControl = getControl();
	Point newSize = formControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	formControl.setSize(newSize);
	sc.setMinWidth(newSize.x);
	sc.setMinHeight(newSize.y);
}
}
