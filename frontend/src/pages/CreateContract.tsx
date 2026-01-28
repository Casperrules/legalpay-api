import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  contractApi,
  merchantApi,
  payerApi,
  Merchant,
  Payer,
  ContractCreateRequest,
} from "../services/api";

export default function CreateContract() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [payers, setPayers] = useState<Payer[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const [formData, setFormData] = useState<ContractCreateRequest>({
    merchantId: "",
    payerId: "",
    principalAmount: 100000,
    interestRate: 12.0,
    startDate: "",
    endDate: "",
    paymentType: "EMI",
    paymentFrequency: "MONTHLY",
  });

  useEffect(() => {
    loadMerchantsAndPayers();
    // Set default dates (start: tomorrow, end: 1 year later)
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const oneYearLater = new Date(tomorrow);
    oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);

    setFormData((prev) => ({
      ...prev,
      startDate: tomorrow.toISOString().split("T")[0],
      endDate: oneYearLater.toISOString().split("T")[0],
    }));
  }, []);

  const loadMerchantsAndPayers = async () => {
    try {
      const [merchantsData, payersData] = await Promise.all([
        merchantApi.list(),
        payerApi.list(),
      ]);
      setMerchants(merchantsData);
      setPayers(payersData);

      // Auto-select first merchant and payer
      if (merchantsData.length > 0) {
        setFormData((prev) => ({ ...prev, merchantId: merchantsData[0].id }));
      }
      if (payersData.length > 0) {
        setFormData((prev) => ({ ...prev, payerId: payersData[0].id }));
      }
    } catch (err) {
      console.error("Error loading merchants/payers:", err);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);

    try {
      const contract = await contractApi.create(formData);
      setSuccess(true);

      // Redirect to contract details after 2 seconds
      setTimeout(() => {
        navigate(`/contracts/${contract.id}`);
      }, 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to create contract");
      console.error("Error creating contract:", err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = e.target;

    // If switching to ONE_TIME, remove paymentFrequency
    if (name === "paymentType" && value === "ONE_TIME") {
      setFormData((prev) => {
        const { paymentFrequency, ...rest } = prev;
        return { ...rest, paymentType: "ONE_TIME" };
      });
    }
    // If switching to EMI, add paymentFrequency
    else if (name === "paymentType" && value === "EMI") {
      setFormData((prev) => ({
        ...prev,
        paymentType: "EMI",
        paymentFrequency: "MONTHLY",
      }));
    } else {
      setFormData((prev) => ({
        ...prev,
        [name]:
          name === "principalAmount" || name === "interestRate"
            ? parseFloat(value)
            : value,
      }));
    }
  };

  const calculateEMI = () => {
    const { principalAmount, interestRate, startDate, endDate } = formData;
    const start = new Date(startDate);
    const end = new Date(endDate);
    const months =
      (end.getFullYear() - start.getFullYear()) * 12 +
      (end.getMonth() - start.getMonth());

    if (months <= 0) return 0;

    const monthlyRate = interestRate / 12 / 100;
    const emi =
      (principalAmount * monthlyRate * Math.pow(1 + monthlyRate, months)) /
      (Math.pow(1 + monthlyRate, months) - 1);

    return isNaN(emi) ? 0 : emi;
  };

  const estimatedEMI = calculateEMI();

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-6">
        <button
          onClick={() => navigate("/dashboard")}
          className="text-sm text-gray-600 hover:text-gray-900 flex items-center"
        >
          <span className="mr-2">←</span>
          Back to Dashboard
        </button>
      </div>

      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-6">
            Create New Contract
          </h1>

          {success && (
            <div className="mb-6 rounded-lg bg-green-50 p-4 border border-green-200">
              <div className="flex">
                <div className="text-2xl mr-3">✅</div>
                <div>
                  <h3 className="text-sm font-medium text-green-800">
                    Contract created successfully!
                  </h3>
                  <p className="mt-1 text-sm text-green-700">
                    Redirecting to contract details...
                  </p>
                </div>
              </div>
            </div>
          )}

          {error && (
            <div className="mb-6 rounded-lg bg-red-50 p-4 border border-red-200">
              <div className="flex">
                <div className="text-2xl mr-3">⚠️</div>
                <div>
                  <h3 className="text-sm font-medium text-red-800">Error</h3>
                  <p className="mt-1 text-sm text-red-700">{error}</p>
                </div>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Merchant Selection */}
            <div>
              <label
                htmlFor="merchantId"
                className="block text-sm font-medium text-gray-700"
              >
                Merchant
              </label>
              <select
                id="merchantId"
                name="merchantId"
                value={formData.merchantId}
                onChange={handleChange}
                required
                className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
              >
                <option value="">Select Merchant</option>
                {merchants.map((merchant) => (
                  <option key={merchant.id} value={merchant.id}>
                    {merchant.businessName} ({merchant.email})
                  </option>
                ))}
              </select>
            </div>

            {/* Payer Selection */}
            <div>
              <label
                htmlFor="payerId"
                className="block text-sm font-medium text-gray-700"
              >
                Payer (Customer)
              </label>
              <select
                id="payerId"
                name="payerId"
                value={formData.payerId}
                onChange={handleChange}
                required
                className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
              >
                <option value="">Select Payer</option>
                {payers.map((payer) => (
                  <option key={payer.id} value={payer.id}>
                    {payer.fullName} ({payer.email})
                  </option>
                ))}
              </select>
            </div>

            {/* Principal Amount */}
            <div>
              <label
                htmlFor="principalAmount"
                className="block text-sm font-medium text-gray-700"
              >
                Principal Amount (₹)
              </label>
              <input
                type="number"
                id="principalAmount"
                name="principalAmount"
                value={formData.principalAmount}
                onChange={handleChange}
                min="10000"
                max="10000000"
                step="1000"
                required
                className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
              />
              <p className="mt-1 text-xs text-gray-500">
                Min: ₹10,000 | Max: ₹1,00,00,000
              </p>
            </div>

            {/* Interest Rate */}
            <div>
              <label
                htmlFor="interestRate"
                className="block text-sm font-medium text-gray-700"
              >
                Annual Interest Rate (%)
              </label>
              <input
                type="number"
                id="interestRate"
                name="interestRate"
                value={formData.interestRate}
                onChange={handleChange}
                min="1"
                max="36"
                step="0.1"
                required
                className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
              />
              <p className="mt-1 text-xs text-gray-500">
                Range: 1% - 36% per annum
              </p>
            </div>

            {/* Payment Type */}
            <div>
              <label
                htmlFor="paymentType"
                className="block text-sm font-medium text-gray-700"
              >
                Payment Type
              </label>
              <select
                id="paymentType"
                name="paymentType"
                value={formData.paymentType || "EMI"}
                onChange={handleChange}
                required
                className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
              >
                <option value="ONE_TIME">One-Time Payment</option>
                <option value="EMI">EMI (Installments)</option>
              </select>
              <p className="mt-1 text-xs text-gray-500">
                {formData.paymentType === "ONE_TIME"
                  ? "Full amount due on specified date"
                  : "Amount divided into regular installments"}
              </p>
            </div>

            {/* Payment Frequency - Only for EMI */}
            {formData.paymentType === "EMI" && (
              <div>
                <label
                  htmlFor="paymentFrequency"
                  className="block text-sm font-medium text-gray-700"
                >
                  Payment Frequency
                </label>
                <select
                  id="paymentFrequency"
                  name="paymentFrequency"
                  value={formData.paymentFrequency}
                  onChange={handleChange}
                  required
                  className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
                >
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </div>
            )}

            {/* For ONE_TIME: Due Date */}
            {formData.paymentType === "ONE_TIME" && (
              <div>
                <label
                  htmlFor="endDate"
                  className="block text-sm font-medium text-gray-700"
                >
                  Due Date
                </label>
                <input
                  type="date"
                  id="endDate"
                  name="endDate"
                  value={formData.endDate}
                  onChange={handleChange}
                  required
                  className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
                />
                <p className="mt-1 text-xs text-gray-500">
                  When the full payment is due
                </p>
              </div>
            )}

            {/* For EMI: Start Date and End Date */}
            {formData.paymentType === "EMI" && (
              <>
                <div>
                  <label
                    htmlFor="startDate"
                    className="block text-sm font-medium text-gray-700"
                  >
                    Start Date
                  </label>
                  <input
                    type="date"
                    id="startDate"
                    name="startDate"
                    value={formData.startDate}
                    onChange={handleChange}
                    required
                    className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    First EMI payment date
                  </p>
                </div>

                <div>
                  <label
                    htmlFor="endDate"
                    className="block text-sm font-medium text-gray-700"
                  >
                    End Date
                  </label>
                  <input
                    type="date"
                    id="endDate"
                    name="endDate"
                    value={formData.endDate}
                    onChange={handleChange}
                    required
                    className="mt-1 block w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm px-4 py-2 border"
                  />
                  <p className="mt-1 text-xs text-gray-500">
                    Final EMI payment date
                  </p>
                </div>
              </>
            )}

            {/* EMI Estimate - Only for EMI type */}
            {formData.paymentType === "EMI" && estimatedEMI > 0 && (
              <div className="rounded-lg bg-blue-50 p-4 border border-blue-200">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-blue-900">
                      Estimated EMI
                    </p>
                    <p className="text-xs text-blue-700 mt-1">
                      Based on reducing balance method
                    </p>
                  </div>
                  <div className="text-2xl font-bold text-blue-900">
                    ₹
                    {estimatedEMI.toLocaleString("en-IN", {
                      maximumFractionDigits: 0,
                    })}
                  </div>
                </div>
              </div>
            )}

            {/* One-Time Payment Info */}
            {formData.paymentType === "ONE_TIME" &&
              formData.principalAmount > 0 && (
                <div className="rounded-lg bg-green-50 p-4 border border-green-200">
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-green-900">
                        Total Amount Due
                      </p>
                      <p className="text-xs text-green-700 mt-1">
                        Single payment on end date
                      </p>
                    </div>
                    <div className="text-2xl font-bold text-green-900">
                      ₹
                      {formData.principalAmount.toLocaleString("en-IN", {
                        maximumFractionDigits: 0,
                      })}
                    </div>
                  </div>
                </div>
              )}

            {/* Submit Button */}
            <div className="flex justify-end space-x-3 pt-4 border-t">
              <button
                type="button"
                onClick={() => navigate("/dashboard")}
                className="px-4 py-2 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading || success}
                className="px-4 py-2 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <span className="flex items-center">
                    <span className="animate-spin mr-2">⏳</span>
                    Creating...
                  </span>
                ) : (
                  "Create Contract"
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
