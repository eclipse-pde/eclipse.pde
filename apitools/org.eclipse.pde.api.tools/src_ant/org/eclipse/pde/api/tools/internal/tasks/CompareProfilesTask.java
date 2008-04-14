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
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

public class CompareProfilesTask extends Task {
	
	private static final String REPORT_XML_FILE_NAME = "compare.xml"; //$NON-NLS-1$
	private static final String PLUGINS_FOLDER_NAME = "plugins"; //$NON-NLS-1$
	private static final String ECLIPSE_FOLDER_NAME = "eclipse"; //$NON-NLS-1$
	private static final String CVS_FOLDER_NAME = "CVS"; //$NON-NLS-1$
	private static final String REFERENCE = "reference"; //$NON-NLS-1$
	private static final String CURRENT = "currentProfile"; //$NON-NLS-1$
	private static final String REFERENCE_PROFILE_NAME = "reference_profile"; //$NON-NLS-1$
	private static final String CURRENT_PROFILE_NAME = "current_profile"; //$NON-NLS-1$

	private static final boolean DEBUG = true;

	String referenceLocation;
	String profileLocation;
	String reportLocation;
	String eeFileLocation;

	public void setProfile(String profileLocation) {
		this.profileLocation = profileLocation;
	}
	public void setReference(String referenceLocation) {
		this.referenceLocation = referenceLocation;
	}
	public void setReport(String reportLocation) {
		this.reportLocation = reportLocation;
	}
	
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
	}
	
	public void execute() throws BuildException {
		if (DEBUG) {
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
		// unzip reference
		File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		extractSDK(tempDir, REFERENCE, this.referenceLocation);

		extractSDK(tempDir, CURRENT, this.profileLocation);

		// run the comparison
		// create profile for the reference
		IApiProfile referenceProfile = createProfile(REFERENCE_PROFILE_NAME, getInstallDir(tempDir, REFERENCE), this.eeFileLocation);
		IApiProfile currentProfile = createProfile(CURRENT_PROFILE_NAME, getInstallDir(tempDir, CURRENT), this.eeFileLocation);
		
		IDelta delta = null;
		
		try {
			delta = ApiComparator.compare(referenceProfile, currentProfile, VisibilityModifiers.API);
		} finally {
			referenceProfile.dispose();
			currentProfile.dispose();
		}
		if (delta == null) {
			// an error occured during the comparison
			throw new BuildException("An error occured during the comparison"); //$NON-NLS-1$
		}
		if (delta != ApiComparator.NO_DELTA) {
			// dump the report in the appropriate folder
			BufferedWriter writer = null;
			File outputDir = new File(this.reportLocation);
			if (!outputDir.exists()) {
				if (!outputDir.mkdirs()) {
					throw new BuildException("An error occured during the comparison"); //$NON-NLS-1$
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
	private static void extractSDK(File tempDir, String dirName, String location) {
		File installDir = new File(tempDir, dirName);
		if (installDir.exists()) {
			// delta existing folder
			if (!Util.delete(installDir)) {
				throw new BuildException("Could not delete : " + installDir.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		if (!installDir.mkdirs()) {
			throw new BuildException("Could not create : " + installDir.getAbsolutePath()); //$NON-NLS-1$
		}

		try {
			Util.unzip(location, installDir.getAbsolutePath());
		} catch (IOException e) {
			throw new BuildException("Could not unzip SDK into : " + installDir.getAbsolutePath()); //$NON-NLS-1$
		}
	}
	
	private static String getInstallDir(File dir, String profileInstallName) {
		return new File(new File(new File(dir, profileInstallName), ECLIPSE_FOLDER_NAME), PLUGINS_FOLDER_NAME).getAbsolutePath();
	}

	private static IApiProfile createProfile(String profileName, String fileName, String eeFileLocation) {
		try {
			IApiProfile baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = Factory.newApiProfile(profileName);
			} else if (eeFileLocation != null) {
				baseline = Factory.newApiProfile(profileName, new File(eeFileLocation));
			} else {
				baseline = Factory.newApiProfile(profileName, Util.getEEDescriptionFile());
			}
			// create a component for each jar/directory in the folder
			File dir = new File(fileName);
			File[] files = dir.listFiles();
			List components = new ArrayList();
			for (int i = 0; i < files.length; i++) {
				File bundle = files[i];
				if (!bundle.getName().equals(CVS_FOLDER_NAME)) {
					// ignore CVS folder
					IApiComponent component = baseline.newApiComponent(bundle.getAbsolutePath());
					if(component != null) {
						components.add(component);
					}
				}
			}
			
			baseline.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			return baseline;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
}
