/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.tests.macro;

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