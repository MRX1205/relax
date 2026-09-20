import { describe, expect, it } from "vitest";

import { getApiBaseUrl } from "../miniprogram/config/api-environment";

describe("getApiBaseUrl", () => {
  it("maps WeChat environment versions to their API endpoints", () => {
    expect(getApiBaseUrl("develop")).toBe("http://192.168.1.7:8080");
    expect(getApiBaseUrl("trial")).toBe("https://realxback.lyhlz.cn");
    expect(getApiBaseUrl("release")).toBe("https://realxback.lyhlz.cn");
  });
});
