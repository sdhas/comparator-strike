import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ExcelParserService {
    private static final Logger LOG = LoggerFactory.getLogger(ExcelParserService.class);

    private String urlExtractorPrefix;

    private String urlExtractorSuffix;

    private String folderPath;

    private String fileToUploadName;

    private String fileToComparedName;

    private String fileNonExistingUrlsName;

    public void uploadExcelFileToDatabase() {

        LOG.info("Upload is going to start!");

        Properties properties = ApplicationUtil.getProperties();
        folderPath = properties.getProperty("folder.path");
        fileToUploadName = properties.getProperty("file.to.upload.name");

        urlExtractorPrefix = properties.getProperty("url.extractor.prefix");
        urlExtractorSuffix = properties.getProperty("url.extractor.suffix");

        String fileToUpload = folderPath + "/input/" + fileToUploadName;
        LOG.info("File to upload : " + fileToUpload);

        try {
            FileInputStream fileInputStream = new FileInputStream(fileToUpload);

            XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
            XSSFSheet worksheet = workbook.getSheetAt(0);
            List<Specification> specificationList = new ArrayList<>();
            if (validateTitleRow(worksheet.getRow(0))) {

                for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {
                    XSSFRow row = worksheet.getRow(i);
                    Specification specification = new Specification();
                    specification.setId(row.getCell(0).getStringCellValue());
                    specification.setBrand(row.getCell(1).getStringCellValue());
                    specification.setMpn(row.getCell(2).getStringCellValue());
                    specification.setGtin(row.getCell(3).getStringCellValue());
                    specification.setTitle(row.getCell(4).getStringCellValue());
                    specification.setMinPrice(row.getCell(5).getStringCellValue());
                    specification.setSample(row.getCell(6).getStringCellValue());
                    specification.setOtherSpec(row.getCell(7).getStringCellValue());
                    specification.setUrl(row.getCell(8).getStringCellValue());

                    specification.setUrlId(getUrlIdFromUrl(row.getCell(8).getStringCellValue()));

                    specificationList.add(specification);
                }
            }
            DatabaseActions databaseActions = new DatabaseActions();
            databaseActions.insertSpecifications(specificationList);

        } catch (FileNotFoundException e) {
            LOG.error("FileNotFoundException : " + e.getLocalizedMessage(), e);
        } catch (Exception e) {
            LOG.error("Exception : " + e.getLocalizedMessage(), e);
        }
        LOG.info("Finished Uploading!");
    }


    private String getUrlIdFromUrl(String url) {
        String urlId = StringUtils.deleteWhitespace(url);
        if (url.contains(urlExtractorPrefix) && url.contains(urlExtractorSuffix)) {
            urlId = StringUtils.replace(url, urlExtractorPrefix, "");
            urlId = StringUtils.replace(urlId, urlExtractorSuffix, "");
        }
        return urlId;
    }


    public void compareExcelFileWithExistingData() {
        LOG.info("Compare is going to start!");

        Properties properties = ApplicationUtil.getProperties();
        folderPath = properties.getProperty("folder.path");
        fileToComparedName = properties.getProperty("file.to.compare.name");
        fileNonExistingUrlsName = properties.getProperty("file.non.existing.urls.name");

        urlExtractorPrefix = properties.getProperty("url.extractor.prefix");
        urlExtractorSuffix = properties.getProperty("url.extractor.suffix");

        String fileToCompare = folderPath + "/input/" + fileToComparedName;
        LOG.info("File to compare : " + fileToCompare);

        List<String> nonExistingUrls = new ArrayList();

        try {
            FileInputStream fileInputStream = new FileInputStream(fileToCompare);

            XSSFWorkbook compareWorkbook = new XSSFWorkbook(fileInputStream);
            XSSFSheet compareWorksheet = compareWorkbook.getSheetAt(0);
            for (int i = 1; i < compareWorksheet.getPhysicalNumberOfRows(); i++) {
                XSSFRow row = compareWorksheet.getRow(i);
                String url = row.getCell(1).getStringCellValue();
                String urlId = getUrlIdFromUrl(url);
                DatabaseActions databaseActions = new DatabaseActions();
                if (databaseActions.isSpecificationAvailableForUrlId(urlId)) {
                    nonExistingUrls.add(url);
                }
            }

            if (CollectionUtils.isNotEmpty(nonExistingUrls)) {
                String fileToWrite = folderPath + "/output/" + fileNonExistingUrlsName;
                LOG.info("File to write non existing urls : " + fileToWrite);

                XSSFWorkbook newWorkbook = createNewWorkbookWithNonExistingUrls(nonExistingUrls);
                FileOutputStream fileOutputStream = new FileOutputStream(fileToWrite);
                newWorkbook.write(fileOutputStream);
            }
        } catch (FileNotFoundException e) {
            LOG.error("FileNotFoundException : " + e.getLocalizedMessage(), e);
        } catch (Exception e) {
            LOG.error("Exception : " + e.getLocalizedMessage(), e);
        }
        LOG.info("Finished Comparing!");
    }


    private boolean validateTitleRow(XSSFRow row) {
        // ID	Brand	MPN	GTIN	Title	Min_Price	Sample	Other Spec	Url

        return row.getCell(0).getStringCellValue().equalsIgnoreCase("ID")
                && row.getCell(1).getStringCellValue().equalsIgnoreCase("BRAND")
                && row.getCell(2).getStringCellValue().equalsIgnoreCase("MPN")
                && row.getCell(3).getStringCellValue().equalsIgnoreCase("GTIN")
                && row.getCell(4).getStringCellValue().equalsIgnoreCase("TITLE")
                && row.getCell(5).getStringCellValue().equalsIgnoreCase("MIN_PRICE")
                && row.getCell(6).getStringCellValue().equalsIgnoreCase("SAMPLE")
                && row.getCell(7).getStringCellValue().equalsIgnoreCase("OTHER SPEC")
                && row.getCell(8).getStringCellValue().equalsIgnoreCase("URL");

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
