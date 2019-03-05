import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ApplicationUtil {
	private static final Logger LOG = LoggerFactory.getLogger(ApplicationUtil.class);

	public static Connection getConnect() throws SQLException {
		Properties prop = getProperties();
		String url = prop.getProperty("db.url");
		String username = prop.getProperty("db.username");
		String password = prop.getProperty("db.password");
		return DriverManager.getConnection(url, username, password);
	}

	public static Properties getProperties() {
		InputStream is;
		Properties prop = null;
		try {
			prop = new Properties();
			is = new FileInputStream(new File("C:\\excel-comparer-tool\\config\\config.properties"));
			prop.load(is);
		} catch (FileNotFoundException e) {
			LOG.error(e.getMessage());
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return prop;
	}

	public static String getUrlIdFromUrl(String url, String urlExtractorPrefix, String urlExtractorSuffix) {
		String urlId = StringUtils.deleteWhitespace(url);
		if (url.contains(urlExtractorPrefix) && url.contains(urlExtractorSuffix)) {
			urlId = StringUtils.replace(url, urlExtractorPrefix, "");
			urlId = StringUtils.replace(urlId, urlExtractorSuffix, "");
		}
		return urlId;
	}
}
