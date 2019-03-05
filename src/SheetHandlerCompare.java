import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SheetHandlerCompare extends DefaultHandler {

	private static final Logger LOG = LoggerFactory.getLogger(SheetHandlerCompare.class);

	private SharedStringsTable sst;
	private String url;
	private boolean nextIsString;
	private String cellReference;
	private String urlExtractorPrefix;
	private String urlExtractorSuffix;

	public SheetHandlerCompare(SharedStringsTable sst, String urlExtractorPrefix, String urlExtractorSuffix) {
		this.sst = sst;
		this.urlExtractorPrefix = urlExtractorPrefix;
		this.urlExtractorSuffix = urlExtractorSuffix;
	}

	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		// c => cell
		if (name.equals("c")) {
			// Print the cell reference
			cellReference = StringUtils.substring(attributes.getValue("r"), 0, 1);
			// Figure out if the value is an index in the SST
			String cellType = attributes.getValue("t");
			if (cellType != null && cellType.equals("s")) {
				nextIsString = true;
			} else {
				nextIsString = false;
			}
		}
		// Clear contents cache
		url = "";
	}

	public void endElement(String uri, String localName, String name) throws SAXException {
		// Process the last contents as required.
		// Do now, as characters() may be called more than once

		if (nextIsString) {
			int idx = Integer.parseInt(url);
			url = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
			nextIsString = false;
		}

		// v => contents of a cell
		// Output after we've seen the string contents
		if (name.equals("v") && cellReference.equals("B")) {
			DatabaseActions databaseActions = new DatabaseActions();
			String urlId = ApplicationUtil.getUrlIdFromUrl(url, urlExtractorPrefix, urlExtractorSuffix);
			if (databaseActions.isSpecificationAvailableForUrlId(urlId)) {
				databaseActions.insertNewUrls(url);
				LOG.info("Inserting url into newurls table" + url);
			}
		}
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		url += new String(ch, start, length);
	}
}
