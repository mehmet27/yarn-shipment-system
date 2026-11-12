const LOWER = 'abcdefghijkmnpqrstuvwxyz';
const UPPER = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
const DIGITS = '23456789';
const GROUPS = 3;

function pick(alphabet: string): string {
  const [value = 0] = crypto.getRandomValues(new Uint32Array(1));
  return alphabet.charAt(value % alphabet.length);
}

/** Karışabilecek karakterleri (0/O, 1/l/I) içermeyen, okunup iletilebilir geçici şifre: "Kx7m-Qp2w-Nv9d". */
export function generatePassword(): string {
  return Array.from(
    { length: GROUPS },
    () => pick(UPPER) + pick(LOWER) + pick(DIGITS) + pick(LOWER),
  ).join('-');
}
