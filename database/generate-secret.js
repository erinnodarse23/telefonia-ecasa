const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

const secret = crypto.randomBytes(32).toString('hex');

console.log('=== JWT Secret generado exitosamente ===');
console.log(`Secret: ${secret}`);
console.log('');

const outputPath = path.join(__dirname, '..', 'jwt-secret.txt');
fs.writeFileSync(outputPath, secret, 'utf8');

console.log(`Archivo guardado en: ${outputPath}`);
console.log('');
console.log('Copia el secret en tu archivo .env:');
console.log(`JWT_SECRET=${secret}`);
