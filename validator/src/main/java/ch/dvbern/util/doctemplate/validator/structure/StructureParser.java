package ch.dvbern.util.doctemplate.validator.structure;

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

import ch.dvbern.lib.doctemplate.common.DocTemplateException;
import net.sourceforge.rtf.UnsupportedRTFTemplate;
import org.xml.sax.SAXException;

/**
 * Interface for a doctemplate {@link StructureParser} which parses the different fields in {@link StructureNode}'s
 */
public interface StructureParser {

	/**
	 * This method parses a reader (RTF) into a Stack of {@link StructureNode}
	 *
	 * @param reader the file/input reader
	 * @return the stack of nodes
	 * @throws DocTemplateException          wrong doctemplate format
	 * @throws UnsupportedRTFTemplate        unsupported rtf base template
	 * @throws IOException                   reader exception
	 * @throws SAXException                  rtf parsing error
	 * @throws DocTemplateStructureException wrong/invalid doctemplate structure
	 */
	Stack<StructureNode> parse(Reader reader) throws DocTemplateException, UnsupportedRTFTemplate,
			IOException, SAXException, DocTemplateStructureException;
}
