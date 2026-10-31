package p;
public interface Base extends Added {
	@Override
	default Object getTable() {
		return null;
	}
}
