package org.eclipse.pde.internal.forms;

import org.eclipse.swt.widgets.*;

public interface IFormPage {

boolean becomesInvisible(IFormPage newPage);
void becomesVisible(IFormPage previousPage);
void createControl(Composite parent);
Control getControl();
String getLabel();
String getTitle();
boolean isSource();
boolean isVisible();
}
