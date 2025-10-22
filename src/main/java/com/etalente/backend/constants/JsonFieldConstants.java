package com.etalente.backend.constants;

public final class JsonFieldConstants {

    private JsonFieldConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // User Profile Fields
    public static final String PROFILE_EXPERIENCE_YEARS = "experienceYears";
    public static final String PROFILE_EDUCATION = "education";
    public static final String PROFILE_SKILLS = "skills";
    public static final String PROFILE_LOCATION = "location";

    // Job Post Fields
    public static final String JOB_POST_SKILLS = "skills";
    public static final String JOB_POST_LOCATION = "location";

    // Location Fields
    public static final String LOCATION_CITY = "city";
    public static final String LOCATION_COUNTRY = "country";
    public static final String LOCATION_STATE = "state";

    // Skill Fields
    public static final String SKILL_NAME = "name";
    public static final String SKILL_LEVEL = "level";

    // Education Fields
    public static final String EDUCATION_DEGREE = "degree";
    public static final String EDUCATION_INSTITUTION = "institution";
    public static final String EDUCATION_FIELD = "field";
}
