import { getApiBaseUrl, type EnvironmentVersion } from "./api-environment";

function getEnvironmentVersion(): EnvironmentVersion {
  const version = wx.getAccountInfoSync().miniProgram.envVersion;
  return version === "trial" || version === "release" ? version : "develop";
}

export const environment = {
  version: getEnvironmentVersion(),
  apiBaseUrl: getApiBaseUrl(getEnvironmentVersion()),
};
