import { describe, expect, it } from 'vitest';
import { count, kg, percent, text } from './reportModel';
import type { ReportTable } from './reportModel';
import { buildPdfDefinition } from './writePdf';
import type { PdfOptions } from './writePdf';

const OPTIONS: PdfOptions = {
  title: 'Şahin İplik Raporu',
  intro: 'Açıklama',
  empty: 'Kayıt yok',
  signatures: ['Hazırlayan', 'Kontrol eden', 'Onaylayan'],
  dateLine: 'Tarih / İmza',
  logo: null,
};

const TABLE: ReportTable = {
  title: 'Fire',
  headers: ['Fasoncu', 'Gönderilen', 'Oran', 'Adet'],
  rows: [[text('Çağrı Örme'), kg(1234.5), percent(4.03), count(2)]],
  total: [text('Toplam'), kg(1234.5), percent(4.03), count(2)],
};

describe('buildPdfDefinition', () => {
  it('yatay A4 ve Türkçe karakterli yazı tipiyle kurulur', () => {
    const definition = buildPdfDefinition([TABLE], OPTIONS);

    expect(definition.pageSize).toBe('A4');
    expect(definition.pageOrientation).toBe('landscape');
    expect(definition.defaultStyle).toMatchObject({ font: 'Roboto' });
  });

  function bodyOf(): { text: string; alignment?: string; bold?: boolean }[][] {
    const content = JSON.parse(JSON.stringify(buildPdfDefinition([TABLE], OPTIONS).content)) as {
      table?: { body: { text: string; alignment?: string; bold?: boolean }[][] };
    }[];
    return content.find((item) => item.table !== undefined)?.table?.body ?? [];
  }

  it('başlık satırı her sayfada tekrarlanır', () => {
    const content = JSON.parse(JSON.stringify(buildPdfDefinition([TABLE], OPTIONS).content)) as {
      table?: { headerRows: number };
    }[];

    expect(content.find((item) => item.table !== undefined)?.table?.headerRows).toBe(1);
    expect(bodyOf()[0]?.map((cell) => cell.text)).toEqual(TABLE.headers);
  });

  it('sayılar tr-TR biçimli ve sağa hizalıdır', () => {
    const row = bodyOf()[1];

    expect(row?.map((cell) => cell.text)).toEqual(['Çağrı Örme', '1.234,500 kg', '%4,03', '2']);
    expect(row?.map((cell) => cell.alignment)).toEqual(['left', 'right', 'right', 'right']);
  });

  it('toplam satırı kalındır', () => {
    expect(bodyOf()[2]?.every((cell) => cell.bold === true)).toBe(true);
  });

  it('kayıtsız tablo yerine açıklama metni, sonda imza alanları ve logo varsa en başta görsel gelir', () => {
    const definition = buildPdfDefinition(
      [{ title: TABLE.title, headers: TABLE.headers, rows: [] }],
      {
        ...OPTIONS,
        logo: 'data:image/png;base64,AAAA',
      },
    );
    const json = JSON.stringify(definition.content);

    expect(json).toContain('Kayıt yok');
    expect(json).not.toContain('"table"');
    for (const label of OPTIONS.signatures) expect(json).toContain(label);
    expect(Array.isArray(definition.content) && definition.content[0]).toMatchObject({
      image: 'data:image/png;base64,AAAA',
    });
  });
});
