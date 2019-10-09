package ch.dvbern.util.doctemplate.validator;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import ch.dvbern.lib.doctemplate.common.DocTemplateException;
import ch.dvbern.util.doctemplate.validator.structure.RTFStructureParser;
import ch.dvbern.util.doctemplate.validator.structure.StructureNode;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import net.sourceforge.rtf.UnsupportedRTFTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Main class for the doctemplate validator cli/gui
 *
 * @author Raphael Gosteli
 */
public class Validator extends Application {

	/* Logger */
	private static final Log LOG = LogFactory.getLog(Validator.class);

	/* cli arguments */
	private static final String NO_GUI_ARG = "--no-gui";
	private static final Collection<String> SUPPORTED_FILE_EXTENSIONS = Collections.singletonList(".rtf");
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile(" ");
	private static final Pattern BACKSLASH_PATTERN = Pattern.compile(
			"\\\\");
	/* fields */
	private static boolean guiEnabled = true;
	private static File file = null;
	/* javafx objects */
	private static Stage stage = null;
	private static BorderPane root = null;
	private static Stack<StructureNode> structureNodeStack = null;
	private static TreeItem<String> rootTreeItem = null;
	private static TextArea outputTextArea = null;

	/**
	 * Entry point for the validator
	 *
	 * @param args the commandline arguments
	 */
	public static void main(String[] args) {

		parseArguments(args);

		/* launch the application in gui or cli mode */
		if (guiEnabled) {
			launch(args);
		} else {
			/* file "validation" */
			if (file.exists() && file.isFile()) {
				validateDoctemplate(file);
			} else {
				LOG.error("Specified file '" + file.getPath() + "' does not exist!");
				System.exit(-1);
			}
		}
	}

	private static void parseArguments(String[] args) {
		for (String arg : args) {
			if (arg.equalsIgnoreCase(NO_GUI_ARG)) {
				guiEnabled = false;
			} else {
				/* check if its a supported file extension */
				for (String supportedFileExtension : SUPPORTED_FILE_EXTENSIONS) {
					if (arg.endsWith(supportedFileExtension)) {
						file = new File(arg);
						break;
					}
				}
			}
		}
	}

	private static void validateDoctemplate(File f) {
		resetOutputTextArea();
		try {
			FileInputStream fileInputStream = new FileInputStream(f);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, UTF_8));

			RTFStructureParser structureParser = new RTFStructureParser();

			/* parse the doctemplate */
			structureNodeStack =
					structureParser.parse(bufferedReader);

			if (structureNodeStack.size() == 1) {
				StructureNode rootStructureNode = structureNodeStack.get(0);
				if (guiEnabled) {
					/* update the tree view */
					rootTreeItem.setValue(rootStructureNode.getKey());
					rootTreeItem.getChildren().clear();
					listNodes(rootStructureNode.getNodes(), rootTreeItem);
				}
			}

			log("Structure valid!", false);
		} catch (SAXException | IOException | UnsupportedRTFTemplate | DocTemplateException e) {
			LOG.error(e);
			log(e.getMessage(), true);
		}
	}

	private static void listNodes(List<StructureNode> nodes, TreeItem<String> parentTreeItem) {
		for (StructureNode node : nodes) {

			/* tree item value string builder */
			StringBuilder valueBuilder = new StringBuilder();

			if (node.getType() != null) {

				TreeItem<String> treeItem = new TreeItem<>();

				switch (node.getType()) {
				case DOCUMENT:
					valueBuilder.append("Document");
					break;
				case IF:
					valueBuilder.append("If");
					treeItem.setGraphic(new ImageView(new Image("if.png")));
					break;
				case FIELD:
					valueBuilder.append("Field");
					treeItem.setGraphic(new ImageView(new Image("field.png")));
					break;
				case WHILE:
					valueBuilder.append("Iteration");
					treeItem.setGraphic(new ImageView(new Image("iteration.png")));
					break;
				}

				valueBuilder.append(' ').append(node.getKey());

				treeItem.setValue(valueBuilder.toString());

				parentTreeItem.getChildren().add(treeItem);

				/* start recursion if the node has child nodes */
				if (node.getNodes() != null && !node.getNodes().isEmpty()) {
					listNodes(node.getNodes(), treeItem);
				}
			}

		}
	}

	private static void resetOutputTextArea() {
		outputTextArea.setText("");
		outputTextArea.setStyle("-fx-text-fill: #008000;");
	}

	private static void log(String s, boolean exception) {
		if (exception) {
			outputTextArea.setStyle("-fx-text-fill: #ff0000;");
		}
		outputTextArea.setText(s);
	}

	/**
	 * This method gets called if the application runs in gui mode
	 *
	 * @param primaryStage the entry point stage
	 */
	@Override
	public void start(Stage primaryStage) {
		stage = primaryStage;
		constructUI();

		/* stage initialization */
		primaryStage.setTitle("doctemplate - Validator");
		primaryStage.setScene(new Scene(root, 800, 600));
		primaryStage.show();
	}

	private void constructUI() {
		root = new BorderPane();

		/* tree view */
		rootTreeItem = new TreeItem<>("Root");
		TreeView<String> treeView = new TreeView<>(rootTreeItem);

		/* output */
		outputTextArea = new TextArea();
		outputTextArea.setEditable(false);
		outputTextArea.setStyle("-fx-text-fill: #008000;");
		outputTextArea.setStyle("-fx-font-family: Consolas, monospace;");

		root.setTop(getMenuBar());
		root.setLeft(treeView);
		root.setCenter(outputTextArea);
	}

	private Node getMenuBar() {

		Menu fileMenu = new Menu("File");

		/* open file menu item */
		MenuItem openFileItem = new MenuItem("Open");
		openFileItem.setAccelerator(KeyCombination.valueOf("Ctrl+N"));
		openFileItem.setOnAction(event -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Choose doctemplate document");

			for (String supportedFileExtension : SUPPORTED_FILE_EXTENSIONS) {
				fileChooser.getExtensionFilters().add(new ExtensionFilter(supportedFileExtension.substring(1) + ' '
						+ "doctemplate",
						'*' + supportedFileExtension));
			}

			file = fileChooser.showOpenDialog(stage);
			if (file != null && file.canRead()) {
				validateDoctemplate(file);
			}
		});

		/* exit menu item */
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.setOnAction(event -> {
			System.exit(0);
		});

		fileMenu.getItems().add(openFileItem);
		fileMenu.getItems().add(exitItem);

		/* generate menu */
		Menu generateMenu = new Menu("Generate");

		MenuItem navigationDocumentItem = new MenuItem("Navigation document");
		navigationDocumentItem.setAccelerator(KeyCombination.valueOf("Ctrl+G"));
		navigationDocumentItem.setOnAction(event -> {
			handleNavigationDocumentGeneration();
		});

		generateMenu.getItems().add(navigationDocumentItem);

		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().add(fileMenu);
		menuBar.getMenus().add(generateMenu);

		return menuBar;
	}

	private void handleNavigationDocumentGeneration() {
		/* check if the required fields are provided */
		if (file != null && structureNodeStack != null) {
			if (structureNodeStack.size() == 1) {

				StructureNode rootStructureNode = structureNodeStack.get(0);

				try {
					/* create temp file */
					File navigationFile = File.createTempFile(String.format("%s_navigation", file.getName()),
							".rtf");
					navigationFile.deleteOnExit();

					FileOutputStream fileOutputStream = new FileOutputStream(navigationFile);

					String doctemplateUrl = getFileProtocolUrl(file);

					/* basic rtf template */
					String rtfTemplate = "{\\rtf1\\ansi\\deff0 {\\fonttbl {\\f0 Arial;}}\n"
							+ "\\f0\\fs32 %s }";

					/* get the hyperlink list */
					rtfTemplate = String.format(rtfTemplate, getRtfLinkList(rootStructureNode.getNodes(), 0,
							doctemplateUrl));

					fileOutputStream.write(rtfTemplate.getBytes(UTF_8));
					fileOutputStream.close();

					/* open the document using the default file handler if provided */
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().browse(new URL(getFileProtocolUrl(navigationFile)).toURI());
					}

				} catch (IOException | URISyntaxException e) {
					LOG.error(e);
				}

			}
		}
	}

	private String getRtfLinkList(List<StructureNode> structureNodes, int indent, String url) {

		/* rtf hyperlink template */
		String hyperlinkTemplate = "{\\field{\\*\\fldinst HYPERLINK \"%s\" \\\\l \"%s\"}{\\fldrslt %s}}";

		StringBuilder rtfLinkListBuilder = new StringBuilder();
		for (StructureNode structureNode : structureNodes) {

			if (structureNode.getType() != null) {

				StringBuilder indentString = new StringBuilder();
				for (int i = 0; i < indent; i++) {
					indentString.append("  ");
				}

				switch (structureNode.getType()) {
				case DOCUMENT:
				case FIELD:
					break;
				case IF:
					/* hyperlink for the IF_{condition} */
					String ifHyperlink = String.format(hyperlinkTemplate, url, "IF_" + structureNode.getKey(),
							"IF_" + structureNode.getKey());

					/* hyperlink for the ENDIF_{condition} */
					String endifHyperlink = String.format(hyperlinkTemplate, url, "ENDIF_" + structureNode.getKey(),
							"ENDIF_" + structureNode.getKey());

					/* format the hyperlink template */
					rtfLinkListBuilder.append(String.format("\\line %s %s %s \\line %s %s", indentString, ifHyperlink,
							getRtfLinkList(structureNode.getNodes(), indent + 1, url), indentString, endifHyperlink));
					break;
				case WHILE:
					/* hyperlink for the WHILE_{condition} */
					String whileHyperlink = String.format(hyperlinkTemplate, url, "WHILE_" + structureNode.getKey(),
							"IF_" + structureNode.getKey());

					/* hyperlink for the ENDWHILE_{condition} */
					String endWhileHyperlink = String.format(hyperlinkTemplate, url,
							"ENDWHILE_" + structureNode.getKey(),
							"ENDIF_" + structureNode.getKey());

					/* format the hyperlink template */
					rtfLinkListBuilder.append(String.format("\\line %s %s %s \\line %s %s", indentString,
							whileHyperlink,
							getRtfLinkList(structureNode.getNodes(), indent + 1, url),
							indentString, endWhileHyperlink));
					break;
				}
			}
		}
		return rtfLinkListBuilder.toString();
	}

	private String getFileProtocolUrl(File f) {
		/* formats the file:///{file_path} url */
		return String.format("file:///%s", f.getPath()).replaceAll("\\\\", "/").replaceAll(" ", "%20");

	}

}
