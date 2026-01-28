import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";

export default function PaymentSuccess() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const contractId = searchParams.get("contractId");

  useEffect(() => {
    // Auto-redirect after 5 seconds
    const timer = setTimeout(() => {
      if (contractId) {
        navigate(`/contracts/${contractId}`);
      } else {
        navigate("/dashboard");
      }
    }, 5000);

    return () => clearTimeout(timer);
  }, [contractId, navigate]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 to-blue-50 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-xl p-8 text-center">
        <div className="text-6xl mb-6">✅</div>
        <h1 className="text-3xl font-bold text-gray-900 mb-4">
          Payment Successful!
        </h1>
        <p className="text-gray-600 mb-6">
          Your payment has been processed successfully. The contract is now
          active and legally enforceable.
        </p>

        <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
          <p className="text-sm text-green-800">
            A confirmation email with payment details and legal evidence packet
            has been sent to your registered email address.
          </p>
        </div>

        <div className="space-y-3">
          <button
            onClick={() => navigate(`/contracts/${contractId}`)}
            className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700"
          >
            View Contract Details
          </button>
          <button
            onClick={() => navigate("/dashboard")}
            className="w-full bg-gray-100 text-gray-700 py-3 rounded-lg font-semibold hover:bg-gray-200"
          >
            Back to Dashboard
          </button>
        </div>

        <p className="text-xs text-gray-500 mt-6">
          Redirecting automatically in 5 seconds...
        </p>
      </div>
    </div>
  );
}
