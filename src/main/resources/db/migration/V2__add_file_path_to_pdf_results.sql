-- V2: Add file_path column to track where the uploaded file is stored.
-- For local storage: absolute file path on disk.
-- For S3 storage: the S3 object key.
ALTER TABLE pdf_results
    ADD COLUMN IF NOT EXISTS file_path VARCHAR(1000);
