import React, { useState } from "react";
import { paymentService, RazorpayResponse } from "../services/paymentService";
import { useNavigate } from "react-router-dom";

interface PaymentButtonProps {
  contractId: string;
  amount: number;
  disabled?: boolean;
}

export default function PaymentButton({
  contractId,
  amount,
  disabled = false,
}: PaymentButtonProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const handlePayment = async () => {
    setLoading(true);
    setError(null);

    try {
      // Step 1: Create payment order
      const orderData = await paymentService.createPaymentOrder(contractId);

      // Step 2: Open Razorpay checkout
      paymentService.openRazorpayCheckout(
        orderData,
        // On success
        async (response: RazorpayResponse) => {
          try {
            // Step 3: Verify payment with backend
            await paymentService.verifyPayment(
              response.razorpay_order_id,
              response.razorpay_payment_id,
              response.razorpay_signature,
            );

            // Step 4: Redirect to success page
            navigate(`/payment/success?contractId=${contractId}`);
          } catch (err) {
            console.error("Payment verification failed:", err);
            setError("Payment verification failed. Please contact support.");
            setLoading(false);
          }
        },
        // On error
        (error: any) => {
          console.error("Payment failed:", error);
          setError(error.description || "Payment failed. Please try again.");
          setLoading(false);
        },
        // On dismiss
        () => {
          setLoading(false);
        },
      );
    } catch (err: any) {
      console.error("Error creating payment order:", err);
      setError(err.message || "Failed to initiate payment. Please try again.");
      setLoading(false);
    }
  };

  return (
    <div>
      <button
        onClick={handlePayment}
        disabled={disabled || loading}
        className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-3 px-6 rounded-lg font-semibold text-lg hover:from-blue-700 hover:to-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
      >
        {loading ? (
          <span className="flex items-center justify-center">
            <svg className="animate-spin h-5 w-5 mr-3" viewBox="0 0 24 24">
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
                fill="none"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            Processing...
          </span>
        ) : (
          `Pay ₹${amount.toLocaleString("en-IN")}`
        )}
      </button>

      {error && (
        <div className="mt-4 bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-700 text-sm">{error}</p>
        </div>
      )}
    </div>
  );
}
