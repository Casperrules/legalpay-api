import axios from "axios";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Types
export interface Contract {
  id: string;
  merchantId: string;
  payerId: string;
  principalAmount: number;
  interestRate: number;
  emiAmount: number;
  startDate: string;
  endDate: string;
  paymentType: "ONE_TIME" | "EMI";
  paymentFrequency?: "MONTHLY" | "QUARTERLY" | "YEARLY";
  status:
    | "DRAFT"
    | "PENDING_ESIGN"
    | "SIGNED"
    | "ACTIVE"
    | "COMPLETED"
    | "DEFAULTED"
    | "CANCELLED";
  paymentStatus?: string; // PENDING, PARTIAL, PAID, FAILED, REFUNDED
  totalPaidAmount?: number;
  lastPaymentAt?: string;
  pdfUrl?: string;
  esignDocumentId?: string;
  blockchainHash?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface ContractCreateRequest {
  merchantId: string;
  payerId: string;
  principalAmount: number;
  interestRate: number;
  startDate: string;
  endDate: string;
  paymentType: "ONE_TIME" | "EMI";
  paymentFrequency?: "MONTHLY" | "QUARTERLY" | "YEARLY";
}

export interface Merchant {
  id: string;
  businessName: string;
  email: string;
  phoneNumber: string;
  status: "PENDING_KYC" | "ACTIVE" | "SUSPENDED";
}

export interface Payer {
  id: string;
  fullName: string;
  email: string;
  phoneNumber: string;
}

export interface Payment {
  id: string;
  contractId: string;
  dueDate: string;
  amount: number;
  status: "SCHEDULED" | "SUCCESS" | "FAILED" | "PENDING";
  paymentDate?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// API Methods
export const authApi = {
  login: async (email: string, password: string): Promise<any> => {
    const response = await api.post("/api/v1/auth/login", { email, password });
    return response.data;
  },
};

export const healthApi = {
  check: async () => {
    const response = await api.get("/health");
    return response.data;
  },
};

export const contractApi = {
  create: async (data: ContractCreateRequest): Promise<Contract> => {
    const response = await api.post("/api/v1/contracts", data);
    return response.data;
  },

  getById: async (id: string): Promise<Contract> => {
    const response = await api.get(`/api/v1/contracts/${id}`);
    return response.data;
  },

  list: async (
    userId: string,
    userRole: string,
    page = 0,
    size = 10,
  ): Promise<PageResponse<Contract>> => {
    const params: any = { page, size };
    if (userRole === "MERCHANT") {
      params.merchantId = userId;
    } else if (userRole === "PAYER") {
      params.payerId = userId;
    }
    const response = await api.get("/api/v1/contracts", { params });
    return response.data;
  },

  initiateESign: async (id: string): Promise<Contract> => {
    const response = await api.post(`/api/v1/contracts/${id}/esign`);
    return response.data;
  },
};

export const merchantApi = {
  list: async (): Promise<Merchant[]> => {
    const response = await api.get("/api/v1/merchants");
    return response.data;
  },
  getById: async (id: string): Promise<Merchant> => {
    const response = await api.get(`/api/v1/merchants/${id}`);
    return response.data;
  },
};

export const payerApi = {
  list: async (): Promise<Payer[]> => {
    const response = await api.get("/api/v1/payers");
    return response.data;
  },
  getById: async (id: string): Promise<Payer> => {
    const response = await api.get(`/api/v1/payers/${id}`);
    return response.data;
  },
};

export default api;
