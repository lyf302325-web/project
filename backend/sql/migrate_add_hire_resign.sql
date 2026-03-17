ALTER TABLE user
  ADD COLUMN hire_date DATE NULL AFTER attendance_group_id,
  ADD COLUMN resign_date DATE NULL AFTER hire_date;

