import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelComparator {
    private static final Logger LOG = LoggerFactory.getLogger(ExcelComparator.class);

    public static void main(String[] args) {
        LOG.info("Starting the application!");
        ExcelParserService excelParserService = new ExcelParserService();

        if (args != null && args.length == 1) {
            LOG.info("Initiated with the process : " + args[0]);
            if ("upload".equals(args[0])) {
                excelParserService.uploadExcelFileToDatabase();
            } else if ("compare".equals(args[0])) {
                excelParserService.compareExcelFileWithExistingData();
            }
        } else {
            LOG.error("Choose only 'upload' to upload the excel file or 'compare' to compare the excel file!");
        }
    }
}
