interface RelaxAppOption {
  globalData: {
    accessToken: string | null;
  };
}

App<RelaxAppOption>({
  globalData: {
    accessToken: null,
  },
});

