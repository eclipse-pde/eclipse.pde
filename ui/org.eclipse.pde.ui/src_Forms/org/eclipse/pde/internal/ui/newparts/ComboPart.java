/*
 * Created on Jan 31, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.newparts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class ComboPart {
	private Control combo;
	/**
	 *  
	 */
	public ComboPart() {
	}
	public void addSelectionListener(SelectionListener listener) {
		if (combo instanceof Combo)
			((Combo) combo).addSelectionListener(listener);
		else
			((CCombo) combo).addSelectionListener(listener);
	}
	public void createControl(Composite parent, FormToolkit toolkit, int style) {
		if (toolkit.getBorderStyle() == SWT.BORDER)
			combo = new Combo(parent, style | SWT.BORDER);
		else
			combo = new CCombo(parent, style | SWT.FLAT);
		toolkit.adapt(combo, true, true);
	}
	public Control getControl() {
		return combo;
	}
	public int getSelectionIndex() {
		if (combo instanceof Combo)
			return ((Combo) combo).getSelectionIndex();
		else
			return ((CCombo) combo).getSelectionIndex();
	}
	public void add(String item) {
		if (combo instanceof Combo)
			((Combo) combo).add(item);
		else
			((CCombo) combo).add(item);
	}
	public void select(int index) {
		if (combo instanceof Combo)
			((Combo) combo).select(index);
		else
			((CCombo) combo).select(index);
	}
	public void setText(String text) {
		if (combo instanceof Combo)
			((Combo) combo).setText(text);
		else
			((CCombo) combo).setText(text);
	}
}