import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  contractApi,
  merchantApi,
  payerApi,
  Contract,
  Merchant,
  Payer,
} from "../services/api";
import { format } from "date-fns";
import PaymentButton from "../components/PaymentButton";

export default function ContractDetails() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [contract, setContract] = useState<Contract | null>(null);
  const [merchant, setMerchant] = useState<Merchant | null>(null);
  const [payer, setPayer] = useState<Payer | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [initiatingESign, setInitiatingESign] = useState(false);

  useEffect(() => {
    if (id) {
      loadContract(id);
    }
  }, [id]);

  const loadContract = async (contractId: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await contractApi.getById(contractId);
      setContract(data);

      // Load merchant and payer details
      const [merchantData, payerData] = await Promise.all([
        merchantApi.getById(data.merchantId),
        payerApi.getById(data.payerId),
      ]);
      setMerchant(merchantData);
      setPayer(payerData);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load contract");
      console.error("Error loading contract:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleInitiateESign = async () => {
    if (!contract) return;

    setInitiatingESign(true);
    try {
      const updated = await contractApi.initiateESign(contract.id);
      setContract(updated);
      alert(
        "✅ eSign initiated successfully! Check your email for the signing link.",
      );
    } catch (err: any) {
      alert(
        "⚠️ Failed to initiate eSign: " +
          (err.response?.data?.message || "Unknown error"),
      );
      console.error("Error initiating eSign:", err);
    } finally {
      setInitiatingESign(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      minimumFractionDigits: 0,
    }).format(amount);
  };

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      DRAFT: "bg-gray-100 text-gray-700",
      PENDING_ESIGN: "bg-yellow-100 text-yellow-700",
      SIGNED: "bg-blue-100 text-blue-700",
      ACTIVE: "bg-green-100 text-green-700",
      COMPLETED: "bg-purple-100 text-purple-700",
      DEFAULTED: "bg-red-100 text-red-700",
      CANCELLED: "bg-gray-100 text-gray-500",
    };
    return colors[status] || "bg-gray-100 text-gray-700";
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        <p className="ml-3 text-sm text-gray-500">Loading contract...</p>
      </div>
    );
  }

  if (error || !contract) {
    return (
      <div className="text-center py-12">
        <div className="text-6xl mb-4">⚠️</div>
        <h3 className="text-lg font-medium text-gray-900 mb-2">
          Error Loading Contract
        </h3>
        <p className="text-gray-500 mb-4">{error || "Contract not found"}</p>
        <button
          onClick={() => navigate("/dashboard")}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          Back to Dashboard
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate("/dashboard")}
          className="text-sm text-gray-600 hover:text-gray-900 flex items-center"
        >
          <span className="mr-2">←</span>
          Back to Dashboard
        </button>

        <span
          className={`px-3 py-1 inline-flex text-sm leading-5 font-semibold rounded-full ${getStatusColor(contract.status)}`}
        >
          {contract.status}
        </span>
      </div>

      {/* Main Card */}
      <div className="bg-white shadow rounded-lg overflow-hidden">
        <div className="px-4 py-5 sm:px-6 bg-gradient-to-r from-blue-600 to-blue-700">
          <h1 className="text-2xl font-bold text-white">Contract Details</h1>
          <p className="mt-1 text-sm text-blue-100">ID: {contract.id}</p>
        </div>

        <div className="px-4 py-5 sm:p-6">
          <dl className="grid grid-cols-1 gap-x-4 gap-y-6 sm:grid-cols-2">
            {/* Principal Amount */}
            <div>
              <dt className="text-sm font-medium text-gray-500">
                Principal Amount
              </dt>
              <dd className="mt-1 text-2xl font-semibold text-gray-900">
                {formatCurrency(contract.principalAmount)}
              </dd>
            </div>

            {/* EMI Amount */}
            <div>
              <dt className="text-sm font-medium text-gray-500">EMI Amount</dt>
              <dd className="mt-1 text-2xl font-semibold text-green-600">
                {formatCurrency(contract.emiAmount)}
              </dd>
            </div>

            {/* Interest Rate */}
            <div>
              <dt className="text-sm font-medium text-gray-500">
                Interest Rate
              </dt>
              <dd className="mt-1 text-lg font-medium text-gray-900">
                {contract.interestRate}% per annum
              </dd>
            </div>

            {/* Payment Type */}
            <div>
              <dt className="text-sm font-medium text-gray-500">
                Payment Type
              </dt>
              <dd className="mt-1 text-lg font-medium text-gray-900">
                {contract.paymentType === "ONE_TIME"
                  ? "One-Time Payment"
                  : "EMI"}
              </dd>
            </div>

            {/* Payment Frequency - Only for EMI */}
            {contract.paymentType === "EMI" && contract.paymentFrequency && (
              <div>
                <dt className="text-sm font-medium text-gray-500">
                  Payment Frequency
                </dt>
                <dd className="mt-1 text-lg font-medium text-gray-900">
                  {contract.paymentFrequency}
                </dd>
              </div>
            )}

            {/* For EMI: Start Date */}
            {contract.paymentType === "EMI" && (
              <div>
                <dt className="text-sm font-medium text-gray-500">
                  Start Date
                </dt>
                <dd className="mt-1 text-lg font-medium text-gray-900">
                  {format(new Date(contract.startDate), "MMMM d, yyyy")}
                </dd>
              </div>
            )}

            {/* End Date / Due Date */}
            <div>
              <dt className="text-sm font-medium text-gray-500">
                {contract.paymentType === "ONE_TIME" ? "Due Date" : "End Date"}
              </dt>
              <dd className="mt-1 text-lg font-medium text-gray-900">
                {format(new Date(contract.endDate), "MMMM d, yyyy")}
              </dd>
            </div>

            {/* Merchant */}
            <div>
              <dt className="text-sm font-medium text-gray-500">
                Merchant (Lender)
              </dt>
              <dd className="mt-1 text-lg font-medium text-gray-900">
                {merchant?.businessName || "Loading..."}
              </dd>
              <dd className="mt-1 text-sm text-gray-500">{merchant?.email}</dd>
            </div>

            {/* Payer */}
            <div>
              <dt className="text-sm font-medium text-gray-500">
                Payer (Borrower)
              </dt>
              <dd className="mt-1 text-lg font-medium text-gray-900">
                {payer?.fullName || "Loading..."}
              </dd>
              <dd className="mt-1 text-sm text-gray-500">{payer?.email}</dd>
            </div>

            {/* PDF URL */}
            {contract.pdfUrl && (
              <div className="sm:col-span-2">
                <dt className="text-sm font-medium text-gray-500">
                  Contract PDF
                </dt>
                <dd className="mt-1">
                  <a
                    href={contract.pdfUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-800 flex items-center"
                  >
                    <span className="mr-2">📄</span>
                    View Contract Document
                  </a>
                </dd>
              </div>
            )}

            {/* eSign Document ID */}
            {contract.esignDocumentId && (
              <div className="sm:col-span-2">
                <dt className="text-sm font-medium text-gray-500">
                  eSign Document ID
                </dt>
                <dd className="mt-1 text-sm font-mono text-gray-900">
                  {contract.esignDocumentId}
                </dd>
              </div>
            )}

            {/* Blockchain Hash */}
            {contract.blockchainHash && (
              <div className="sm:col-span-2">
                <dt className="text-sm font-medium text-gray-500">
                  Blockchain Hash
                </dt>
                <dd className="mt-1 text-sm font-mono text-gray-900 break-all">
                  {contract.blockchainHash}
                </dd>
              </div>
            )}

            {/* Timestamps */}
            <div>
              <dt className="text-sm font-medium text-gray-500">Created At</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {contract.createdAt
                  ? format(new Date(contract.createdAt), "PPpp")
                  : "N/A"}
              </dd>
            </div>

            {contract.updatedAt && (
              <div>
                <dt className="text-sm font-medium text-gray-500">
                  Last Updated
                </dt>
                <dd className="mt-1 text-sm text-gray-900">
                  {format(new Date(contract.updatedAt), "PPpp")}
                </dd>
              </div>
            )}
          </dl>
        </div>

        {/* Actions */}
        <div className="px-4 py-4 sm:px-6 bg-gray-50 border-t border-gray-200">
          <div className="flex flex-col sm:flex-row gap-3">
            {contract.status === "DRAFT" && (
              <button
                onClick={handleInitiateESign}
                disabled={initiatingESign}
                className="inline-flex items-center justify-center px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
              >
                {initiatingESign ? (
                  <>
                    <span className="animate-spin mr-2">⏳</span>
                    Initiating...
                  </>
                ) : (
                  <>
                    <span className="mr-2">✍️</span>
                    Initiate eSign
                  </>
                )}
              </button>
            )}

            {contract.status === "PENDING_ESIGN" && (
              <div className="flex items-center text-sm text-yellow-700 bg-yellow-50 px-4 py-2 rounded-lg">
                <span className="mr-2">⏳</span>
                Waiting for customer to sign the contract
              </div>
            )}

            {contract.status === "SIGNED" && (
              <div className="flex items-center text-sm text-blue-700 bg-blue-50 px-4 py-2 rounded-lg">
                <span className="mr-2">✅</span>
                Contract signed. Ready for payment.
              </div>
            )}

            {contract.status === "ACTIVE" && (
              <button
                onClick={() => navigate("/payments")}
                className="inline-flex items-center justify-center px-4 py-2 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
              >
                <span className="mr-2">💳</span>
                View Payment Schedule
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Payment Section */}
      {(contract.status === "SIGNED" || contract.status === "ACTIVE") && (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <div className="px-4 py-5 sm:px-6 bg-gradient-to-r from-green-600 to-green-700">
            <h2 className="text-xl font-bold text-white">Payment</h2>
            <p className="mt-1 text-sm text-green-100">
              {contract.paymentStatus === "PAID"
                ? "Payment Completed"
                : "Complete payment to activate contract"}
            </p>
          </div>

          <div className="px-4 py-5 sm:p-6">
            {contract.paymentStatus === "PENDING" && (
              <div className="space-y-4">
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <p className="text-sm text-blue-800">
                    <strong>Amount Due:</strong>{" "}
                    {formatCurrency(contract.principalAmount)}
                  </p>
                  <p className="text-xs text-blue-600 mt-2">
                    Complete payment using UPI, cards, or netbanking
                  </p>
                </div>

                <PaymentButton
                  contractId={contract.id}
                  amount={contract.principalAmount}
                  disabled={
                    contract.status !== "ACTIVE" && contract.status !== "SIGNED"
                  }
                />
              </div>
            )}

            {contract.paymentStatus === "PAID" && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-6">
                <div className="flex items-center">
                  <svg
                    className="h-6 w-6 text-green-600 mr-3"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                    />
                  </svg>
                  <div>
                    <p className="text-green-800 font-semibold">
                      Payment Completed
                    </p>
                    <p className="text-green-600 text-sm">
                      Amount Paid:{" "}
                      {formatCurrency(
                        contract.totalPaidAmount || contract.principalAmount,
                      )}
                    </p>
                    {contract.lastPaymentAt && (
                      <p className="text-green-600 text-xs mt-1">
                        Paid on:{" "}
                        {format(new Date(contract.lastPaymentAt), "PPP")}
                      </p>
                    )}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Info Box */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex">
          <div className="flex-shrink-0">
            <span className="text-2xl">💡</span>
          </div>
          <div className="ml-3">
            <h3 className="text-sm font-medium text-blue-900">
              Contract Workflow
            </h3>
            <div className="mt-2 text-sm text-blue-700">
              <p className="mb-2">
                <strong>DRAFT:</strong> Contract created, ready for eSign
              </p>
              <p className="mb-2">
                <strong>PENDING_ESIGN:</strong> Sent to customer for digital
                signature
              </p>
              <p className="mb-2">
                <strong>SIGNED:</strong> Customer signed, mandate setup in
                progress
              </p>
              <p>
                <strong>ACTIVE:</strong> Payments being collected automatically
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
