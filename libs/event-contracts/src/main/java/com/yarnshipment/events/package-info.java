/**
 * Servisler arası olay sözleşmeleri (EVT-01). Alanlar yalnızca eklenir (EVT-06); sonradan eklenen alanlar eski
 * olaylarda boş gelebileceği için {@code @Nullable}'dır. Okuyucular bilinmeyen alanları yok sayar.
 */
@NullMarked
package com.yarnshipment.events;

import org.jspecify.annotations.NullMarked;
