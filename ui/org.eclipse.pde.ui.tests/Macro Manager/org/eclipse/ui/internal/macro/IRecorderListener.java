/*
 * Created on Nov 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipse.ui.internal.macro;

/**
 * @author dejan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IRecorderListener {
	int STOP = 1;
	int INDEX = 2;
	void recordingStarted();
	void recordingStopped();
/**
 * Called when the user pressed Ctrl+Shift+F10 (index)
 * or Ctrl+Shift+F11 (stop) to interrupt
 * the recording process. Clients may use this event
 * to insert named indexes, stop the recording etc.
 * @param type <code>STOP</code> or <code>INDEX</code>
 */
	void recordingInterrupted(int type);
}