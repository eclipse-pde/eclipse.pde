package org.eclipse.pde.internal.core.iproduct;

public interface ISplashInfo extends IProductObject {
	
	public static final String P_LOCATION = "location";
	
	void setLocation(String location);
	
	String getLocation();
	
}
