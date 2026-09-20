import { getServiceAreas } from "../../services/region";

Page({
  data: {
    loading: true,
    areas: [] as ServiceArea[],
  },

  async onLoad() {
    try {
      const areas = await getServiceAreas();
      this.setData({ areas, loading: false });
    } catch {
      this.setData({ loading: false });
    }
  },
});
