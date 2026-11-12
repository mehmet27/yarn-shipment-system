-- Fasoncu ünvanı tektir (QA H-07). Karşılaştırma büyük/küçük harf ve baş/son boşluk farkını yok sayar; pasif
-- kayıtlar da dahildir, çünkü pasif fasoncu yeniden etkinleştirilebilir. Kuraldan önce açılmış tekrar kayıtlar
-- silinmez: ilk açılan ünvanını korur, sonrakilerin ünvanına " (2)", " (3)" ... eklenir.

UPDATE md_subcontractor AS s
SET title = btrim(s.title) || ' (' || d.rn || ')'
FROM (
    SELECT id, row_number() OVER (PARTITION BY lower(btrim(title)) ORDER BY created_at, id) AS rn
    FROM md_subcontractor
) AS d
WHERE s.id = d.id AND d.rn > 1;

CREATE UNIQUE INDEX md_subcontractor_title_key ON md_subcontractor (lower(btrim(title)));
