import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SheetHandlerUpload extends DefaultHandler {
	private static final Logger LOG = LoggerFactory.getLogger(SheetHandlerUpload.class);

	private SharedStringsTable sst;
	private String lastContents;
	private boolean nextIsString;
	private Specification spec;
	private String cellReference;
	private String urlExtractorPrefix;
	private String urlExtractorSuffix;

	public SheetHandlerUpload(SharedStringsTable sst, String urlExtractorPrefix, String urlExtractorSuffix) {
		this.sst = sst;
		this.urlExtractorPrefix = urlExtractorPrefix;
		this.urlExtractorSuffix = urlExtractorSuffix;
	}

	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		if (name.contentEquals("row")) {
			spec = new Specification();
		}
		// c => cell
		if (name.equals("c")) {
			// Print the cell reference
			cellReference = StringUtils.substring(attributes.getValue("r"), 0, 1);
//			System.out.print(attributes.getValue("r") + " - ");
			// Figure out if the value is an index in the SST
			String cellType = attributes.getValue("t");
			if (cellType != null && cellType.equals("s")) {
				nextIsString = true;
			} else {
				nextIsString = false;
			}
		}
		// Clear contents cache
		lastContents = "";
	}

	public void endElement(String uri, String localName, String name) throws SAXException {

		if (name.contentEquals("row")) {
			DatabaseActions databaseActions = new DatabaseActions();
			databaseActions.insertSpecification(spec);
			LOG.info("Inserted the row with id : " + spec.getId());
		}

		// Process the last contents as required.
		// Do now, as characters() may be called more than once

		if (nextIsString) {
			int idx = Integer.parseInt(lastContents);
			lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
			nextIsString = false;
		}

		// v => contents of a cell
		// Output after we've seen the string contents
		if (name.equals("v")) {
//			System.out.println(lastContents);
			switch (cellReference) {
			case "A":
				spec.setId(lastContents);
				break;
			case "B":
				spec.setBrand(lastContents);
				break;
			case "C":
				spec.setMpn(lastContents);
				break;
			case "D":
				spec.setGtin(lastContents);
				break;
			case "E":
				spec.setTitle(lastContents);
				break;
			case "F":
				spec.setMinPrice(lastContents);
				break;
			case "G":
				spec.setSample(lastContents);
				break;
			case "H":
				spec.setOtherSpec(lastContents);
				break;
			case "I":
				spec.setUrl(lastContents);
				spec.setUrlId(ApplicationUtil.getUrlIdFromUrl(lastContents, urlExtractorPrefix, urlExtractorSuffix));
				break;
			}
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		lastContents += new String(ch, start, length);
	}
}
