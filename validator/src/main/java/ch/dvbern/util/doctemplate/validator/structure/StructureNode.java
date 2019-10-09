package ch.dvbern.util.doctemplate.validator.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link StructureNode} represents a condition, iteration, document or a field in a doctemplate
 *
 * @author Raphael Gosteli
 */
public class StructureNode {

	private final String key;
	private final Type type;
	private Map<String, String> args;
	private List<StructureNode> nodes = null;

	/**
	 * Constructor for a StructureNode
	 *
	 * @param key  the key
	 * @param type the type
	 */
	public StructureNode(String key, Type type) {
		this.args = new HashMap<>();
		this.key = key;
		this.type = type;
	}

	/**
	 * Method for adding another {@link StructureNode} as a child
	 *
	 * @param n the child structure node
	 */
	public void addNodeElement(StructureNode n) {
		if (this.nodes == null) {
			this.nodes = new ArrayList<>();
		}
		this.nodes.add(n);
	}

	public String getKey() {
		return key;
	}

	public Type getType() {
		return type;
	}

	public List<StructureNode> getNodes() {
		return nodes;
	}

	public Map<String, String> getArgs() {
		return args;
	}

	/**
	 * Types of {@link StructureNode}
	 */
	public enum Type {
		DOCUMENT,
		IF,
		FIELD,
		WHILE;
	}
}
