Component({
  properties: {
    label: {
      type: String,
      value: "确定",
    },
    variant: {
      type: String,
      value: "primary",
    },
    loading: {
      type: Boolean,
      value: false,
    },
    disabled: {
      type: Boolean,
      value: false,
    },
  },
  methods: {
    handleTap() {
      if (!this.properties.loading && !this.properties.disabled) {
        this.triggerEvent("action");
      }
    },
  },
});

