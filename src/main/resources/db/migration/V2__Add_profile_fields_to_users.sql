-- V2__Add_profile_fields_to_users.sql

-- Add new columns to the users table for candidate profiles
ALTER TABLE users
ADD COLUMN first_name VARCHAR(255),
ADD COLUMN last_name VARCHAR(255),
ADD COLUMN phone VARCHAR(50),
ADD COLUMN summary TEXT,
ADD COLUMN profile_image_url VARCHAR(255);

-- Add a boolean flag to track profile completion
ALTER TABLE users
ADD COLUMN profile_complete BOOLEAN NOT NULL DEFAULT FALSE;
