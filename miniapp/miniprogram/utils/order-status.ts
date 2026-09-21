export interface OrderStatusInfo {
  text: string;
  color: string;
  bg: string;
  icon: string;
  desc: string;
}

export const ORDER_STATUS_MAP: Record<string, OrderStatusInfo> = {
  PENDING_PAYMENT: {
    text: "待支付",
    color: "#FA8C16",
    bg: "#FFF7E6",
    icon: "⏳",
    desc: "订单已提交，请在15分钟内完成支付",
  },
  PAID: {
    text: "待接单",
    color: "#1890FF",
    bg: "#E6F7FF",
    icon: "🛎️",
    desc: "已完成支付，系统正在通知技师接单",
  },
  ACCEPTED: {
    text: "已接单",
    color: "#722ED1",
    bg: "#F9F0FF",
    icon: "👍",
    desc: "技师已确认接单，正准备安排出发",
  },
  DEPARTED: {
    text: "已出发",
    color: "#13C2C2",
    bg: "#E6FFFB",
    icon: "🚗",
    desc: "技师已启程前往服务地址，请保持电话畅通",
  },
  ARRIVED: {
    text: "已到达",
    color: "#52C41A",
    bg: "#F6FFED",
    icon: "📍",
    desc: "技师已到达预约地点",
  },
  IN_SERVICE: {
    text: "服务中",
    color: "#E54D42",
    bg: "#FFF2E8",
    icon: "💆",
    desc: "技师正在提供专业到家理疗服务",
  },
  COMPLETED: {
    text: "已完成",
    color: "#52C41A",
    bg: "#F6FFED",
    icon: "🎉",
    desc: "本次服务已完成，期待再次为您服务",
  },
  CANCELLED: {
    text: "已取消",
    color: "#8C8C8C",
    bg: "#F5F5F5",
    icon: "❌",
    desc: "订单已取消",
  },
  EXPIRED: {
    text: "已超时",
    color: "#8C8C8C",
    bg: "#F5F5F5",
    icon: "⏱️",
    desc: "支付超时，系统已自动取消订单",
  },
  REJECTED: {
    text: "已拒单",
    color: "#F5222D",
    bg: "#FFF1F0",
    icon: "🚫",
    desc: "技师无法接单，费用将原路全额退还",
  },
  REFUNDED: {
    text: "已退款",
    color: "#2F54EB",
    bg: "#F0F5FF",
    icon: "💰",
    desc: "退款已受理或已退回原支付账户",
  },
};

export function formatOrderStatus(status: string): OrderStatusInfo {
  return (
    ORDER_STATUS_MAP[status] || {
      text: status || "未知状态",
      color: "#8C8C8C",
      bg: "#F5F5F5",
      icon: "📋",
      desc: "请查看订单详细信息",
    }
  );
}

/**
 * 格式化履约操作动作标签
 */
export function getTechActionLabel(status: string): { action: string; label: string; primary: boolean } | null {
  switch (status) {
    case "PAID":
      return { action: "accept", label: "立即接单", primary: true };
    case "ACCEPTED":
      return { action: "depart", label: "我已出发", primary: true };
    case "DEPARTED":
      return { action: "arrive", label: "我已到达", primary: true };
    case "ARRIVED":
      return { action: "start", label: "开始服务", primary: true };
    case "IN_SERVICE":
      return { action: "complete", label: "服务完成", primary: true };
    default:
      return null;
  }
}
