module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testMatch: ['**/e2e/wechat.spec.ts'],
  testTimeout: 60000,
  moduleFileExtensions: ['ts', 'js', 'json'],
}
