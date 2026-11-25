package com.yarnshipment.shipment.adapter.out.persistence.ledger;

import com.yarnshipment.shipment.application.port.out.ledger.FabricRecipePort;
import com.yarnshipment.shipment.domain.fabric.FabricRecipe;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeConflictException;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeNotFoundException;
import com.yarnshipment.shipment.domain.fabric.FabricRecipeScope;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

/** Kumaş reçetelerinin JDBC uygulaması; karışım satırları tek jsonb sütunundadır. */
@Component
final class FabricRecipeAdapter implements FabricRecipePort {

    private static final String SELECT = """
            SELECT id, fabric_type, lines, active FROM md_fabric_recipe
            WHERE active OR :includeInactive ORDER BY lower(fabric_type)""";
    private static final String INSERT = """
            INSERT INTO md_fabric_recipe (id, fabric_type, lines, active, created_at, created_by)
            VALUES (:id, :fabricType, :lines::jsonb, :active, :at, :createdBy)""";
    private static final String UPDATE = """
            UPDATE md_fabric_recipe SET fabric_type = :fabricType, lines = :lines::jsonb, active = :active,
                   updated_at = :at
            WHERE id = :id""";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final JdbcClient jdbc;

    FabricRecipeAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<FabricRecipe> findAll(FabricRecipeScope scope) {
        return jdbc.sql(SELECT)
                .param("includeInactive", scope == FabricRecipeScope.ALL)
                .query((rs, i) -> toRecipe(rs))
                .list();
    }

    @Override
    public void insert(FabricRecipe recipe, UUID createdBy, Instant at) {
        try {
            jdbc.sql(INSERT)
                    .param("id", recipe.id())
                    .param("fabricType", recipe.fabricType())
                    .param("lines", writeLines(recipe.yarns()))
                    .param("active", recipe.isActive())
                    .param("at", Timestamp.from(at))
                    .param("createdBy", createdBy)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new FabricRecipeConflictException(recipe.fabricType());
        }
    }

    @Override
    public void replace(FabricRecipe recipe, Instant at) {
        int changed;
        try {
            changed = jdbc.sql(UPDATE)
                    .param("id", recipe.id())
                    .param("fabricType", recipe.fabricType())
                    .param("lines", writeLines(recipe.yarns()))
                    .param("active", recipe.isActive())
                    .param("at", Timestamp.from(at))
                    .update();
        } catch (DuplicateKeyException e) {
            throw new FabricRecipeConflictException(recipe.fabricType());
        }
        if (changed == 0) {
            throw new FabricRecipeNotFoundException(recipe.id());
        }
    }

    private static FabricRecipe toRecipe(ResultSet rs) throws SQLException {
        try {
            List<FabricRecipe.Line> lines = new ArrayList<>();
            for (JsonNode node : JSON.readTree(rs.getString("lines"))) {
                lines.add(new FabricRecipe.Line(
                        node.get("yarnType").asString(), node.get("ratioPct").decimalValue()));
            }
            return new FabricRecipe(
                    rs.getObject("id", UUID.class), rs.getString("fabric_type"), lines, rs.getBoolean("active"));
        } catch (JacksonException e) {
            throw new IllegalStateException("md_fabric_recipe.lines is not valid JSON", e);
        }
    }

    private static String writeLines(List<FabricRecipe.Line> lines) {
        ArrayNode array = JSON.createArrayNode();
        for (FabricRecipe.Line line : lines) {
            array.addObject()
                    .put("yarnType", line.yarnType())
                    .put("ratioPct", new BigDecimal(line.ratioPct().toPlainString()));
        }
        return array.toString();
    }
}
