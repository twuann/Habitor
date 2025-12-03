package com.example.habitor.model;

/**
 * Enum representing merge strategies for local-to-cloud data migration.
 * Used when a user signs in with existing local habits.
 * 
 * Requirement 7.3: Offer to merge local habits with cloud data when signing in.
 */
public enum MergeStrategy {
    /**
     * Keep local habits, ignore cloud data.
     * Local habits will be uploaded to cloud, replacing any existing cloud data.
     */
    KEEP_LOCAL,
    
    /**
     * Replace local habits with cloud data.
     * Local habits will be deleted and replaced with cloud habits.
     */
    KEEP_CLOUD,
    
    /**
     * Merge both local and cloud habits.
     * Uses timestamp-based conflict resolution for habits with the same name.
     */
    MERGE_BOTH
}
