import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DatabaseActions {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseActions.class);

    public void insertSpecifications(List<Specification> specificationsList) {

        String SQL = "INSERT INTO specification(id,brand,mpn,gtin,title,minprice,sample,otherspec,url,urlid) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = ApplicationUtil.getConnect();
             PreparedStatement statement = conn.prepareStatement(SQL)) {
            for (Specification specification : specificationsList) {
                statement.setString(1, specification.getId());
                statement.setString(2, specification.getBrand());
                statement.setString(3, specification.getMpn());
                statement.setString(4, specification.getGtin());
                statement.setString(5, specification.getTitle());
                statement.setString(6, specification.getMinPrice());
                statement.setString(7, specification.getSample());
                statement.setString(8, specification.getOtherSpec());
                statement.setString(9, specification.getUrl());
                statement.setString(10, specification.getUrlId());
                try {
                    int affectedRows = statement.executeUpdate();
                    if (affectedRows == 0) {
                        LOG.error("Failed to insert Specification with id : " + specification.getId());
                    }
                } catch (SQLException e) {
                    LOG.error(e.getMessage());
                }
            }
        } catch (SQLException ex) {
            LOG.error(ex.getMessage());
        }
    }

    public boolean isSpecificationAvailableForUrlId(String urlId) {

        String SQL = "SELECT urlid FROM  specification WHERE urlid='" + urlId+"'";

        try (Connection conn = ApplicationUtil.getConnect();
             PreparedStatement statement = conn.prepareStatement(SQL);
             ResultSet rs = statement.executeQuery()) {
            return rs.next();
        } catch (SQLException ex) {
            LOG.error(ex.getMessage());
        }
        return false;
    }
}
