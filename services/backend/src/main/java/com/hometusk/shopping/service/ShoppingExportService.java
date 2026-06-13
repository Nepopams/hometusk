package com.hometusk.shopping.service;

import com.hometusk.shopping.domain.ShoppingItem;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ShoppingExportService {

    /**
     * Export items as plain text.
     * Format: "name" or "name - quantity unit", with category/source labels when present.
     */
    public String exportAsText(List<ShoppingItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ShoppingItem item : items) {
            sb.append(formatTextLine(item)).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Export items as CSV (RFC 4180 compliant).
     * Headers: name,quantity,unit,category,source,purchased
     */
    public String exportAsCsv(List<ShoppingItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,quantity,unit,category,source,purchased\n");
        for (ShoppingItem item : items) {
            sb.append(escapeCsvField(item.getName())).append(",");
            sb.append(item.getQuantity() != null ? item.getQuantity() : 1).append(",");
            sb.append(escapeCsvField(item.getUnit())).append(",");
            sb.append(escapeCsvField(item.getCategory())).append(",");
            sb.append(escapeCsvField(item.getSource())).append(",");
            sb.append(item.isPurchased()).append("\n");
        }
        return sb.toString();
    }

    private String formatTextLine(ShoppingItem item) {
        String name = item.getName();
        Integer qty = item.getQuantity();
        String unit = item.getUnit();

        boolean hasUnit = unit != null && !unit.isBlank();
        StringBuilder line = new StringBuilder(name);
        if ((qty == null || qty == 1) && !hasUnit) {
            appendMetadata(line, item);
            return line.toString();
        }

        line.append(" - ");
        if (qty != null) {
            line.append(qty);
        }
        if (hasUnit) {
            if (qty != null) {
                line.append(" ");
            }
            line.append(unit);
        }
        appendMetadata(line, item);
        return line.toString();
    }

    private void appendMetadata(StringBuilder line, ShoppingItem item) {
        boolean hasCategory = item.getCategory() != null && !item.getCategory().isBlank();
        boolean hasSource = item.getSource() != null && !item.getSource().isBlank();
        if (!hasCategory && !hasSource) {
            return;
        }

        line.append(" [");
        if (hasCategory) {
            line.append("category: ").append(item.getCategory());
        }
        if (hasCategory && hasSource) {
            line.append(", ");
        }
        if (hasSource) {
            line.append("source: ").append(item.getSource());
        }
        line.append("]");
    }

    /**
     * RFC 4180 CSV field escaping.
     * Quote if contains comma, quote, or newline. Double internal quotes.
     */
    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting =
                value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (needsQuoting) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
