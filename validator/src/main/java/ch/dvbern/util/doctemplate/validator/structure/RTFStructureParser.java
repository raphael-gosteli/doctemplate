package ch.dvbern.util.doctemplate.validator.structure;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import ch.dvbern.lib.doctemplate.common.DocTemplateException;
import ch.dvbern.util.doctemplate.validator.structure.StructureNode.Type;
import net.sourceforge.rtf.RTFTemplate;
import net.sourceforge.rtf.UnsupportedRTFTemplate;
import net.sourceforge.rtf.document.RTFDocument;
import net.sourceforge.rtf.document.RTFElement;
import net.sourceforge.rtf.document.RTFEndBookmark;
import net.sourceforge.rtf.document.RTFField;
import net.sourceforge.rtf.document.RTFStartBookmark;
import net.sourceforge.rtf.helper.RTFTemplateBuilder;
import org.xml.sax.SAXException;

/**
 * The {@link RTFStructureParser} implements the {@link StructureParser} for a RTF doctemplate
 */
public class RTFStructureParser implements StructureParser {

	private static final String CONDITION_BEGIN = "IF_";
	private static final String CONDITION_END = "ENDIF_";
	private static final String ITERATION_BEGIN = "WHILE_";
	private static final String ITERATION_END = "ENDWHILE_";
	private static final String SORT_FIELD_PREFIX = "SORT_";
	private static final String ALTERNATE_SUFFIX = "_ALT";

	private final Stack<StructureNode> nodeStack;

	public RTFStructureParser() {
		this.nodeStack = new Stack<>();
	}

	@Override
	public Stack<StructureNode> parse(Reader reader) throws DocTemplateException, UnsupportedRTFTemplate,
			IOException, SAXException, DocTemplateStructureException {
		RTFTemplate rtfTemplate = RTFTemplateBuilder.newRTFTemplateBuilder().newRTFTemplate();
		rtfTemplate.setTemplate(reader);

		RTFDocument rtfDoc = rtfTemplate.transform();

		nodeStack.push(new StructureNode("Root", Type.DOCUMENT));
		parseTemplate(rtfDoc);

		return nodeStack;
	}

	/**
	 * The parseTemplate method parses/pushes the {@link RTFElement} to the {@link Stack<StructureNode>}
	 *
	 * @param rtfElement the rtf element (document)
	 * @throws DocTemplateException if the structure of the document is invalid
	 * @see ch.dvbern.lib.doctemplate.rtf.RTFMergeEngine#parseTemplate(RTFElement)
	 */
	private void parseTemplate(RTFElement rtfElement) throws DocTemplateException, DocTemplateStructureException {
		for (Object o : rtfElement.getElementList()) {

			if (o instanceof RTFElement) {
				if (o instanceof RTFField) {
					String key = ((RTFField) o).getName();
					if (key != null && !key.startsWith("$")) {
						StructureNode node = new StructureNode(key, Type.FIELD);
						nodeStack.peek().addNodeElement(node);
					}
				} else if (o instanceof RTFStartBookmark) {
					RTFStartBookmark bm = (RTFStartBookmark) o;
					String bmName = bm.getName();
					if (bmName.startsWith(CONDITION_BEGIN)) {
						String key = bmName.substring(CONDITION_BEGIN.length());
						StructureNode conditionNode = new StructureNode(key, Type.IF);
						nodeStack.peek().addNodeElement(conditionNode);
						nodeStack.push(conditionNode);
					} else if (bmName.startsWith(ITERATION_BEGIN)) {
						String key = bmName.substring(ITERATION_BEGIN.length());
						StructureNode iterationNode = new StructureNode(key, Type.WHILE);
						nodeStack.peek().addNodeElement(iterationNode);
						nodeStack.push(iterationNode);
					} else if (bmName.startsWith(SORT_FIELD_PREFIX)) {
						String key = bmName.substring(SORT_FIELD_PREFIX.length());

						int altPos = key.indexOf(ALTERNATE_SUFFIX);
						if (altPos > 0) {
							key = key.substring(0, altPos);
						}
						StructureNode ime = nodeStack.peek();
						if (ime.getType() == Type.WHILE) {
							ime.getArgs().put("sort", key);
						} else {
							throw new DocTemplateStructureException("no StructureNode of type ITERATION on "
									+ "parse stack");
						}
					} else if (!bmName.startsWith(CONDITION_END) && !bmName.startsWith(ITERATION_END)) {
						String s = bm.getRTFContentOfSimpleElement();
						StructureNode node = new StructureNode(s, null);
						nodeStack.peek().addNodeElement(node);
					}
				} else if (o instanceof RTFEndBookmark) {
					RTFEndBookmark bm = (RTFEndBookmark) o;
					String bmName = bm.getName();
					if (bmName.startsWith(CONDITION_END)) {
						if (nodeStack.size() > 1) {
							String key = bmName.substring(CONDITION_END.length());
							StructureNode lastCondition = nodeStack.lastElement();
							if (lastCondition.getKey().equals(key) && lastCondition.getType() == Type.IF) {
								nodeStack.pop();
							} else {
								throw new DocTemplateStructureException(String.format("Missing END%s for %s_%s",
										lastCondition.getType().toString(),
										lastCondition.getType().toString(), lastCondition.getKey()));
							}

						} else {
							throw new DocTemplateException("error.rtftemplate.invalid.structure");
						}
					} else if (bmName.startsWith(ITERATION_END)) {
						if (nodeStack.size() > 1) {
							String key = bmName.substring(ITERATION_END.length());
							StructureNode lastCondition = nodeStack.lastElement();
							if (lastCondition.getKey().equals(key) && lastCondition.getType() == Type.WHILE) {
								nodeStack.pop();
							} else {
								throw new DocTemplateStructureException(String.format("Missing END%s for %s_%s",
										lastCondition.getType().toString(),
										lastCondition.getType().toString(), lastCondition.getKey()));
							}

						} else {
							throw new DocTemplateException("error.rtftemplate.invalid.structure");
						}
					} else if (!bmName.startsWith(CONDITION_BEGIN) && !bmName.startsWith(ITERATION_BEGIN) && !bmName.startsWith(SORT_FIELD_PREFIX)) {
						String s = bm.getRTFContentOfSimpleElement();
						StructureNode node = new StructureNode(s, null);
						nodeStack.peek().addNodeElement(node);
					}
				} else {
					parseTemplate((RTFElement) o);
				}
			} else {
				String s = o.toString();
				StructureNode node = new StructureNode(s, null);
				nodeStack.peek().addNodeElement(node);
			}
		}
	}
}