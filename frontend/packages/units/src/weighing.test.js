import { describe, expect, it } from 'vitest';
import { compareWeighing } from './weighing';
describe('compareWeighing', () => {
    it('fark toleransın içindeyse uyuşmazlık yoktur, ama eksikse neden gerekir', () => {
        expect(compareWeighing('1980.000', '1982.500', '5.000')).toEqual({
            diffKg: '-2.500',
            isShort: true,
            exceedsTolerance: false,
        });
    });
    it('fark toleransı aşınca uyuşmazlık vardır; fazla yük de uyuşmazlıktır', () => {
        expect(compareWeighing('1970.000', '1982.500', '5.000')?.exceedsTolerance).toBe(true);
        expect(compareWeighing('1990.000', '1982.500', '5.000')).toEqual({
            diffKg: '7.500',
            isShort: false,
            exceedsTolerance: true,
        });
    });
    it('tolerans bilinmiyorsa her fark uyuşmazlık sayılır; tam eşitlik değildir', () => {
        expect(compareWeighing('1982.000', '1982.500')?.exceedsTolerance).toBe(true);
        expect(compareWeighing('1982.500', '1982.500')?.exceedsTolerance).toBe(false);
    });
    it('sayı olmayan girdide undefined döner', () => {
        expect(compareWeighing('abc', '1982.500')).toBeUndefined();
    });
});
