package org.eclipse.pde.internal.forms;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Label;

public interface IHyperlinkListener {

public void linkActivated(Control linkLabel);
public void linkEntered(Control linkLabel);
public void linkExited(Control linkLabel);
}
