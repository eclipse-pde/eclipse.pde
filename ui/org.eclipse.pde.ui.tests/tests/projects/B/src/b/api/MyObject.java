package b.api;

public class MyObject extends c.api.MyObject implements d.api.Processor {

	@Override
	public Object process(String input) {
		return new d.api.MyObject(input);
	}

	public d.api.MyObject processData(d.api.MyObject input) {
		if (input != null) {
			return new d.api.MyObject(input.getData());
		}
		return new d.api.MyObject();
	}

	public g.api.MyObject createService() {
		return new g.api.MyObject();
	}
}
