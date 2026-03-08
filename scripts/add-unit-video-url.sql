-- =============================================================================
-- Add video_url column to units table
-- Run: psql -d ukti_db -f scripts/add-unit-video-url.sql
-- =============================================================================

ALTER TABLE units
  ADD COLUMN IF NOT EXISTS video_url VARCHAR(500) NULL;

COMMENT ON COLUMN units.video_url IS 'URL of intro/instructional video for this unit. Played when user opens the unit.';
