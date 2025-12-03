package com.example.habitor.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Arrays;
import java.util.List;

/**
 * Category entity for organizing habits.
 * Includes predefined default categories and support for custom categories.
 */
@Entity(tableName = "Category")
public class Category {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String color;        // Hex color code
    public boolean isDefault;   // true for predefined categories

    // Default constructor for Room
    public Category() {
        this.name = "";
        this.color = "#808080";
        this.isDefault = false;
    }

    @Ignore
    public Category(String name, String color, boolean isDefault) {
        this.name = name;
        this.color = color;
        this.isDefault = isDefault;
    }

    // ===========================
    // GETTERS AND SETTERS
    // ===========================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    // ===========================
    // DEFAULT CATEGORIES
    // ===========================

    /**
     * Get list of default categories to be inserted on first app launch.
     */
    @Ignore
    public static List<Category> getDefaultCategories() {
        return Arrays.asList(
            new Category("Health", "#4CAF50", true),      // Green
            new Category("Work", "#2196F3", true),        // Blue
            new Category("Personal", "#9C27B0", true),    // Purple
            new Category("Learning", "#FF9800", true),    // Orange
            new Category("Other", "#607D8B", true)        // Blue Grey
        );
    }

    /**
     * Default category names for validation and reference.
     */
    @Ignore
    public static final String CATEGORY_HEALTH = "Health";
    @Ignore
    public static final String CATEGORY_WORK = "Work";
    @Ignore
    public static final String CATEGORY_PERSONAL = "Personal";
    @Ignore
    public static final String CATEGORY_LEARNING = "Learning";
    @Ignore
    public static final String CATEGORY_OTHER = "Other";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id &&
                isDefault == category.isDefault &&
                objectsEquals(name, category.name) &&
                objectsEquals(color, category.color);
    }

    private static boolean objectsEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
