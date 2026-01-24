package com.hometusk.gamification.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "criteria", length = 500)
    private String criteria;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    protected Badge() {}

    public Badge(String code, String name, String description, String criteria, String iconName) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.criteria = criteria;
        this.iconName = iconName;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCriteria() {
        return criteria;
    }

    public String getIconName() {
        return iconName;
    }
}
