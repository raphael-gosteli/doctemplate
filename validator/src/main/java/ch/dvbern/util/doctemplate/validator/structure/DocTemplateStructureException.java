package ch.dvbern.util.doctemplate.validator.structure;

import ch.dvbern.lib.doctemplate.common.DocTemplateException;

/**
 * This {@link DocTemplateException} gets thrown when the structure of a doctemplate is invalid
 *
 * @author Raphael Gosteli
 */
public class DocTemplateStructureException extends DocTemplateException {

	private static final long serialVersionUID = 8069963983862377957L;

	/**
	 * Constructor for the {@link DocTemplateException}
	 *
	 * @param errorCode the error code
	 * @param args      the exception arguments
	 */
	public DocTemplateStructureException(String errorCode, Object... args) {
		super("Invalid structure: " + errorCode, args);
	}
}
