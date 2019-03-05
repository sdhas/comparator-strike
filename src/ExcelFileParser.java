import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class ExcelFileParser {
	private static final Logger LOG = LoggerFactory.getLogger(ExcelFileParser.class);

	public void uploadExcelFileToDatabase() {

		try {
			Properties properties = ApplicationUtil.getProperties();
			String folderPath = properties.getProperty("folder.path");
			String fileToUploadName = properties.getProperty("file.to.upload.name");
			String urlExtractorPrefix = properties.getProperty("url.extractor.prefix");
			String urlExtractorSuffix = properties.getProperty("url.extractor.suffix");

			String fileToUpload = folderPath + "/input/" + fileToUploadName;

			/*
			 * Opens a package (archive / xlsx file) with read / write permissions. It is
			 * also possible to access it read only, which should be the first choice for
			 * read operations in case the file is already accessed by another user. To open
			 * read only provide an InputStream instead of a file path.
			 */
			OPCPackage pkg = OPCPackage.open(fileToUpload);
			XSSFReader r = new XSSFReader(pkg);

			/*
			 * Read the sharedStrings.xml from an xlsx file into an object representation.
			 */
			SharedStringsTable sst = r.getSharedStringsTable();

			/*
			 * Hand a read SharedStringsTable for further reference to the SAXParser and the
			 * underlying ContentHandler.
			 */
			XMLReader parser = fetchSheetParser(sst, urlExtractorPrefix, urlExtractorSuffix);

			/*
			 * To look up the Sheet Name / Sheet Order / rID, you need to process the core
			 * Workbook stream. Normally it's of the form rId# or rSheet#
			 * 
			 * Great! How do I know, if it is rSheet or rId? Thanks Microsoft. Anyhow, let's
			 * carry on with the noise.
			 * 
			 * I reference the third sheet from the left since the index starts at one.
			 */
			InputStream sheet2 = r.getSheet("rId1");
			InputSource sheetSource = new InputSource(sheet2);

			/*
			 * Run through a Sheet using a window of several XML tags instead of attempting
			 * to read the whole file into RAM at once. Leaves the handling of file content
			 * to the ContentHandler, which is in this case the nested class SheetHandler.
			 */
			parser.parse(sheetSource);

			/*
			 * Close the underlying InputStream for a Sheet XML.
			 */
			sheet2.close();

			LOG.info("Upload process done! verify the action!");
		} catch (IOException io) {
			LOG.error("IOException occurred : " + io.getMessage(), io);
		} catch (OpenXML4JException oxe) {
			LOG.error("IOException occurred : " + oxe.getMessage(), oxe);
		} catch (SAXException sax) {
			LOG.error("IOException occurred : " + sax.getMessage(), sax);
		} catch (Exception e) {
			LOG.error("Exception occurred : " + e.getMessage(), e);
		}
	}

	public void compareExcelFileWithExistingData() {
		LOG.info("Compare is going to start!");

		try {
			Properties properties = ApplicationUtil.getProperties();
			String folderPath = properties.getProperty("folder.path");
			String fileToComparedName = properties.getProperty("file.to.compare.name");
			String fileNonExistingUrlsName = properties.getProperty("file.non.existing.urls.name");
			String urlExtractorPrefix = properties.getProperty("url.extractor.prefix");
			String urlExtractorSuffix = properties.getProperty("url.extractor.suffix");

			String fileToCompare = folderPath + "/input/" + fileToComparedName;
			OPCPackage pkg = OPCPackage.open(fileToCompare);
			XSSFReader r = new XSSFReader(pkg);
			SharedStringsTable sst = r.getSharedStringsTable();
			XMLReader compareParser = fetchSheetParserCompare(sst, urlExtractorPrefix, urlExtractorSuffix);
			InputStream sheet2 = r.getSheet("rId1");
			InputSource sheetSource = new InputSource(sheet2);
			DatabaseActions databaseActions = new DatabaseActions();
			// cleaning NewUrls Table
			if (databaseActions.cleanNewUrlsTable()) {
				LOG.info("Cleaned the NewUrls Table");
			}
			compareParser.parse(sheetSource);
			sheet2.close();

			List<String> nonExistingUrls = databaseActions.getAllNewUrls();
			if (CollectionUtils.isNotEmpty(nonExistingUrls)) {
				String fileToWrite = folderPath + "/output/" + fileNonExistingUrlsName;
				LOG.info("File to write non existing urls : " + fileToWrite);

				XSSFWorkbook newWorkbook = createNewWorkbookWithNonExistingUrls(nonExistingUrls);
				FileOutputStream fileOutputStream = new FileOutputStream(fileToWrite);
				newWorkbook.write(fileOutputStream);
			}

			LOG.info("Comparing process done! verify the output file.");
		} catch (IOException io) {
			LOG.error("IOException occurred : " + io.getMessage(), io);
		} catch (OpenXML4JException oxe) {
			LOG.error("IOException occurred : " + oxe.getMessage(), oxe);
		} catch (SAXException sax) {
			LOG.error("IOException occurred : " + sax.getMessage(), sax);
		} catch (Exception e) {
			LOG.error("Exception occurred : " + e.getMessage(), e);
		}
	}

	public XMLReader fetchSheetParser(SharedStringsTable sst, String urlExtractorPrefix, String urlExtractorSuffix)
			throws SAXException {
		/*
		 * XMLReader parser = XMLReaderFactory
		 * .createXMLReader("org.apache.xerces.parsers.SAXParser");
		 */
		XMLReader parser = XMLReaderFactory.createXMLReader();

		ContentHandler handler = new SheetHandlerUpload(sst, urlExtractorPrefix, urlExtractorSuffix);
		parser.setContentHandler(handler);
		return parser;
	}

	public XMLReader fetchSheetParserCompare(SharedStringsTable sst, String urlExtractorPrefix,
			String urlExtractorSuffix) throws SAXException {
		XMLReader parser = XMLReaderFactory.createXMLReader();

		ContentHandler handler = new SheetHandlerCompare(sst, urlExtractorPrefix, urlExtractorSuffix);
		parser.setContentHandler(handler);
		return parser;
	}

	private XSSFWorkbook createNewWorkbookWithNonExistingUrls(List<String> nonExistingUrls) {
		XSSFWorkbook newWorkbook = new XSSFWorkbook();
		Sheet sheet = newWorkbook.createSheet("New Urls");
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("URLs");

		int rowNum = 1;
		for (String nonExistingUrl : nonExistingUrls) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(nonExistingUrl);
		}
		return newWorkbook;

	}
}
