package c.api;

public class MyObject implements Configurable {
	private Object config;
	private boolean configured;

	@Override
	public void configure(Object config) {
		this.config = config;
		this.configured = true;
	}

	@Override
	public boolean isConfigured() {
		return configured;
	}

	public Object getConfig() {
		return config;
	}
}
