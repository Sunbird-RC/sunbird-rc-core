package io.opensaber.registry.util;

public class RecordIdentifier {

	private final static String SEPARATOR = "-";
	private final static String REGEX_RECORDID = "[0-9a-z]*-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private String shardLabel;
	private String uuid;

	public RecordIdentifier(String shardLabel, String uuid) {
		this.shardLabel = shardLabel;
		this.uuid = uuid;
	}

	public String getShardLabel() {
		return shardLabel;
	}

	public String getUuid() {
		return uuid;
	}

	private static String format(String shardLabel, String uuid) {
		return shardLabel + SEPARATOR + uuid;
	}

	/**
	 * Returns spring representation of RecordIdentifier. Example: shard
	 * SEPARATOR uuid
	 */
	@Override
	public String toString() {
		String result;
		if (this.getShardLabel() != null && !this.getShardLabel().isEmpty()) {
			result = format(this.getShardLabel(), this.getUuid());
		} else {
			result = this.getUuid();
		}
		return result;

	}

	/**
	 * Creates RecordIdentifier object from a string representation
	 * 
	 * @param input
	 * @return
	 */
	public static RecordIdentifier parse(String input) {
		return new RecordIdentifier(getLabel(input), getUUID(input));
	}

	/**
	 * Return a value only when input form is shard SEPARATOR uuid
	 * 
	 * @param input
	 * @return
	 */
	private static String getLabel(String input) {
		String shardLabel = null;
		if (isValid(input))
			shardLabel = input.substring(0, input.indexOf(SEPARATOR));
		return shardLabel;
	}

	private static String getUUID(String input) {
		String uuid = input;
		if (isValid(input)) {
			uuid = input.substring(input.indexOf(SEPARATOR) + 1, input.length());
		}
		return uuid;
	}

	private static boolean isValid(String uuid) {
		return uuid.matches(REGEX_RECORDID);
	}

	public static final String getSeparator() {
		return SEPARATOR;
	}

}
