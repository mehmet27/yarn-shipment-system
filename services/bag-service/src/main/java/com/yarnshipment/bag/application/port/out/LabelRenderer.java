package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.BagLabel;
import java.util.List;

/** Etiketleri yazdırılabilir belgeye çevirir. */
public interface LabelRenderer {

    /** Her etiket bir sayfa; dönen değer PDF'tir. */
    byte[] render(List<BagLabel> labels);
}
