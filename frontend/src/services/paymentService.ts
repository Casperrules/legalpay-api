import { api } from "./api";

export interface PaymentOrderResponse {
  orderId: string;
  amount: number;
  currency: string;
  razorpayKeyId: string;
  contractTitle: string;
  merchantName: string;
  payerEmail: string;
  payerPhone: string;
}

export interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  order_id: string;
  prefill: {
    email: string;
    contact: string;
  };
  theme: {
    color: string;
  };
  handler: (response: RazorpayResponse) => void;
  modal: {
    ondismiss: () => void;
  };
}

export interface RazorpayResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

declare global {
  interface Window {
    Razorpay: any;
  }
}

export const paymentService = {
  /**
   * Create payment order for a contract
   */
  async createPaymentOrder(contractId: string): Promise<PaymentOrderResponse> {
    const response = await api.post("/payments/create-order", { contractId });
    return response.data;
  },

  /**
   * Verify payment with backend
   */
  async verifyPayment(
    razorpayOrderId: string,
    razorpayPaymentId: string,
    razorpaySignature: string,
  ): Promise<any> {
    const response = await api.post("/payments/verify", {
      razorpayOrderId,
      razorpayPaymentId,
      razorpaySignature,
    });
    return response.data;
  },

  /**
   * Open Razorpay checkout modal
   */
  openRazorpayCheckout(
    orderData: PaymentOrderResponse,
    onSuccess: (response: RazorpayResponse) => void,
    onError: (error: any) => void,
    onDismiss: () => void,
  ): void {
    if (!window.Razorpay) {
      console.error("Razorpay SDK not loaded");
      onError(new Error("Razorpay SDK not loaded"));
      return;
    }

    const options: RazorpayOptions = {
      key: orderData.razorpayKeyId,
      amount: Math.round(orderData.amount * 100), // Convert to paise
      currency: orderData.currency,
      name: orderData.merchantName,
      description: orderData.contractTitle,
      order_id: orderData.orderId,
      prefill: {
        email: orderData.payerEmail,
        contact: orderData.payerPhone,
      },
      theme: {
        color: "#2563EB", // Blue-600
      },
      handler: onSuccess,
      modal: {
        ondismiss: onDismiss,
      },
    };

    const razorpay = new window.Razorpay(options);

    razorpay.on("payment.failed", function (response: any) {
      onError(response.error);
    });

    razorpay.open();
  },
};
