/*
 * Created on Feb 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.ui.forms.events.IHyperlinkListener;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface IFormEntryListener extends IHyperlinkListener {
/**
 * The user clicked on the text control and focus was
 * transfered to it.
 * @param entry
 */
	void focusGained(FormEntry entry);
/**
 * The user changed the text in the text control of the entry.
 * @param entry
 */
	void textDirty(FormEntry entry);
/**
 * The value of the entry has been changed to be the text
 * in the text control (as a result of 'commit' action).
 * @param entry
 */
	void textValueChanged(FormEntry entry);
/**
 * The user pressed the 'Browse' button for the entry.
 * @param entry
 */
	void browseButtonSelected(FormEntry entry);
}
