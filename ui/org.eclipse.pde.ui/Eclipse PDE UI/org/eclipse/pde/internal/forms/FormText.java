package org.eclipse.pde.internal.forms;

import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import java.util.*;

public class FormText {
	private Text text;
	private String value;
	private boolean dirty;
	private Vector listeners=new Vector();

public FormText(Text text) {
	this.text = text;
	this.value = text.getText();
	addListeners();
}
public void addFormTextListener(IFormTextListener listener) {
	listeners.addElement(listener);
}
private void addListeners() {
	text.addKeyListener(new KeyAdapter() {
		public void keyReleased(KeyEvent e) {
			keyReleaseOccured(e);
		}
	});
	text.addModifyListener(new ModifyListener() {
		public void modifyText(ModifyEvent e) {
			editOccured(e);
		}
	});
	text.addFocusListener (new FocusAdapter() {
		public void focusLost(FocusEvent e) {
			if (dirty) commit();
		}
	});
}
public void commit() {
	if (dirty) {
		value = text.getText();
		//notify
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			((IFormTextListener) iter.next()).textValueChanged(this);
		}
	}
	dirty = false;
}
protected void editOccured(ModifyEvent e) {
	dirty = true;
}
public Text getControl() {
	return text;
}
public java.lang.String getValue() {
	return value;
}
public boolean isDirty() {
	return dirty;
}
protected void keyReleaseOccured(KeyEvent e) {
	if (e.character == '\r') {
		// commit value
		if (dirty) commit();
	}
	else if (e.character == '\u001b') { // Escape character
		text.setText(value); // restore old
		dirty= false;
	}
}
public void removeFormTextListener(IFormTextListener listener) {
	listeners.removeElement(listener);
}
public void setDirty(boolean newDirty) {
	dirty = newDirty;
}
public void setValue(String value) {
	if (text!=null) text.setText(value);
}
}
