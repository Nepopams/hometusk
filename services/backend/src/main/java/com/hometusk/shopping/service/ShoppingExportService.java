package com.hometusk.shopping.service;

import com.hometusk.shopping.domain.ShoppingItem;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ShoppingExportService {

    /**
     * Export items as plain text.
     * Format: "name" or "name - quantity unit"
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
     * Headers: name,quantity,unit,purchased
     */
    public String exportAsCsv(List<ShoppingItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,quantity,unit,purchased\n");
        for (ShoppingItem item : items) {
            sb.append(escapeCsvField(item.getName())).append(",");
            sb.append(item.getQuantity() != null ? item.getQuantity() : 1).append(",");
            sb.append(escapeCsvField(item.getUnit())).append(",");
            sb.append(item.isPurchased()).append("\n");
        }
        return sb.toString();
    }

    private String formatTextLine(ShoppingItem item) {
        String name = item.getName();
        Integer qty = item.getQuantity();
        String unit = item.getUnit();

        boolean hasUnit = unit != null && !unit.isBlank();
        if ((qty == null || qty == 1) && !hasUnit) {
            return name;
        }

        StringBuilder line = new StringBuilder(name);
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
        return line.toString();
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
