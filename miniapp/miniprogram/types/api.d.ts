interface ApiEnvelope<T> {
  code: string;
  message: string;
  data: T;
  requestId: string;
}

interface HealthStatus {
  service: string;
  status: string;
  timestamp: string;
}

