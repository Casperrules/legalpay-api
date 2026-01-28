import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { healthApi } from '../services/api';

export default function Home() {
  const [apiStatus, setApiStatus] = useState<'checking' | 'online' | 'offline'>('checking');

  useEffect(() => {
    checkApiHealth();
  }, []);

  const checkApiHealth = async () => {
    try {
      await healthApi.check();
      setApiStatus('online');
    } catch (error) {
      setApiStatus('offline');
    }
  };

  const features = [
    {
      icon: '📄',
      title: 'Digital Contracts',
      description: 'Create and manage payment contracts with eSign integration',
    },
    {
      icon: '✍️',
      title: 'eSign Integration',
      description: 'Legally valid digital signatures via Digio/Leegality',
    },
    {
      icon: '💳',
      title: 'Auto Collections',
      description: 'Automated EMI collection via eNACH/UPI Autopay',
    },
    {
      icon: '🔄',
      title: 'Smart Retry',
      description: 'Intelligent retry logic for failed payments',
    },
    {
      icon: '⚖️',
      title: 'Legal Notices',
      description: 'Automated Section 25 legal notice generation',
    },
    {
      icon: '🔗',
      title: 'Blockchain Audit',
      description: 'Immutable contract records on Polygon',
    },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-20">
        <div className="text-center">
          <div className="text-6xl mb-6">⚖️</div>
          <h1 className="text-4xl sm:text-5xl font-extrabold text-gray-900 mb-4">
            Welcome to <span className="text-blue-600">LegalPay</span>
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
            Automated payment collection platform for merchants with legal enforcement capabilities
          </p>

          {/* API Status */}
          <div className="inline-flex items-center px-4 py-2 rounded-full bg-white shadow-sm mb-8">
            <div
              className={`w-2 h-2 rounded-full mr-2 ${
                apiStatus === 'online'
                  ? 'bg-green-500'
                  : apiStatus === 'offline'
                  ? 'bg-red-500'
                  : 'bg-yellow-500'
              }`}
            />
            <span className="text-sm font-medium text-gray-700">
              API Status:{' '}
              {apiStatus === 'online' ? 'Online' : apiStatus === 'offline' ? 'Offline' : 'Checking...'}
            </span>
          </div>

          {/* CTA Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link
              to="/dashboard"
              className="inline-flex items-center justify-center px-8 py-3 border border-transparent text-base font-medium rounded-lg text-white bg-blue-600 hover:bg-blue-700 shadow-lg hover:shadow-xl transition-all"
            >
              <span className="mr-2">📊</span>
              Go to Dashboard
            </Link>
            <Link
              to="/contracts/new"
              className="inline-flex items-center justify-center px-8 py-3 border border-gray-300 text-base font-medium rounded-lg text-gray-700 bg-white hover:bg-gray-50 shadow-lg hover:shadow-xl transition-all"
            >
              <span className="mr-2">➕</span>
              Create Contract
            </Link>
          </div>
        </div>

        {/* Features Grid */}
        <div className="mt-20">
          <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
            Platform Features
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <div
                key={index}
                className="bg-white rounded-xl shadow-md p-6 hover:shadow-xl transition-shadow"
              >
                <div className="text-4xl mb-4">{feature.icon}</div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">
                  {feature.title}
                </h3>
                <p className="text-gray-600">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Quick Start */}
        <div className="mt-20 bg-white rounded-xl shadow-lg p-8">
          <h2 className="text-2xl font-bold text-gray-900 mb-6 text-center">
            Quick Start Guide
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl font-bold text-blue-600">
                1
              </div>
              <h3 className="font-semibold text-gray-900 mb-2">Create Contract</h3>
              <p className="text-sm text-gray-600">
                Add contract details, payment terms, and customer information
              </p>
            </div>
            <div className="text-center">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl font-bold text-blue-600">
                2
              </div>
              <h3 className="font-semibold text-gray-900 mb-2">eSign Document</h3>
              <p className="text-sm text-gray-600">
                Send contract for digital signature and setup payment mandate
              </p>
            </div>
            <div className="text-center">
              <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl font-bold text-blue-600">
                3
              </div>
              <h3 className="font-semibold text-gray-900 mb-2">Auto Collections</h3>
              <p className="text-sm text-gray-600">
                Sit back while payments are collected automatically via mandate
              </p>
            </div>
          </div>
        </div>

        {/* Documentation Links */}
        <div className="mt-12 text-center">
          <h3 className="text-lg font-medium text-gray-900 mb-4">
            Explore Documentation
          </h3>
          <div className="flex flex-wrap justify-center gap-4">
            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center px-4 py-2 border border-blue-300 rounded-lg text-sm font-medium text-blue-700 bg-blue-50 hover:bg-blue-100"
            >
              📚 API Documentation
            </a>
            <a
              href="http://localhost:8080/h2-console"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center px-4 py-2 border border-green-300 rounded-lg text-sm font-medium text-green-700 bg-green-50 hover:bg-green-100"
            >
              🗄️ Database Console
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
