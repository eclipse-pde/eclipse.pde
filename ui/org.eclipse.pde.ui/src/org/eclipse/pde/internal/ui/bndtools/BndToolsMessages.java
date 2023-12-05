package org.eclipse.pde.internal.ui.bndtools;

import org.eclipse.osgi.util.NLS;

public class BndToolsMessages extends NLS {
	private static final String BUNDLE_NAME = BndToolsMessages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String RepositoryTargetLocationPage_AddBndRepository;
	public static String RepositoryTargetLocationPage_AddBndRepositoryContainer;
	public static String RepositoryTargetLocationPage_Contents;
	public static String RepositoryTargetLocationPage_Location;
	public static String RepositoryTargetLocationPage_Repository;
	public static String RepositoryTargetLocationPage_ToBeAdded;
	public static String RepositoryTargetLocationWizard_BndRepositoryTargetLocation;
	public static String RunDescriptorTargetLocationPage_AddBndRunDescriptor;
	public static String RunDescriptorTargetLocationPage_AddBndRunDescriptorContainer;
	public static String RunDescriptorTargetLocationPage_RunBundles;
	public static String RunDescriptorTargetLocationPage_RunDescriptor;
	public static String RunDescriptorTargetLocationPage_Select;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, BndToolsMessages.class);
	}

	private BndToolsMessages() {
	}
}
