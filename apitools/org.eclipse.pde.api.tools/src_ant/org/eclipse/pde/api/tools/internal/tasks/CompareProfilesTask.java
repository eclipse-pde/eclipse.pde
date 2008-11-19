/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;

/**
 * Ant task to compare API profiles.
 */
public class CompareProfilesTask extends CommonUtilsTask {
	
	private static final String REPORT_XML_FILE_NAME = "compare.xml"; //$NON-NLS-1$
	private static final String REFERENCE = "reference"; //$NON-NLS-1$
	private static final String CURRENT = "currentProfile"; //$NON-NLS-1$
	private static final String REFERENCE_PROFILE_NAME = "reference_profile"; //$NON-NLS-1$
	private static final String CURRENT_PROFILE_NAME = "current_profile"; //$NON-NLS-1$

	public void execute() throws BuildException {
		if (this.debug) {
			System.out.println("reference : " + this.referenceLocation); //$NON-NLS-1$
			System.out.println("profile to compare : " + this.profileLocation); //$NON-NLS-1$
			System.out.println("report location : " + this.reportLocation); //$NON-NLS-1$
		}
		if (this.referenceLocation == null
				|| this.profileLocation == null
				|| this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println("Missing arguments :"); //$NON-NLS-1$
			writer.print("reference location :"); //$NON-NLS-1$
			writer.println(this.referenceLocation);
			writer.print("current profile location :"); //$NON-NLS-1$
			writer.println(this.profileLocation);
			writer.print("report location :"); //$NON-NLS-1$
			writer.println(this.reportLocation);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		// create reference
		File referenceInstallDir = extractSDK(REFERENCE, this.referenceLocation);

		File profileInstallDir = extractSDK(CURRENT, this.profileLocation);

		// run the comparison
		// create profile for the reference
		IApiBaseline referenceProfile = createProfile(REFERENCE_PROFILE_NAME, getInstallDir(referenceInstallDir, REFERENCE), this.eeFileLocation);
		IApiBaseline currentProfile = createProfile(CURRENT_PROFILE_NAME, getInstallDir(profileInstallDir, CURRENT), this.eeFileLocation);
		
		IDelta delta = null;
		
		try {
			delta = ApiComparator.compare(referenceProfile, currentProfile, VisibilityModifiers.API);
		} finally {
			referenceProfile.dispose();
			currentProfile.dispose();
			deleteProfile(this.referenceLocation, REFERENCE);
			deleteProfile(this.profileLocation, CURRENT);
		}
		if (delta == null) {
			// an error occured during the comparison
			throw new BuildException(Messages.errorInComparison);
		}
		if (delta != ApiComparator.NO_DELTA) {
			// dump the report in the appropriate folder
			BufferedWriter writer = null;
			File outputDir = new File(this.reportLocation);
			if (!outputDir.exists()) {
				if (!outputDir.mkdirs()) {
					throw new BuildException(Messages.errorInComparison);
				}
			}
			File outputFile = new File(this.reportLocation, REPORT_XML_FILE_NAME);
			try {
				if (outputFile.exists()) {
					// delete the file
					// TODO we might want to customize it
					outputFile.delete();
				}
				writer = new BufferedWriter(new FileWriter(outputFile));
				DeltaXmlVisitor visitor = new DeltaXmlVisitor();
				delta.accept(visitor);
				writer.write(visitor.getXML());
				writer.flush();
			} catch (IOException e) {
				ApiPlugin.log(e);
			} catch (CoreException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}
}
