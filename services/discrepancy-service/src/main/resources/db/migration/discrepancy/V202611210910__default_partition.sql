-- discrepancy_activity aylık bölümlüdür ve bölümler yalnızca 2027-12'ye kadar açılmıştı; o tarihten sonraki ilk yazma
-- "no partition of relation found for row" hatasıyla düşerdi. İleriki ayların bölümlerini uygulama açar
-- (yarnshipment-app, PartitionMaintenance: açılışta ve her gece, 12 ay ilerisi). Bu yedek bölüm, o iş bir nedenle
-- çalışmamışsa satırın yine de yazılabilmesi içindir; olağan durumda boş kalır. Dolu bir yedek bölüm, kapsadığı ayın
-- bölümünün açılmasını engeller; iş bunu ERROR olarak loglar ve satırlar elle taşınır.
CREATE TABLE discrepancy_activity_default PARTITION OF discrepancy_activity DEFAULT;
