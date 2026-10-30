package com.yarnshipment.bag.domain.model;

/**
 * Basılacak bir etiket.
 *
 * @param printNo bu çuvalın kaçıncı baskısı; 1'den büyükse etikete yeniden baskı olduğu yazılır
 */
public record BagLabel(Bag bag, int printNo) {

    public boolean isReprint() {
        return printNo > 1;
    }
}
