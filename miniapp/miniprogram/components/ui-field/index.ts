Component({
  properties: {
    label: String,
    value: String,
    placeholder: String,
    required: Boolean,
    error: String,
    type: {
      type: String,
      value: "text",
    },
  },
  methods: {
    handleInput(event: WechatMiniprogram.Input) {
      this.triggerEvent("change", { value: event.detail.value });
    },
  },
});

