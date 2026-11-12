-- Etiket kalktığı için kalite sınıfının etiket rengi kaldırılır; yerine listelerde ve seçicilerde kullanılan sıra gelir.
ALTER TABLE md_quality_grade DROP COLUMN label_color;
ALTER TABLE md_quality_grade ADD COLUMN sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order BETWEEN 0 AND 9999);

COMMENT ON COLUMN md_quality_grade.sort_order IS 'Listelerde ve seçicilerde görünme sırası (küçük önce); eşitlikte kod';

-- sort_order değiştirilebilir sütunlar arasına alınır (etiket rengi yerine).
CREATE OR REPLACE TRIGGER md_quality_grade_guard
    AFTER UPDATE OR DELETE ON md_quality_grade
    FOR EACH ROW EXECUTE FUNCTION guard_immutable_columns('name', 'sort_order', 'active');

-- Mevcut sınıflar koda göre sıralanır (A, B, C ...).
UPDATE md_quality_grade g
   SET sort_order = numbered.seq
  FROM (SELECT id, row_number() OVER (ORDER BY upper(code)) AS seq FROM md_quality_grade) numbered
 WHERE g.id = numbered.id;
