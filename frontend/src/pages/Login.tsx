import React, { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { authApi } from "../services/api";
import { useNavigate, Link } from "react-router-dom";

const Login: React.FC = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const data = await authApi.login(email, password);
      // Data expected structure based on backend AuthResponse:
      // { token: String, role: String, username: String, name: String, id: String }

      const user = {
        id: data.id,
        username: data.username || data.email,
        role: data.role || "USER",
        name: data.name,
      };

      login(data.token, user);
    } catch (err: any) {
      console.error("Login Error: ", err);
      setError("Invalid email or password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-blue-900">
          LegalPay
        </h2>
        <h2 className="mt-2 text-center text-xl font-bold text-gray-900">
          Sign in to your account
        </h2>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <form className="space-y-6" onSubmit={handleSubmit}>
            <div>
              <label
                htmlFor="email"
                className="block text-sm font-medium text-gray-700"
              >
                Email address
              </label>
              <div className="mt-1">
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700"
              >
                Password
              </label>
              <div className="mt-1">
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                />
              </div>
              <div className="text-right mt-2">
                <Link
                  to="/forgot-password"
                  className="text-sm text-blue-600 hover:text-blue-700"
                >
                  Forgot password?
                </Link>
              </div>
            </div>

            {error && <div className="text-red-500 text-sm">{error}</div>}

            <div>
              <button
                type="submit"
                disabled={loading}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                {loading ? "Signing in..." : "Sign in"}
              </button>
            </div>

            <div className="text-xs text-gray-500 mt-4 text-center">
              <p>Demo Credentials:</p>
              <p>Merchant: merchant@example.com / password</p>
              <p>Payer: rajesh@example.com / password</p>
            </div>

            <div className="mt-6 border-t pt-6">
              <p className="text-center text-sm text-gray-600">
                Don't have an account?
              </p>
              <div className="mt-3 grid grid-cols-2 gap-3">
                <Link
                  to="/register/merchant"
                  className="text-center px-4 py-2 border border-blue-300 rounded-md text-sm font-medium text-blue-600 bg-blue-50 hover:bg-blue-100"
                >
                  Register as Merchant
                </Link>
                <Link
                  to="/register/payer"
                  className="text-center px-4 py-2 border border-green-300 rounded-md text-sm font-medium text-green-600 bg-green-50 hover:bg-green-100"
                >
                  Register as Payer
                </Link>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Login;
