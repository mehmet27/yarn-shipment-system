export const YARN_LIST_ID = 'merkez-iplik-cinsleri';

/** Stoktaki iplik cinslerini giriş alanlarına öneri olarak sunar (elle yazım hatasını azaltır). */
export function YarnTypeList({ yarnTypes }: { yarnTypes: string[] }) {
  return (
    <datalist id={YARN_LIST_ID}>
      {yarnTypes.map((name) => (
        <option key={name} value={name} />
      ))}
    </datalist>
  );
}
