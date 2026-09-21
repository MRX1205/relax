import { describe, expect, it } from "vitest";

import { getApiBaseUrl } from "../miniprogram/config/api-environment";

describe("getApiBaseUrl", () => {
  it("maps WeChat environment versions to their API endpoints", () => {
    expect(getApiBaseUrl("develop")).toBe("http://127.0.0.1:8080");
    expect(getApiBaseUrl("trial")).toBe("https://realxback.lyhlz.cn");
    expect(getApiBaseUrl("release")).toBe("https://realxback.lyhlz.cn");
  });
});
