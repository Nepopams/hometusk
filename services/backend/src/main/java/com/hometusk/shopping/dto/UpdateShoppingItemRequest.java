package com.hometusk.shopping.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Partial shopping item update")
public class UpdateShoppingItemRequest {

    private Boolean purchased;
    private String category;
    private String source;
    private UUID linkedTaskId;
    private boolean purchasedPresent;
    private boolean categoryPresent;
    private boolean sourcePresent;
    private boolean linkedTaskPresent;

    @Schema(description = "Whether the item has been purchased", example = "true")
    public Boolean purchased() {
        return purchased;
    }

    @JsonSetter("purchased")
    public void setPurchased(Boolean purchased) {
        this.purchased = purchased;
        this.purchasedPresent = true;
    }

    @Schema(description = "Optional shopping category", example = "groceries")
    public String category() {
        return category;
    }

    @JsonSetter("category")
    public void setCategory(String category) {
        this.category = category;
        this.categoryPresent = true;
    }

    @Schema(description = "Optional source/store name", example = "Perekrestok")
    @Size(max = 120, message = "Source must be at most 120 characters")
    public String source() {
        return source;
    }

    @JsonSetter("source")
    public void setSource(String source) {
        this.source = source;
        this.sourcePresent = true;
    }

    @Schema(description = "Optional linked task update. Null explicitly unlinks the item.")
    public UUID linkedTaskId() {
        return linkedTaskId;
    }

    @JsonSetter("linkedTaskId")
    public void setLinkedTaskId(UUID linkedTaskId) {
        this.linkedTaskId = linkedTaskId;
        this.linkedTaskPresent = true;
    }

    @JsonIgnore
    public boolean hasPurchased() {
        return purchasedPresent;
    }

    @JsonIgnore
    public boolean hasCategory() {
        return categoryPresent;
    }

    @JsonIgnore
    public boolean hasSource() {
        return sourcePresent;
    }

    @JsonIgnore
    public boolean hasLinkedTask() {
        return linkedTaskPresent;
    }

    @JsonIgnore
    public boolean hasAnyMutableField() {
        return purchasedPresent || categoryPresent || sourcePresent || linkedTaskPresent;
    }
}
