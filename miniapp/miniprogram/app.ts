import { getCachedAccount, loadCurrentAccount, loginWithWechat } from "./services/auth";
import { getAccessToken } from "./services/http";

App<RelaxAppOption>({
  globalData: {
    account: null,
  },
  async onLaunch() {
    this.globalData.account = getCachedAccount();
    if (getAccessToken()) {
      try {
        await loadCurrentAccount();
      } catch (e) {
        console.warn("Restore account failed, try auto login", e);
        try {
          await loginWithWechat();
        } catch (err) {
          console.warn("Auto login failed", err);
        }
      }
    } else {
      try {
        await loginWithWechat();
      } catch (err) {
        console.warn("Initial wechat login failed", err);
      }
    }
  },
});

