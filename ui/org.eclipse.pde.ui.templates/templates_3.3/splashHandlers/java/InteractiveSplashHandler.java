/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package $packageName$;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.splash.AbstractSplashHandler;

/**
 * @since 3.3
 * 
 */
public class InteractiveSplashHandler extends AbstractSplashHandler {
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.splash.AbstractSplashHandler#init(org.eclipse.swt.widgets.Shell)
	 */
	public void init(final Shell splash) {
		super.init(splash);
		splash.setLayout(new FillLayout());
		// make our composite inherit the splash background
		splash.setBackgroundMode(SWT.INHERIT_DEFAULT);
		
		final String[] username = new String[1], password = new String[1];
		final boolean[] loggedIn = new boolean[] { false };
		Composite panel = new Composite(splash, SWT.BORDER);
		panel.setLayout(new GridLayout(3, false));
		{
			Composite spanner = new Composite(panel, SWT.NONE);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.horizontalSpan = 3;
			spanner.setLayoutData(data);
		}
		{
			Label label = new Label(panel, SWT.NONE);
			label.setText("&User Name:"); //NON-NLS-1
			GridData data2 = new GridData();
			data2.horizontalIndent = 175;
			label.setLayoutData(data2);
			final Text text = new Text(panel, SWT.BORDER);
			text.addVerifyListener(new VerifyListener() {

				public void verifyText(VerifyEvent e) {
					username[0] = e.text;
				}
			});
			
			GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
			data.widthHint = 175;
			data.horizontalSpan = 2;
			text.setLayoutData(data);
			
			
		}
		{
			Label label = new Label(panel, SWT.NONE);
			label.setText("&Password:"); //NON-NLS-1
			GridData data2 = new GridData();
			data2.horizontalIndent = 175;
			label.setLayoutData(data2);			
			final Text text = new Text(panel, SWT.PASSWORD | SWT.BORDER);
			text.addVerifyListener(new VerifyListener() {

				public void verifyText(VerifyEvent e) {
					password[0] = e.text;
				}
			});
			GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
			data.widthHint = 175;
			data.horizontalSpan = 2;
			text.setLayoutData(data);
		}
		{
			Label label = new Label(panel, SWT.NONE);
			label.setVisible(false);
		}
		{
			Button logIn = new Button(panel, SWT.PUSH);
			logIn.setText("OK"); //NON-NLS-1
			GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
			data.widthHint = 80;
			data.verticalIndent = 10;
			logIn.setLayoutData(data);
			logIn.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetSelected(SelectionEvent e) {
					if (username[0] != null
							&& !username[0].equals("") && password[0] != null && !password[0].equals("")) { //NON-NLS-1 //NON-NLS-2
						loggedIn[0] = true;
					} else {
						MessageDialog
								.openError(
										splash,
										"Could not Authenticate", "There was a problem logging in."); //NON-NLS-1//NON-NLS-2
					}
				}

			});
		}
		{
			Button quit = new Button(panel, SWT.PUSH);
			quit.setText("Cancel"); //NON-NLS-1
			GridData data = new GridData(SWT.NONE, SWT.NONE, false, false);
			data.widthHint = 80;	
			data.verticalIndent = 10;
			quit.setLayoutData(data);
			quit.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetSelected(SelectionEvent e) {
					// quick and dirty exit. If people really need this feature
					// we can talk about new API to abort startup down the road
					splash.getDisplay().close();
					System.exit(0);
				}

			});
		}
		// layout the new controls
		splash.layout(true);

		// spin the loop until we're logged in
		while (loggedIn[0] == false) {
			while (splash.getDisplay().readAndDispatch())
				;
		}
	}
}
