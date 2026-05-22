module.exports = {
  testEnvironment: "node",
  testMatch: ["**/test/**/*.test.ts"],
  setupFiles: ["./test/setup.ts"],
  transform: {
    "^.+\\.ts$": ["ts-jest", { tsconfig: "tsconfig.test.json" }],
  },
};
