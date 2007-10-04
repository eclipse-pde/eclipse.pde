package org.eclipse.pde.internal.build.site.compatibility;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.site.BuildTimeFeatureFactory;
import org.eclipse.pde.internal.build.site.BuildTimeSite;

public class FeatureReference {
	private BuildTimeSite site;
	private String urlString;
	private URL url;
	private Feature feature;

	public void setSiteModel(BuildTimeSite site) {
		this.site = site;
	}

	public void setURLString(String externalForm) {
		urlString = externalForm;
	}

	public Feature getFeature() throws CoreException {
		if (feature != null)
			return feature;
		
		if (site != null)
			feature = site.createFeature(getURL());
		else {
			BuildTimeFeatureFactory factory = BuildTimeFeatureFactory.getInstance();
			feature = factory.createFeature(getURL(), null);
		}
		return feature;
	}

	public URL getURL() {
		if (url == null)
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return url;
	}
}
